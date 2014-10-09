package de.probst.ba.core.net.http.stream;

import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.media.database.DataBaseReadChannel;
import de.probst.ba.core.util.io.LimitedReadableByteChannel;
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
import io.netty.handler.stream.ChunkedNioStream;
import io.netty.util.CharsetUtil;

import javax.activation.MimetypesFileTypeMap;
import java.util.Objects;

import static io.netty.handler.codec.http.HttpHeaders.Names.ACCEPT_RANGES;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_RANGE;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.Names.RANGE;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.PARTIAL_CONTENT;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class HttpStreamServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final DataBase dataBase;
    private final MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();

    public HttpStreamServerHandler(DataBase dataBase) {
        Objects.requireNonNull(dataBase);
        this.dataBase = dataBase;

        mimetypesFileTypeMap.addMimeTypes("video/mp4 mp4");
    }


    @Override
    public void messageReceived(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        if (!request.decoderResult().isSuccess()) {
            sendError(ctx, BAD_REQUEST, null);
            return;
        }

        if (request.method() != GET) {
            sendError(ctx, METHOD_NOT_ALLOWED, null);
            return;
        }

        final String path = request.uri().substring(1);

        try {
            DataBaseReadChannel channel =
                    dataBase.findIncremental(dataInfo -> dataInfo.getName().get().equals(path)).get();

            // Read ranges
            long lower = -1, upper = -1, length = channel.size();
            String contentRange = request.headers().get(RANGE);
            if (contentRange != null) {
                contentRange = contentRange.replace("bytes=", "");

                String[] boundries = contentRange.split("-");
                if (boundries.length > 0 && boundries[0] != null && !boundries[0].equals("")) {
                    lower = Long.valueOf(boundries[0]);
                } else {
                    lower = 0;
                }
                channel.position(lower);
                if (boundries.length > 1 && boundries[1] != null && !boundries[1].equals("")) {
                    upper = Long.valueOf(boundries[1]);
                } else {
                    upper = channel.size();
                }
                length = upper - lower;
                System.out.println("Requesting range: " + lower + "-" + upper);
            }

            // Prepare response
            HttpResponse response =
                    new DefaultHttpResponse(HTTP_1_1, lower != -1 && upper != -1 ? PARTIAL_CONTENT : OK);
            response.headers().set(CONTENT_LENGTH, length);


            response.headers().set(CONTENT_TYPE, mimetypesFileTypeMap.getContentType(path));
            response.headers().set(ACCEPT_RANGES, "bytes");
            if (lower != -1 && upper != -1) {
                response.headers()
                        .set(CONTENT_RANGE, "bytes " + lower + "-" + (upper - 1) + "/" + channel.size());
            }
            System.out.println(response);
            ctx.write(response);

            // Write the content
            ctx.writeAndFlush(new ChunkedNioStream(new LimitedReadableByteChannel(channel, length, true))).addListener(
                    ChannelFutureListener.CLOSE);
        } catch (Exception e) {
            System.out.println("Not found: " + path + ", Reason: " + e);
            sendError(ctx, BAD_REQUEST, e);
            return;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (ctx.channel().isActive()) {
            sendError(ctx, INTERNAL_SERVER_ERROR, cause);
            cause.printStackTrace();
        }
    }

    private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status, Throwable cause) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1,
                status,
                Unpooled.copiedBuffer("Failure: " + status + ", Reason: " + cause + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");

        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}