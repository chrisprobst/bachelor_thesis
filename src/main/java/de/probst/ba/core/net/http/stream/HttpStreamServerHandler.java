package de.probst.ba.core.net.http.stream;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
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
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import static io.netty.handler.codec.http.HttpHeaders.Names.ACCEPT_RANGES;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_RANGE;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.Names.RANGE;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.PARTIAL_CONTENT;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * A simple handler that serves incoming HTTP requests to send their respective
 * HTTP responses.  It also implements {@code 'If-Modified-Since'} header to
 * take advantage of browser cache, as described in
 * <a href="http://tools.ietf.org/html/rfc2616#section-14.25">RFC 2616</a>.
 * <p>
 * <h3>How Browser Caching Works</h3>
 * <p>
 * Web browser caching works with HTTP headers as illustrated by the following
 * sample:
 * <ol>
 * <li>Request #1 returns the content of {@code /file1.txt}.</li>
 * <li>Contents of {@code /file1.txt} is cached by the browser.</li>
 * <li>Request #2 for {@code /file1.txt} does return the contents of the
 * file again. Rather, a 304 Not Modified is returned. This tells the
 * browser to use the contents stored in its cache.</li>
 * <li>The server knows the file has not been modified because the
 * {@code If-Modified-Since} date is the same as the file's last
 * modified date.</li>
 * </ol>
 * <p>
 * <pre>
 * Request #1 Headers
 * ===================
 * GET /file1.txt HTTP/1.1
 *
 * Response #1 Headers
 * ===================
 * HTTP/1.1 200 OK
 * Date:               Tue, 01 Mar 2011 22:44:26 GMT
 * Last-Modified:      Wed, 30 Jun 2010 21:36:48 GMT
 * Expires:            Tue, 01 Mar 2012 22:44:26 GMT
 * Cache-Control:      private, max-age=31536000
 *
 * Request #2 Headers
 * ===================
 * GET /file1.txt HTTP/1.1
 * If-Modified-Since:  Wed, 30 Jun 2010 21:36:48 GMT
 *
 * Response #2 Headers
 * ===================
 * HTTP/1.1 304 Not Modified
 * Date:               Tue, 01 Mar 2011 22:44:28 GMT
 *
 * </pre>
 */
public class HttpStreamServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    public static final int HTTP_CACHE_SECONDS = 60;

    private final File movie = new File("/Volumes/External HDD/Movies/Neuseeland.mp4");
    private final RandomAccessFile randomAccessFile = new RandomAccessFile(movie, "r");
    private ChannelFuture pendingWriteFuture;

    public HttpStreamServerHandler() throws IOException {
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        if (pendingWriteFuture != null) {
            System.out.println("VERY STRANGE!!!");
            pendingWriteFuture.cancel(true);
            ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        }

        if (!request.decoderResult().isSuccess()) {
            sendError(ctx, BAD_REQUEST);
            return;
        }

        if (request.method() != GET) {
            sendError(ctx, METHOD_NOT_ALLOWED);
            return;
        }

        final String path = request.uri();
        if (path == null) {
            sendError(ctx, FORBIDDEN);
            return;
        }

        if (!path.equalsIgnoreCase("/neuseeland.mp4")) {
            System.out.println("Invalued request: " + path);
            sendError(ctx, NOT_FOUND);
            return;
        }

        // Read ranges
        long lower = -1, upper = -1, length = movie.length();
        String contentRange = request.headers().get(RANGE);
        if (contentRange != null) {
            contentRange = contentRange.replace("bytes=", "");


            String[] boundries = contentRange.split("-");
            if (boundries.length > 0 && boundries[0] != null && !boundries[0].equals("")) {
                lower = Long.valueOf(boundries[0]);
            } else {
                lower = 0;
            }
            if (boundries.length > 1 && boundries[1] != null && !boundries[1].equals("")) {
                upper = Long.valueOf(boundries[1]);
            } else {
                upper = movie.length();
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
                    .set(CONTENT_RANGE, "bytes " + lower + "-" + (upper - 1) + "/" + movie.length());
        }
        ctx.write(response);

        // Write the content
        ctx.writeAndFlush(new ChunkedFile(randomAccessFile, lower != -1 ? lower : 0, length, 8192))
           .addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (ctx.channel().isActive()) {
            sendError(ctx, INTERNAL_SERVER_ERROR);
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