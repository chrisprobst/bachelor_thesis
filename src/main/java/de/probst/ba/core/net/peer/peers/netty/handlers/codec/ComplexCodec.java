package de.probst.ba.core.net.peer.peers.netty.handlers.codec;

import de.probst.ba.core.net.peer.peers.netty.handlers.discovery.messages.SocketAddressMessage;
import de.probst.ba.core.util.io.IOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by chrisprobst on 12.09.14.
 */
public class ComplexCodec extends ChannelHandlerAdapter {

    static AtomicInteger serialized = new AtomicInteger();
    static AtomicInteger buffers = new AtomicInteger();

    long d = System.currentTimeMillis();

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof Serializable) {
            ByteBuf serialized = Unpooled.wrappedBuffer(IOUtil.serialize(msg));

            if (!(msg instanceof SocketAddressMessage)) {
                this.serialized.addAndGet(serialized.readableBytes());  //.addAndGet(serialized.readableBytes());
            }

            ByteBuf copy = Unpooled.buffer(4 + 1 + serialized.readableBytes());
            copy.writeInt(1 + serialized.readableBytes());
            copy.writeByte(0);
            copy.writeBytes(serialized);

            ctx.write(copy.array(), promise);

            copy.release();
            serialized.release();

        } else {
            ByteBuf byteBuf = (ByteBuf) msg;

            this.buffers.addAndGet(byteBuf.readableBytes());    ///.addAndGet(byteBuf.readableBytes());

            ByteBuf copy = Unpooled.buffer(4 + 1 + byteBuf.readableBytes());
            copy.writeInt(1 + byteBuf.readableBytes());
            copy.writeByte(1);
            copy.writeBytes(byteBuf);

            ctx.write(copy.array(), promise);

            copy.release();
            byteBuf.release();
        }
        ctx.flush();
/*
        long d2 = System.currentTimeMillis();
        if (d2 - d > 1000) {
            d = d2;
            System.out.println("Serialized: " + serialized + " Buffers: " + buffers);
        }*/
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
        super.channelWritabilityChanged(ctx);
    }

    private ByteBuf cumulativeBuffer = Unpooled.buffer();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        cumulativeBuffer.writeBytes((byte[]) msg);

        while (cumulativeBuffer.readableBytes() >= 4) {
            int packetSize = cumulativeBuffer.getInt(0);
            if (cumulativeBuffer.readableBytes() - 4 >= packetSize) {
                cumulativeBuffer.skipBytes(4);
                boolean raw = cumulativeBuffer.readByte() == 1;
                if (raw) {
                    ByteBuf copy = Unpooled.buffer(packetSize - 1);
                    cumulativeBuffer.readBytes(copy);
                    cumulativeBuffer.discardReadBytes();
                    ctx.fireChannelRead(copy);
                } else {
                    byte[] arr = new byte[packetSize - 1];
                    cumulativeBuffer.readBytes(arr);
                    cumulativeBuffer.discardReadBytes();
                    ctx.fireChannelRead(IOUtil.deserialize(arr));
                }
            } else {
                return;
            }
        }
    }
}
