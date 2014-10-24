package de.probst.ba.core.net.httpserver.httpservers.netty;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.context.MethodValueResolver;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.database.DataLookupException;
import de.probst.ba.core.util.collections.Tuple;
import de.probst.ba.core.util.collections.Tuple2;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimetypesFileTypeMap;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Scanner;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.netty.handler.codec.http.HttpHeaders.Names.ACCEPT_RANGES;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_RANGE;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.Names.RANGE;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.PARTIAL_CONTENT;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public final class NettyHttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    public static final String TOTAL_SIZE_KEY = "total-size";
    public static final String PARTITIONS_KEY = "partitions";

    private static Predicate<DataInfo> byName(String name) {
        Objects.requireNonNull(name);
        return dataInfo -> dataInfo.isCompleted() && dataInfo.getName().isPresent() &&
                           dataInfo.getName().get().equals(name);
    }

    private final Logger logger = LoggerFactory.getLogger(NettyHttpServerHandler.class);
    private final DataBase dataBase;
    private final MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();
    private final Template template;

    public NettyHttpServerHandler(DataBase dataBase) throws IOException {
        Objects.requireNonNull(dataBase);
        this.dataBase = dataBase;

        // Add known MIME types that are not usual
        mimetypesFileTypeMap.addMimeTypes("video/mp4 mp4");

        // Init handlebars
        Handlebars handlebars = new Handlebars();
        String templateString =
                new Scanner(getClass().getResourceAsStream("templates/index.html")).useDelimiter("\\A").next();
        template = handlebars.compileInline(templateString);
    }

    private Optional<Tuple2<Long, Long>> getRangeBoundaries(FullHttpRequest request, long maxLength) {
        long lower, upper;
        CharSequence contentRangeChars = request.headers().get(RANGE);
        if (contentRangeChars != null) {
            String contentRange = contentRangeChars.toString();

            // Remove the unit
            contentRange = contentRange.replace("bytes=", "");

            // Parse boundaries
            String[] boundaries = contentRange.split("-");

            // The lower
            if (boundaries.length > 0 && boundaries[0] != null && !boundaries[0].equals("")) {
                lower = Long.valueOf(boundaries[0]);
            } else {
                lower = 0;
            }

            // The upper
            if (boundaries.length > 1 && boundaries[1] != null && !boundaries[1].equals("")) {
                upper = Long.valueOf(boundaries[1]);
            } else {
                upper = maxLength;
            }

            return Optional.of(Tuple.of(lower, upper));
        } else {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private void sendStream(ChannelHandlerContext ctx, FullHttpRequest request, QueryStringDecoder queryStringDecoder) {

        // Check that we have exactly one name parameter
        List<String> names = queryStringDecoder.parameters().get("name");
        if (names == null || names.size() != 1) {
            sendError(ctx, BAD_REQUEST);
            return;
        }
        String name = names.get(0);

        try {
            // Lookup data info
            Optional<DataInfo> dataInfo = dataBase.getDataInfo().values().stream().filter(byName(name)).findAny();
            if (!dataInfo.isPresent()) {
                throw new DataLookupException("Not found");
            }
            logger.info("Found data info: " + dataInfo);

            // Look for a description object and extract total size
            OptionalLong optionalTotalSize = OptionalLong.empty();
            Optional<Object> descriptionObject = dataInfo.get().getDescription();
            if (descriptionObject.isPresent() && descriptionObject.get() instanceof Map) {
                Map<String, Object> description = (Map<String, Object>) descriptionObject.get();
                optionalTotalSize = OptionalLong.of((long) description.get(TOTAL_SIZE_KEY));
            }

            // Make sure we found it
            if (!optionalTotalSize.isPresent()) {
                throw new DataLookupException("First description object did not contain the total size");
            }
            long totalSize = optionalTotalSize.getAsLong();

            // Calculate the boundaries
            Optional<Tuple2<Long, Long>> boundaries = getRangeBoundaries(request, totalSize);
            boolean hasRange = boundaries.isPresent();
            long offset = hasRange ? boundaries.get().first() : 0;
            long length = hasRange ? boundaries.get().second() - boundaries.get().first() : totalSize;

            // Prepare response
            HttpResponseStatus httpResponseStatus = hasRange ? PARTIAL_CONTENT : OK;
            HttpResponse response = new DefaultHttpResponse(HTTP_1_1, httpResponseStatus);
            response.headers().setLong(CONTENT_LENGTH, length);
            response.headers().set(CONTENT_TYPE, mimetypesFileTypeMap.getContentType(name));
            response.headers().set(ACCEPT_RANGES, "bytes");
            if (hasRange) {
                String contentRange =
                        boundaries.get().first() + "-" + (boundaries.get().second() - 1) + "/" + totalSize;
                response.headers().set(CONTENT_RANGE, "bytes " + contentRange);
            }

            logger.info(
                    "Client requested range [ Offset: " + offset + ", Length: " + length + ", TotalSize: " + totalSize +
                    "]");

            // Write the response and the body
            ctx.write(response);

            // Create chunked database input and write it
            ctx.writeAndFlush(new ChunkedDataBaseInput(dataBase, byName(name), offset, length))
               .addListener(ChannelFutureListener.CLOSE);
        } catch (DataLookupException e) {
            sendError(ctx, NOT_FOUND, e);
        } catch (Exception e) {
            sendError(ctx, INTERNAL_SERVER_ERROR, e);
        }
    }

    private void sendList(ChannelHandlerContext ctx) throws IOException {
        // Get data info
        Map<String, List<DataInfo>> model = dataBase.getDataInfo()
                                                    .values()
                                                    .stream()
                                                    .filter(DataInfo::isCompleted)
                                                    .filter(x -> x.getName().isPresent())
                                                    .collect(Collectors.groupingBy(x -> x.getName().get()));

        // Sort by ID
        model.forEach((k, v) -> Collections.sort(v, Comparator.comparing(DataInfo::getId)));

        Context context = Context
                .newBuilder(model.entrySet())
                .combine("streamUrl", "/stream?name=")
                .resolver(MethodValueResolver.INSTANCE, MapValueResolver.INSTANCE, JavaBeanValueResolver.INSTANCE)
                .build();

        sendHTML(ctx, OK, template.apply(context));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        logger.warn("Failed to process http request, reason: " + cause.getMessage());
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        // Decoder error
        if (!request.decoderResult().isSuccess()) {
            sendError(ctx, BAD_REQUEST);
            return;
        }

        // Only GET is allowed
        if (request.method() != GET) {
            sendError(ctx, METHOD_NOT_ALLOWED);
            return;
        }

        // Setup a query string decoder
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.uri());

        if (queryStringDecoder.path().equals("/stream")) {
            sendStream(ctx, request, queryStringDecoder);
        } else if (queryStringDecoder.path().equals("/list")) {
            sendList(ctx);
        } else {
            sendError(ctx, BAD_REQUEST);
        }
    }

    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        sendError(ctx, status, null);
    }

    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status, Throwable cause) {
        sendPlain(ctx, status, "Failure: " + status + (cause != null ? ", Reason: " + cause : ""));
    }

    private void sendHTML(ChannelHandlerContext ctx, HttpResponseStatus status, String plainMessage) {
        sendString(ctx, status, plainMessage, "text/html; charset=UTF-8");
    }

    private void sendPlain(ChannelHandlerContext ctx, HttpResponseStatus status, String plainMessage) {
        sendString(ctx, status, plainMessage, "text/plain; charset=UTF-8");
    }

    private void sendString(ChannelHandlerContext ctx,
                            HttpResponseStatus status,
                            String message,
                            String contentType) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,
                                                                status,
                                                                Unpooled.copiedBuffer(message + "\r\n",
                                                                                      CharsetUtil.UTF_8));
        response.headers().set(CONTENT_TYPE, contentType);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}