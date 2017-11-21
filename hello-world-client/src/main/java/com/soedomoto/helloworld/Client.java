package com.soedomoto.helloworld;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Client {
    private static Logger logger = LoggerFactory.getLogger(Client.class.getName());

    public static void main(String[] args) throws IOException, InterruptedException {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext(true)
                .build();

        GreeterGrpc.GreeterStub stub = GreeterGrpc.newStub(channel);

        logger.info("Say: world");
        HelloRequest request = HelloRequest.newBuilder().setName("world").build();
        stub.sayHello(request, new StreamObserver<HelloReply>() {
            @Override
            public void onNext(HelloReply helloReply) {
                logger.info(helloReply.getMessage());
            }

            @Override
            public void onError(Throwable throwable) {
                logger.error(throwable.getMessage(), throwable);
            }

            @Override
            public void onCompleted() {
                logger.info("Complete");
            }
        });
    }

}
