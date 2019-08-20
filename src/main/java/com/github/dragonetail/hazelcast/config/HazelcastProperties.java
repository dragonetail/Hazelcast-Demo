package com.github.dragonetail.hazelcast.config;

import com.github.dragonetail.hazelcast.common.hazelcast.HazelcastClientProperties;
import com.github.dragonetail.hazelcast.common.hazelcast.HazelcastServerProperties;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@ConfigurationProperties(prefix = "hazelcast", ignoreUnknownFields = false)
@Component
public class HazelcastProperties {

    private final HazelcastServerProperties pocServer01 =
            new HazelcastServerProperties("pocHazelcastCache01", 55100);

    private final HazelcastClientProperties pocClient01 =
            new HazelcastClientProperties("pocHazelcastCache01", "127.0.0.1:55100");


}