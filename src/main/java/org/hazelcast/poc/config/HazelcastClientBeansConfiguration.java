package org.hazelcast.poc.config;

import com.hazelcast.core.HazelcastInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class HazelcastClientBeansConfiguration {
    @Qualifier("pocClient01HazelcastInstance")
    @Autowired
    private HazelcastInstance hazelcastClientInstance;

    @Bean(name = "pocClient01Map01")
    public Map<String, String> pocClient01Map01() {
        return hazelcastClientInstance.getMap("pocServer01.map01");
    }
}
