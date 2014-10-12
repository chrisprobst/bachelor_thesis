package de.probst.ba.core.net.httpserver.httpservers.netty;

import de.probst.ba.core.media.database.DataBase;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.io.IOException;
import java.util.Objects;

public class NettyHttpServerInitializer extends ChannelInitializer<SocketChannel> {

    private final DataBase dataBase;

    public NettyHttpServerInitializer(DataBase dataBase) {
        Objects.requireNonNull(dataBase);
        this.dataBase = dataBase;
    }

    @Override
    public void initChannel(SocketChannel ch) throws IOException {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(65536));
        pipeline.addLast(new ChunkedWriteHandler());
        pipeline.addLast(new NettyHttpServerHandler(dataBase));
    }
}