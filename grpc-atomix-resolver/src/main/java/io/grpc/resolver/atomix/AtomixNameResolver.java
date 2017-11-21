package io.grpc.resolver.atomix;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import io.atomix.Atomix;
import io.atomix.AtomixClient;
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.netty.NettyTransport;
import io.atomix.group.DistributedGroup;
import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.GuardedBy;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AtomixNameResolver extends NameResolver {

    private final static Logger LOGGER = LoggerFactory.getLogger(AtomixNameResolver.class);
    private final String authority;

    private final List<Address> cluster;
    private final String service;
    @GuardedBy("this")
    private boolean shutdown;
    @GuardedBy("this")
    private Listener listener;
    @GuardedBy("this")
    private AtomixClient client;

    public AtomixNameResolver(String authority, List<Address> cluster, String service) {
        this.authority = authority;
        this.cluster = cluster;
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

        client = AtomixClient.builder().withTransport(new NettyTransport()).build();

        DistributedGroup group;
        try {
            Atomix atomix = client.connect(cluster).get();
            group = atomix.getGroup(service).get();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            listener.onError(Status.UNAVAILABLE.withCause(e));
            return;
        }
        group.onJoin(m -> refreshServers(listener, group));
        group.onLeave(m -> refreshServers(listener, group));

        refreshServers(listener, group);
    }

    private void refreshServers(Listener listener, DistributedGroup group) {
        List<EquivalentAddressGroup> servers = null;
        try {
            servers = group.members().stream()
                    .map(member -> member.<Map<String, InetSocketAddress>>metadata().get().get("address"))
                    .map(address -> new EquivalentAddressGroup(address, Attributes.EMPTY))
                    .collect(Collectors.toList());
            LOGGER.warn("Servers: {}", servers);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            listener.onError(Status.UNAVAILABLE.withCause(e));
        }
        listener.onAddresses(servers, Attributes.EMPTY);
    }

    public final synchronized void shutdown() {
        if (!this.shutdown) {
            this.shutdown = true;
            try {
                client.close().get();
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
    }
}
