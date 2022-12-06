/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.data.pipeline.cdc.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.data.pipeline.cdc.client.handler.LoginRequestHandler;
import org.apache.shardingsphere.data.pipeline.cdc.client.handler.SubscriptionRequestHandler;
import org.apache.shardingsphere.data.pipeline.cdc.protocol.response.CDCResponse;

/**
 * CDC client.
 */
@Slf4j
public final class CDCClient {
    
    /**
     * Start ShardingSphere CDC client.
     *
     * @param port port
     * @param address addresses
     */
    @SneakyThrows(InterruptedException.class)
    public void start(final String address, final int port) {
        startInternal(address, port);
    }
    
    private void startInternal(final String address, final int port) throws InterruptedException {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.channel(NioSocketChannel.class)
                .group(new NioEventLoopGroup())
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.SO_REUSEADDR, true)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    
                    @Override
                    protected void initChannel(final NioSocketChannel channel) {
                        channel.pipeline().addLast(new ProtobufVarint32FrameDecoder());
                        channel.pipeline().addLast(new ProtobufDecoder(CDCResponse.getDefaultInstance()));
                        channel.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
                        channel.pipeline().addLast(new ProtobufEncoder());
                        // TODO username and password are read from the configuration file or args
                        channel.pipeline().addLast(new LoginRequestHandler("root", "root"));
                        channel.pipeline().addLast(new SubscriptionRequestHandler());
                    }
                });
        ChannelFuture future = bootstrap.connect(address, port).sync();
        future.channel().closeFuture().sync();
    }
}