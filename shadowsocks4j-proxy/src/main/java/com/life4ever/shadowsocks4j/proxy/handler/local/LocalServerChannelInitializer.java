package com.life4ever.shadowsocks4j.proxy.handler.local;

import com.life4ever.shadowsocks4j.proxy.handler.common.HeartbeatTimeoutHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

import static com.life4ever.shadowsocks4j.proxy.consts.Shadowsocks4jProxyConst.SERVER_ALL_IDLE_TIME;
import static com.life4ever.shadowsocks4j.proxy.consts.Shadowsocks4jProxyConst.SERVER_READ_IDLE_TIME;
import static com.life4ever.shadowsocks4j.proxy.consts.Shadowsocks4jProxyConst.SERVER_WRITE_IDLE_TIME;

public class LocalServerChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final EventLoopGroup clientWorkerGroup;

    public LocalServerChannelInitializer(EventLoopGroup clientWorkerGroup) {
        this.clientWorkerGroup = clientWorkerGroup;
    }

    @Override
    protected void initChannel(SocketChannel channel) throws Exception {
        ChannelPipeline pipeline = channel.pipeline();

        // encoder
        pipeline.addFirst(Socks5ServerEncoder.DEFAULT);

        // heartbeat
        pipeline.addLast(new IdleStateHandler(SERVER_READ_IDLE_TIME, SERVER_WRITE_IDLE_TIME, SERVER_ALL_IDLE_TIME, TimeUnit.MILLISECONDS));
        pipeline.addLast(HeartbeatTimeoutHandler.getInstance());

        // init
        pipeline.addLast(new Socks5InitialRequestDecoder());
        pipeline.addLast(Socks5InitialRequestHandler.getInstance());

        // command
        pipeline.addLast(new Socks5CommandRequestDecoder());
        pipeline.addLast(Socks5CommandRequestHandler.getInstance(clientWorkerGroup));
    }

}
