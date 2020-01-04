package org.hazelcast.poc.config;

import org.hazelcast.poc.common.HazelcastClientProperties;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
public class HazelcastClientConfiguration {
    @Autowired
    private HazelcastProperties hazelcastProperties;

    @Bean(name = "pocClient01HazelcastInstance", destroyMethod = "shutdown")
    @DependsOn("hazelcastInstance")
    public HazelcastInstance pocClient01HazelcastInstance() {
        HazelcastClientProperties pocClient01 = this.hazelcastProperties.getPocClient01();

        final ClientConfig config = new ClientConfig();
        config.setInstanceName(pocClient01.getInstanceName());
        config.setNetworkConfig(pocClient01.buildNetworkConfig());
        config.getGroupConfig()//
                .setName(pocClient01.getGroupName());
                //.setGroupPassword(pocClient01.getGroupPassword());

        return HazelcastClient.newHazelcastClient(config);
    }
}
