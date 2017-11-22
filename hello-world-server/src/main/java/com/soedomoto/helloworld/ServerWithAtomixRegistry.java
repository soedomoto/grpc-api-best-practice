package com.soedomoto.helloworld;

import io.atomix.Atomix;
import io.atomix.AtomixClient;
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.netty.NettyTransport;
import io.atomix.group.DistributedGroup;
import io.grpc.ServerBuilder;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

public class ServerWithAtomixRegistry {
    private static Logger logger = LoggerFactory.getLogger(Server.class.getName());

    public static void main(String[] args) throws IOException, InterruptedException {
        io.grpc.Server server = ServerBuilder.forPort(50052)
                .addService(new Server.GreeterImpl())
                .build();

        server.start();
        logger.info("Server is started.");

        try {
            logger.info("Register to atomix");
            AtomixClient client = AtomixClient.builder().withTransport(new NettyTransport()).build();
            Atomix atomix = client.connect(new Address("localhost", 8701)).get();
            DistributedGroup group = atomix.getGroup("service-greet").get();
            group.join(Collections.singletonMap("address", new InetSocketAddress("localhost", 50052)));
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                logger.info("*** shutting down gRPC server since JVM is shutting down.");
                server.shutdown();
                logger.info("*** server shut down.");
            }
        });

        server.awaitTermination();
    }

    static class GreeterImpl extends GreeterGrpc.GreeterImplBase {
        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
            HelloReply reply = HelloReply.newBuilder()
                    .setMessage(String.format("Welcome %s ...", request.getName()))
                    .build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }
}
