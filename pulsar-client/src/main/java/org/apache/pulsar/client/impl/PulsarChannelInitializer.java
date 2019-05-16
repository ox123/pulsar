/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pulsar.client.impl;

import java.security.cert.X509Certificate;
import java.util.function.Supplier;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.ssl.SslContext;

import org.apache.pulsar.client.api.AuthenticationDataProvider;
import org.apache.pulsar.client.impl.conf.ClientConfigurationData;
import org.apache.pulsar.common.api.ByteBufPair;
import org.apache.pulsar.common.api.Commands;
import org.apache.pulsar.common.api.PulsarDecoder;
import org.apache.pulsar.common.util.SecurityUtility;

public class PulsarChannelInitializer extends ChannelInitializer<SocketChannel> {

    public static final String TLS_HANDLER = "tls";

    private final Supplier<ClientCnx> clientCnxSupplier;
    private final SslContext sslCtx;
    private final ClientConfigurationData conf;

    public PulsarChannelInitializer(ClientConfigurationData conf, Supplier<ClientCnx> clientCnxSupplier)
            throws Exception {
        super();
        this.clientCnxSupplier = clientCnxSupplier;
        if (conf.isUseTls()) {
            // Set client certificate if available
            AuthenticationDataProvider authData = conf.getAuthentication().getAuthData();
            if (authData.hasDataForTls()) {
                this.sslCtx = SecurityUtility.createNettySslContextForClient(conf.isTlsAllowInsecureConnection(),
                        conf.getTlsTrustCertsFilePath(), (X509Certificate[]) authData.getTlsCertificates(),
                        authData.getTlsPrivateKey());
            } else {
                this.sslCtx = SecurityUtility.createNettySslContextForClient(conf.isTlsAllowInsecureConnection(),
                        conf.getTlsTrustCertsFilePath());
            }
        } else {
            this.sslCtx = null;
        }
        this.conf = conf;
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        if (sslCtx != null) {
            ch.pipeline().addLast(TLS_HANDLER, sslCtx.newHandler(ch.alloc()));
            ch.pipeline().addLast("ByteBufPairEncoder", ByteBufPair.COPYING_ENCODER);
        } else {
            ch.pipeline().addLast("ByteBufPairEncoder", ByteBufPair.ENCODER);
        }

        ch.pipeline()
          .addLast("frameDecoder",
                   new LengthFieldBasedFrameDecoder(
                       Commands.DEFAULT_MAX_MESSAGE_SIZE + Commands.MESSAGE_SIZE_FRAME_PADDING,
                       0, 4, 0, 4));
        ch.pipeline().addLast("handler", clientCnxSupplier.get());
    }
}
