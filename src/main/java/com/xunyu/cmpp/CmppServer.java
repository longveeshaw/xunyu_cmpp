package com.xunyu.cmpp;

import com.xunyu.cmpp.codec.CmppHeaderCodec;
import com.xunyu.cmpp.factory.MarshallingCodecFactory;
import com.xunyu.cmpp.handler.CmppServerChannelHandler;
import com.xunyu.cmpp.handler.EncoderAndDecoderHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author xym
 * @description cmpp 服务端
 * @date 2018/4/18 10:37
 */
public class CmppServer {

    private Logger logger = LoggerFactory.getLogger(CmppServer.class);

    public static void main(String[] args) {
        int port = 8990;
        CmppServer server = CmppServerInstanceGetter.instance;
        server.open(port);
    }

    /**
     * 私有构造器
     */
    private CmppServer(){

    }

    private static class CmppServerInstanceGetter{
        private static CmppServer instance = new CmppServer();
    }

    /**
     * @description 开启服务端口
     * @param port 端口号
     */
    private void open(int port){

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new ChannelInitializer<Channel>() {

                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            /*
                             * 消息总长度(含消息头及消息体) 最大消息长度要从配置里取 处理粘包，断包 TODO 从配置中取最大消息长度
                             */
//                            ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(4 * 1024 , 0, 4, -4, 0, true));

                            ch.pipeline().addLast(new IdleStateHandler(5, 0, 0, TimeUnit.SECONDS));
                            //消息头编解码器
                            ch.pipeline().addLast(new CmppHeaderCodec());
                            ch.pipeline().addLast(new EncoderAndDecoderHandler());
                            ch.pipeline().addLast(new CmppServerChannelHandler());
                        }


                    });
            //绑定端口、同步等待
            ChannelFuture future = b.bind(port).sync();
            logger.info("启动监听，等待连接：{}，监听端口号：{}",future.isSuccess(),port);

            //等待服务监听端口关闭
            future.channel().closeFuture().sync();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }

    }
}
