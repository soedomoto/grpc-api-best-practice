package com.soedomoto.helloworld;

import io.grpc.ServerBuilder;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Server {
    private static Logger logger = LoggerFactory.getLogger(Server.class.getName());

    public static void main(String[] args) throws IOException, InterruptedException {
        io.grpc.Server server = ServerBuilder.forPort(50052)
                .addService(new GreeterImpl())
                .build();

        server.start();
        logger.info("Server is started.");

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
