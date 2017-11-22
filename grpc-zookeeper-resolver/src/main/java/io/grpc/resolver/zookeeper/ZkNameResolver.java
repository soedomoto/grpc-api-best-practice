package io.grpc.resolver.zookeeper;

import com.google.common.base.Preconditions;
import io.grpc.NameResolver;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class ZkNameResolver extends NameResolver {
    private static final Logger logger = LoggerFactory.getLogger(ZkNameResolver.class);

    private final String service;
    private final String authority;
    private Listener listener;

    public ZkNameResolver(String authority, String service) {
        this.authority = authority;
        this.service = service;
    }

    @Override
    public String getServiceAuthority() {
        return this.authority;
    }

    @Override
    public void start(Listener listener) {
        Preconditions.checkState(this.listener == null, "already started");
        this.listener = Preconditions.checkNotNull(listener, "listener");

        try {
            CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient(this.authority,
                    new ExponentialBackoffRetry(1000, 5));
            curatorFramework.start();
            curatorFramework.blockUntilConnected();

            Stat stat = curatorFramework.checkExists().creatingParentContainersIfNeeded().forPath(String.format("/%s", this.service));
            if (stat == null) {
                logger.error(String.format("Service /%s not found !", service));
            }

            List<String> hostPort = curatorFramework.getChildren().forPath(String.format("/%s", this.service));
            List<String> hostPortData = hostPort.stream().map(s -> {
                try {
                    return curatorFramework.getData().forPath(String.format("/%s/%s", this.service, s)).toString();
                } catch (Exception e) {
                    return null;
                }
            }).collect(Collectors.toList());



            String storedString = curatorFramework.getData().forPath(String.format("/%s", this.service)).toString();
            String a = storedString;
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void shutdown() {

    }
}
