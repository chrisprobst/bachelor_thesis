package de.probst.ba.core.net.peer.peers.netty.handlers.codecs;

import de.probst.ba.core.util.IOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;

import java.io.Serializable;
import java.util.List;

/**
 * Created by chrisprobst on 31.08.14.
 */
public class SimpleCodec extends MessageToMessageCodec<ByteBuf, Object> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
        if (msg instanceof Serializable) {
            byte[] buffer = IOUtil.serialize(msg);
            ByteBuf combined = Unpooled.buffer(1 + buffer.length);
            combined.writeByte(0);
            combined.writeBytes(buffer);
            out.add(combined);
        } else {
            ByteBuf old = (ByteBuf) msg;
            ByteBuf combined = Unpooled.buffer(1 + old.readableBytes());
            combined.writeByte(1);
            combined.writeBytes(old);
            out.add(combined);
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        if (msg.readByte() == 0) {
            byte[] arr = new byte[msg.readableBytes()];
            msg.readBytes(arr);
            out.add(IOUtil.deserialize(arr));
        } else {
            msg.retain();
            out.add(msg);
        }
    }
}
