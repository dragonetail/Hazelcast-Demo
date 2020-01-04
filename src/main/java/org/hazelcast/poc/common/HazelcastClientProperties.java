package org.hazelcast.poc.common;

import com.hazelcast.client.config.ClientNetworkConfig;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HazelcastClientProperties {
    private String instanceName;
    private String groupName;
    private String groupPassword = "groupPassword@dymmy1!";
    private String serverAddress;

    public HazelcastClientProperties(final String groupName, final String serverAddress) {
        super();
        this.groupName = groupName;
        this.serverAddress = serverAddress;
    }

    public ClientNetworkConfig buildNetworkConfig() {
        ClientNetworkConfig networkConfig = new ClientNetworkConfig();

        networkConfig.setConnectionAttemptPeriod(30000);
        networkConfig.setConnectionAttemptLimit(1000);
        if (serverAddress != null) {
            final String[] servers = serverAddress.split(",");
            for (final String server : servers) {
                networkConfig.addAddress(server);
            }
        }

        return networkConfig;
    }
}