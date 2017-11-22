package old;

import com.google.common.base.Throwables;
import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

public class ZookeeperZoneAwareNameResolver extends NameResolver {

    public final String ZONE_KEY = "ZONE";


    private final URI targetUri;
    private final ZookeeperServiceRegistrationOps zookeeperServiceRegistry;
    private final Comparator<ZookeeperServiceRegistrationOps.HostandZone> zoneComparator;

    public ZookeeperZoneAwareNameResolver(URI targetUri, ZookeeperServiceRegistrationOps zookeeperServiceRegistry,
                                          Comparator<ZookeeperServiceRegistrationOps.HostandZone> zoneComparator) {
        this.targetUri = targetUri;
        this.zookeeperServiceRegistry = zookeeperServiceRegistry;
        this.zoneComparator = zoneComparator;
    }


    @Override
    public String getServiceAuthority() {
        return targetUri.getAuthority();
    }

    @Override
    public void start(Listener listener) {
        //FORMAT WILL BE: zk://serviceName
        String serviceName = targetUri.getAuthority();

        try {
            List<ZookeeperServiceRegistrationOps.HostandZone> initialDiscovery = zookeeperServiceRegistry.discover(serviceName);
            List<List<EquivalentAddressGroup>> initialServers = convertToResolvedServers(initialDiscovery);
            listener.onAddresses(initialServers.stream().reduce((equivalentAddressGroups, equivalentAddressGroups2) -> {
                List<EquivalentAddressGroup> all = new ArrayList<EquivalentAddressGroup>();
                all.addAll(equivalentAddressGroups);
                all.addAll(equivalentAddressGroups2);
                return all;

            }).get(), Attributes.EMPTY);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

        try {
            zookeeperServiceRegistry.watchForUpdates(serviceName, updatedList -> {
                List<List<EquivalentAddressGroup>> resolvedServers = convertToResolvedServers(updatedList);
                listener.onAddresses(resolvedServers.stream().reduce((equivalentAddressGroups, equivalentAddressGroups2) -> {
                    List<EquivalentAddressGroup> all = new ArrayList<EquivalentAddressGroup>();
                    all.addAll(equivalentAddressGroups);
                    all.addAll(equivalentAddressGroups2);
                    return all;

                }).get(), Attributes.EMPTY);
            });
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

    }

    private List<List<EquivalentAddressGroup>> convertToResolvedServers(List<ZookeeperServiceRegistrationOps.HostandZone> newList) {
        return newList.stream().sorted(zoneComparator).map(hostandZone -> {
            try {
                URI hostURI = hostandZone.getHostURI();
                InetAddress[] allByName = InetAddress.getAllByName(hostURI.getHost());

                return Arrays.stream(allByName)
                        .map(inetAddr -> new InetSocketAddress(inetAddr, hostURI.getPort()))
                        .map(sockAddr -> new EquivalentAddressGroup(sockAddr,
                                Attributes.newBuilder().set(Attributes.Key.of(ZONE_KEY), hostandZone.getZone()).build()))
                        .collect(Collectors.toList());
            } catch (UnknownHostException e) {
                throw Throwables.propagate(e);
            }
        }).collect(Collectors.toList());
    }

    @Override
    public void shutdown() {
        try {
            zookeeperServiceRegistry.close();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

}
