package de.probst.ba.core.net.http.stream;

import de.probst.ba.core.media.database.DataBase;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderUtil;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.stream.ChunkedNioStream;
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
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
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.PARTIAL_CONTENT;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class HttpStreamServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    public static class LimitedReadableByteChannel implements ReadableByteChannel {

        private final ReadableByteChannel peer;
        private long current = 0;
        private final long max;

        private LimitedReadableByteChannel(ReadableByteChannel peer, long max) {
            Objects.requireNonNull(peer);
            this.peer = peer;
            this.max = max;
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            if (current < max) {
                if (current + dst.remaining() <= max) {
                    int read = peer.read(dst);
                    current += read;
                    return read;
                } else {
                    int rem = (int) (max - current);
                    dst.limit(dst.position() + rem);
                    int read = peer.read(dst);
                    current += read;
                    return read;
                }
            } else {
                return -1;
            }
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public void close() throws IOException {

        }
    }

    private final DataBase dataBase;

    public HttpStreamServerHandler(DataBase dataBase) {
        Objects.requireNonNull(dataBase);
        this.dataBase = dataBase;
    }


    @Override
    public void messageReceived(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {

        if (!request.decoderResult().isSuccess()) {
            sendError(ctx, BAD_REQUEST);
            return;
        }

        if (request.method() != GET) {
            sendError(ctx, METHOD_NOT_ALLOWED);
            return;
        }

        final String path = request.uri().substring(1);

        SeekableByteChannel[] channels = null;

        try {
            channels = dataBase.unsafeQueryRawWithName(path);
        } catch (IOException ignored) {
        }

        if (channels == null) {
            System.out.println("Invalid request: " + path);
            sendError(ctx, NOT_FOUND);
            return;
        }

        SeekableByteChannel channel = channels[0];

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
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, lower != -1 && upper != -1 ? PARTIAL_CONTENT : OK);
        response.headers().set(CONTENT_LENGTH, length);
        response.headers().set(CONTENT_TYPE, "video/mp4");
        response.headers().set(ACCEPT_RANGES, "bytes");
        if (HttpHeaderUtil.isKeepAlive(request)) {
            System.out.println("KEEP ALIVE");
            //response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }
        if (lower != -1 && upper != -1) {
            response.headers()
                    .set(CONTENT_RANGE, "bytes " + lower + "-" + (upper - 1) + "/" + channel.size());
        }
        System.out.println(response);
        ctx.write(response);

        // Write the content
        System.out.println(channel.size());
        ctx.writeAndFlush(new ChunkedNioStream(new LimitedReadableByteChannel(channel, length), 8192)).addListener(
                ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (ctx.channel().isActive()) {
            sendError(ctx, INTERNAL_SERVER_ERROR);
            cause.printStackTrace();
        }
    }

    private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");

        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}