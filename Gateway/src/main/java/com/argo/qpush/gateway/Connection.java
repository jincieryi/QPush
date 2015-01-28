package com.argo.qpush.gateway;

import com.argo.qpush.core.entity.Payload;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by yaming_deng on 14-8-6.
 */
public class Connection {

    protected Logger logger = null;

    private final Channel channel;

    public Connection(Channel channel) {
        this.channel = channel;
        logger = LoggerFactory.getLogger(Connection.class.getName() + ".Channel." + channel.hashCode());
    }

    public void send(final SentProgress progress, Payload message){
        // 组装消息包
        if(channel.isOpen()){
            try {
                byte[] msg = message.asAPNSMessage().toByteArray();
                send(progress, msg);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                progress.incrFailed();
            }
        }else{
            progress.incrFailed();
            logger.error("Send Error. Channel is closed. {}, {}", channel, message);
        }
    }

    public void send(final SentProgress progress, final byte[] msg) {
        try {
            final ByteBuf data = channel.config().getAllocator().buffer(msg.length); // (2)
            data.writeBytes(msg);
            final ChannelFuture cf = channel.writeAndFlush(data);
            cf.addListener(new GenericFutureListener<Future<? super Void>>() {
                @Override
                public void operationComplete(Future<? super Void> future) throws Exception {
                    if(cf.cause() != null){
                        logger.error("{}, Send Error.", channel, cf.cause());
                        progress.incrFailed();
                    }else {
                        progress.incrSuccess();
                        if (logger.isDebugEnabled()){
                            logger.debug("{}, Send OK. {}", channel, channel.hashCode());
                        }
                    }
                }
            });
        } catch (Exception e) {
            progress.incrFailed();
            logger.error(e.getMessage(), e);
        }
    }

    public void close(){
        channel.close();
    }

    public Channel getChannel() {
        return channel;
    }

    @Override
    public int hashCode() {
        return channel.hashCode();
    }

    @Override
    public String toString() {
        return "Connection{" +
                "channel=" + channel +
                '}';
    }
}
