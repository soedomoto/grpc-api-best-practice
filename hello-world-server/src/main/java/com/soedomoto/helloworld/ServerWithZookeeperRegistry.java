package com.soedomoto.helloworld;

import io.grpc.ServerBuilder;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.stub.StreamObserver;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class ServerWithZookeeperRegistry {
    private static Logger logger = LoggerFactory.getLogger(Server.class.getName());
    private static ZooKeeper zoo;

    public static void main(String[] args) throws Exception {
        io.grpc.Server server = ServerBuilder.forPort(50054)
                .addService(new GreeterImpl())
                .build();

        server.start();
        logger.info("Server is started.");

        logger.info("Register to zookeeper");
        String zkAddr = "localhost:2181";
        String path = "/service-greet";

//        zoo = new ZooKeeper(zkAddr, 5000, new WWatcher(zkAddr, path));

        Executors.newSingleThreadScheduledExecutor()
                .scheduleWithFixedDelay(() -> {
//                    register2(zkAddr, path);

                    String currTime = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
                    register(zkAddr, path, currTime.getBytes(), CreateMode.PERSISTENT);
                    register(zkAddr, path + "/" + "localhost" + ":" + 50054, currTime.getBytes(), CreateMode.EPHEMERAL);
                }, 0, 10, TimeUnit.SECONDS);

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

    private static Map<String, Stat> stats = new HashMap<>();

    private static void register(String zkAddr, String path, byte[] data, CreateMode cm) {
        Stat stat = stats.getOrDefault(path, null);

        try {
            if (zoo == null) {
                zoo = new ZooKeeper(zkAddr, 5000, new WWatcher(zkAddr, path, data, cm));
            }

            stat = zoo.exists(path, true);

            if (stat == null) {
                zoo.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, cm);
            } else {
                zoo.setData(path, data, stat.getVersion());
            }
        } catch (KeeperException.BadVersionException e) {
            try {
                zoo.delete(path, stat.getVersion());
            } catch (Exception e1) {
                logger.error(e.getMessage(), e);
            }
        } catch (KeeperException.ConnectionLossException e) {
            logger.error(String.format("Connection to %s is lost. Reconnecting", zkAddr), e);

            try {
                zoo = new ZooKeeper(zkAddr, 5000, new WWatcher(zkAddr, path, data, cm));
            } catch (IOException e1) {
                logger.error(String.format("Failed connect to %s.", zkAddr), e1);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        stats.put(path, stat);
    }

//    private static void register2(String zkAddr, String path) {
//        String currTime = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
//        Stat stat;
//
//        try {
//            if (zoo == null) {
//                zoo = new ZooKeeper(zkAddr, 5000, new WWatcher(zkAddr, path));
//            }
//
//
//
////            try {
//                stat = zoo.exists(path, true);
//                if (stat == null) {
//                    zoo.create(path, currTime.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
//                }
////            } catch (Exception e) {
////                logger.error("Failed to create path");
////            }
//
//            String server_addr = path + "/" + "localhost" + ":" + 50054;
//            stat = zoo.exists(server_addr, true);
//            if (stat == null) {
////                try {
//                    zoo.create(server_addr, currTime.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
////                } catch (Exception e) {
////                    logger.error("Failed to create server_data");
////                }
//            } else {
////                try {
//                    zoo.setData(server_addr, currTime.getBytes(), stat.getVersion());
////                } catch (Exception e) {
////                    logger.error("Failed to update server_data");
////                }
//            }
//        } catch (KeeperException.BadVersionException) {
//            zoo.delete(server_addr, stat.getVersion());
//        } catch (KeeperException.ConnectionLossException e) {
//            logger.error(String.format("Connection to %s is lost. Reconnecting", zkAddr), e);
//
//            try {
//                zoo = new ZooKeeper(zkAddr, 5000, new WWatcher(zkAddr, path));
//            } catch (IOException e1) {
//                logger.error(String.format("Failed connect to %s.", zkAddr), e1);
//            }
//        } catch (Exception e) {
//            logger.error(e.getMessage(), e);
//
//
//        }
//    }

    private static class WWatcher implements Watcher {
        private final String path;
        private final String zkAddr;
        private final byte[] data;
        private final CreateMode cm;

        public WWatcher(String zkAddr, String path, byte[] data, CreateMode cm) {
            this.zkAddr = zkAddr;
            this.path = path;
            this.data = data;
            this.cm = cm;
        }

        @Override
        public void process(WatchedEvent we) {
            if (we.getState() == Event.KeeperState.SyncConnected) {
                register(zkAddr, path, data, cm);
            }
        }
    }

//    private static void register(String zkAddr, String path) {
//        try {
//            String currTime = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
//
//            final CountDownLatch connectedSignal = new CountDownLatch(1);
//            ZooKeeper zoo = new ZooKeeper("localhost:2181", 5000, new Watcher() {
//                public void process(WatchedEvent we) {
//                    if (we.getState() == Event.KeeperState.SyncConnected) {
//                        connectedSignal.countDown();
//                    }
//                }
//            });
//            connectedSignal.await();
//
//            Stat stat;
//            try {
//                stat = zoo.exists(path, true);
//                if (stat == null) {
//                    zoo.create(path, currTime.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
//                }
//            } catch (Exception e) {
//                logger.error("Failed to create path");
//            }
//
//            String server_addr = path + "/" + "localhost" + ":" + 2181;
//            try {
//                stat = zoo.exists(server_addr, true);
//                if (stat == null) {
//                    try {
//                        zoo.create(server_addr, currTime.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
//                    } catch (Exception e) {
//                        logger.error("Failed to create server_data");
//                    }
//                } else {
//                    try {
//                        zoo.setData(server_addr, currTime.getBytes(), stat.getVersion());
//                    } catch (Exception e) {
//                        logger.error("Failed to update server_data");
//                    }
//                }
//            } catch (Exception e) {
//                logger.error("Failed to add server_data");
//            }
//        } catch (IOException e) {
//            logger.error(e.getMessage(), e);
//        } catch (InterruptedException e) {
//            logger.error(String.format("Failed connect to %s. Reconnecting", zkAddr), e);
//
//
//        }
//    }

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
