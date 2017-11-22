package com.soedomoto.helloworld;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.resolver.zookeeper.ZkNameResolverProvider;
import old.ZookeeperZoneAwareNameResolverProvider;
import io.grpc.stub.StreamObserver;
import io.grpc.util.RoundRobinLoadBalancerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class ClientWithZookeeperResolver {
    private static Logger logger = LoggerFactory.getLogger(ClientWithZookeeperResolver.class.getName());

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        ManagedChannel channel = ManagedChannelBuilder
                .forTarget(String.format("zk://%s:%d/%s", "localhost", 2181, "service-greet"))
                .nameResolverFactory(new ZkNameResolverProvider())
                .loadBalancerFactory(RoundRobinLoadBalancerFactory.getInstance())
                .usePlaintext(true)
                .build();

        // Input
        HelloRequest req = HelloRequest.newBuilder()
                .setName("world")
                .build();

        // Blocking stub
        long start = System.currentTimeMillis();
        logger.info("Blocking stub");
        String resp = GreeterGrpc.newBlockingStub(channel)
                .sayHello(req)
                .getMessage();
        long end = System.currentTimeMillis();

        logger.info(String.format("%s in %d", resp, end-start));

        // Future stub
        start = System.currentTimeMillis();
        logger.info("Future stub");
        String resp2 = GreeterGrpc.newFutureStub(channel)
                .sayHello(req)
                .get()
                .getMessage();
        end = System.currentTimeMillis();

        logger.info(String.format("%s in %d", resp2, end-start));

        // Default stub
        final long start2 = System.currentTimeMillis();
        logger.info("Default stub");
        GreeterGrpc.newStub(channel)
                .sayHello(req, new StreamObserver<HelloReply>() {
                    @Override
                    public void onNext(HelloReply helloReply) {
                        long end = System.currentTimeMillis();
                        logger.info(String.format("%s in %d", helloReply.getMessage(), end-start2));
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        logger.error(throwable.getMessage());
                    }

                    @Override
                    public void onCompleted() {
                        long end = System.currentTimeMillis();
                        logger.info(String.format("Completed in %d", end-start2));
                    }
                });
    }

}
