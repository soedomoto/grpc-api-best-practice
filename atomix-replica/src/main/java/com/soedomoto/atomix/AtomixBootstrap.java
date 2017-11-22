package com.soedomoto.atomix;

import io.atomix.AtomixReplica;
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.netty.NettyTransport;
import io.atomix.copycat.server.storage.Storage;

import java.util.UUID;

public class AtomixBootstrap {

    public static void main(String[] args) {
        AtomixReplica replica = AtomixReplica.builder(new Address("localhost", 8701))
                .withTransport(new NettyTransport())
                .withStorage(Storage.builder()
                        .withDirectory(System.getProperty("user.dir") + "/logs/" + UUID.randomUUID().toString())
                        .build())
                .build();

        replica.bootstrap().join();
    }

}
