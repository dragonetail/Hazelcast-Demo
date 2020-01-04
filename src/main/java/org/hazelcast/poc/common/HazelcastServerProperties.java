package org.hazelcast.poc.common;

import com.hazelcast.config.InterfacesConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.TcpIpConfig;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

@Getter
@Setter
public class HazelcastServerProperties {
    private String instanceName;
    private String groupName;
    private String groupPassword = "groupPassword@dymmy1!";

    private boolean managementCenterEnabled = false;
    private String managementCenterUrl = "http://localhost:8080/mancenter";
    private int managementCenterUpdateInterval = 5;

    private String interfaces;
    private String members;
    private Integer port;

    public HazelcastServerProperties(final String groupName, final Integer port) {
        this.groupName = groupName;
        this.port = port;
    }

    public NetworkConfig buildNetworkConfig() {
        NetworkConfig networkConfig = new NetworkConfig();
        if (port != null) {
            networkConfig.setPort(port);
            return networkConfig;
        }

        if (StringUtils.hasText(interfaces)) {
            final String[] interfaceArray = interfaces.split(",");
            final InterfacesConfig interfacesConfig = networkConfig.getInterfaces().setEnabled(true);
            for (final String ip : interfaceArray) {
                interfacesConfig.addInterface(ip);
            }
            return networkConfig;
        }

        networkConfig.getJoin().getMulticastConfig().setEnabled(false);
        if (StringUtils.hasText(members)) {
            //            final MulticastConfig multicastConfig = networkConfig.getJoin().getMulticastConfig();
            //            final String[] trustedInterfacesArray = trustedInterfaces.split(",");
            //            for (final String ip : trustedInterfacesArray) {
            //                multicastConfig.addTrustedInterface(ip);
            //            }
            final TcpIpConfig tcpIpConfig = networkConfig.getJoin().getTcpIpConfig().setEnabled(true);
            final String[] memberArray = members.split(",");
            for (final String member : memberArray) {
                tcpIpConfig.addMember(member);
            }
            return networkConfig;
        }
        return networkConfig;
    }
}

