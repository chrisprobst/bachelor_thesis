package de.probst.ba.core.net.pushpeers.netty.transfer;

import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.peers.netty.NettyConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by chrisprobst on 24.10.14.
 */
public class PushHandler extends ChannelHandlerAdapter {

    private final DataBase dataBase;
    private final Set<DataInfo> alreadyUploaded = new HashSet<>();
    private ChannelHandlerContext ctx;
    private Future<?> pushTask;
    private ChannelFuture writeFuture;

    private void schedule() {



        pushTask = ctx.executor().schedule(() -> {
            pushTask = null;
            pushData();
        }, NettyConfig.getAnnounceDelay(), TimeUnit.MILLISECONDS);
    }

    private void pushData() {
        // Create data info without the already uploaded data
        Map<String, DataInfo> dataInfo = new HashMap<>(dataBase.getDataInfo());
        alreadyUploaded.forEach(x -> {
            String hash = x.getHash();
            DataInfo y = dataInfo.get(hash);
            if (y != null) {
                y = y.subtract(x);
                if (y.isEmpty()) {
                    dataInfo.remove(hash);
                } else {
                    dataInfo.put(hash, y);
                }
            }
        });

        // Nothing to send
        if (dataInfo.isEmpty()) {
            schedule();
            return;
        }


    }

    public PushHandler(DataBase dataBase) {
        Objects.requireNonNull(dataBase);
        this.dataBase = dataBase;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        schedule();
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (pushTask != null) {
            pushTask.cancel(false);
            pushTask = null;
        }
        super.channelInactive(ctx);
    }
}

