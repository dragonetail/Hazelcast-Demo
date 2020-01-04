package org.hazelcast.poc.config;

import org.hazelcast.poc.common.HazelcastClientProperties;
import org.hazelcast.poc.common.HazelcastServerProperties;
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