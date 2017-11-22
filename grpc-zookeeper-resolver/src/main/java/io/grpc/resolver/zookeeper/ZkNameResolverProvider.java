package io.grpc.resolver.zookeeper;

import io.grpc.Attributes;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.URI;

public class ZkNameResolverProvider extends NameResolverProvider {
    private static final Logger logger = LoggerFactory.getLogger(ZkNameResolverProvider.class);

    private static final String SCHEME = "zk";

    @Override
    protected boolean isAvailable() {
        return true;
    }

    @Override
    protected int priority() {
        return 5;
    }

    @Override
    public String getDefaultScheme() {
        return SCHEME;
    }

    @Nullable
    @Override
    public NameResolver newNameResolver(URI targetUri, Attributes params) {
        String authority = targetUri.getAuthority();
        String service = targetUri.getPath().substring(1);

        return new ZkNameResolver(authority, service);
    }
}
