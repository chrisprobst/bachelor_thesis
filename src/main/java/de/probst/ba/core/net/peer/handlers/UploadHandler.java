package de.probst.ba.core.net.peer.handlers;

import de.probst.ba.core.net.peer.handlers.messages.UploadRequestMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Created by chrisprobst on 13.08.14.
 */
public class UploadHandler extends SimpleChannelInboundHandler<UploadRequestMessage> {



    @Override
    protected void messageReceived(ChannelHandlerContext ctx,
                                   UploadRequestMessage msg) throws Exception {


    }
}
