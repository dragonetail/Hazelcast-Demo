package org.hazelcast.poc.config;

import org.hazelcast.poc.common.HazelcastServerProperties;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HazelcastServerConfiguration {
    @Autowired
    private HazelcastProperties hazelcastProperties;

    @Bean
    public CacheManager cacheManager() {
        final CacheManager cacheManager =
                new com.hazelcast.spring.cache.HazelcastCacheManager(this.hazelcastInstance());
        return cacheManager;
    }

    @Bean(name = "hazelcastInstance", destroyMethod = "shutdown")
    public HazelcastInstance hazelcastInstance() {
        final Config config = new Config();
        HazelcastServerProperties pocServer01 = this.hazelcastProperties.getPocServer01();

        config.setInstanceName(pocServer01.getInstanceName());
        if (pocServer01.isManagementCenterEnabled()) {
            config.getManagementCenterConfig()
                    .setEnabled(pocServer01.isManagementCenterEnabled())
                    .setUpdateInterval(pocServer01.getManagementCenterUpdateInterval())
                    .setUrl(pocServer01.getManagementCenterUrl());
        }

        config.setNetworkConfig(pocServer01.buildNetworkConfig());
        config.getGroupConfig()
                .setName(pocServer01.getGroupName());
        //.setGroupPassword(pocServer01.getGroupPassword());

        return Hazelcast.newHazelcastInstance(config);
    }
}
