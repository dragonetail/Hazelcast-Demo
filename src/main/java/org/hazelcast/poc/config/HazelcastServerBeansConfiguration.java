package org.hazelcast.poc.config;

import org.hazelcast.poc.model.DeviceStatus;
import org.hazelcast.poc.model.Employee;
import org.hazelcast.poc.model.QueuePocTask;
import org.hazelcast.poc.store.DeviceStatusMapStore;
import org.hazelcast.poc.store.QueueTaskQueueStore;
import com.hazelcast.config.*;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import com.hazelcast.flakeidgen.FlakeIdGenerator;
import com.hazelcast.scheduledexecutor.IScheduledExecutorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.Map;

@Configuration
public class HazelcastServerBeansConfiguration {
    @Qualifier("hazelcastInstance")
    @Autowired
    private HazelcastInstance hazelcastInstance;

    @Autowired
    private DeviceStatusMapStore deviceStatusMapStore;

    @Autowired
    private QueueTaskQueueStore queueTaskQueueStore;

    @PostConstruct
    public void init() {
        Config config = hazelcastInstance.getConfig();

        config.getMapConfig("model.Person")
                .setTimeToLiveSeconds(600)
                .setMaxIdleSeconds(600); // 1分钟
    }

    @Bean(name = "pocServer01Map01")
    public Map<String, String> pocServer01Map01() {
        Config config = hazelcastInstance.getConfig();

        // Basic Hazelcast Map Poc for server and client
        String name = "pocServer01.map01";
        config.getMapConfig(name)
                .setTimeToLiveSeconds(60)
                .setMaxIdleSeconds(60); // 1分钟

        return hazelcastInstance.getMap(name);
    }


    @Bean(name = "springCachePoc01")
    public Map<String, String> springCachePoc01() {
        Config config = hazelcastInstance.getConfig();

        // Spring Cache Poc
        String name = "pocServer01.springCachePoc01";
        config.getMapConfig(name)
                .setTimeToLiveSeconds(60)
                .setMaxIdleSeconds(60); // 1分钟

        return hazelcastInstance.getMap(name);
    }

    @Bean(name = "employeeCacheMap")
    public IMap<Integer, Employee> employeeCacheMap() {
        Config config = hazelcastInstance.getConfig();
        
        // Hibernate second 2 cache for Employee
        String name = "pocServer01.employee";
        config.getMapConfig(name)
                .setTimeToLiveSeconds(3600)
                .setMaxIdleSeconds(3600); // 1小时

        IMap<Integer, Employee> employeeCacheMap = hazelcastInstance.getMap(name);
        employeeCacheMap.addIndex("company", true);
        return employeeCacheMap;
    }

    @Bean(name = "employees")
    public IMap<Integer, Employee> employees() {
        Config config = hazelcastInstance.getConfig();

        // Hibernate second 2 cache for Employee
        String name = "pocServer01.employees";
        config.getMapConfig(name)
                .setTimeToLiveSeconds(3600)
                .setMaxIdleSeconds(3600); // 1小时

        IMap<Integer, Employee> employeeCacheMap = hazelcastInstance.getMap(name);
        employeeCacheMap.addIndex("company", true);
        return employeeCacheMap;
    }

    @Bean(name = "deviceStatusCacheMap")
    public Map<String, DeviceStatus> deviceStatusCacheMap() {
        Config config = hazelcastInstance.getConfig();

        // MapStore Poc
        MapConfig mapConfig = config.getMapConfig("pocServer01.deviceStatus")
                .setTimeToLiveSeconds(120) // 测试用120，应该足够大，比如3600秒
                .setMaxIdleSeconds(120)
                .setEvictionPolicy(EvictionPolicy.LRU);// 1小时
        final MapStoreConfig mapStoreConfig = mapConfig.getMapStoreConfig();
        mapStoreConfig.setImplementation(deviceStatusMapStore)
                .setEnabled(true)
                .setWriteDelaySeconds(60)
                .setWriteBatchSize(1000)
                .setWriteCoalescing(true);
        config.addMapConfig(mapConfig);

        return hazelcastInstance.getMap("pocServer01.deviceStatus");
    }

    @Bean(name = "pocFlakeIdGenerator01")
    public FlakeIdGenerator pocFlakeIdGenerator01() {
        //Config config = hazelcastInstance.getConfig();
        //FlakeIdGeneratorConfig flakeIdGeneratorConfig = config.getFlakeIdGeneratorConfig("pocServer01.pocFlakeIdGenerator01");

        return hazelcastInstance.getFlakeIdGenerator("pocServer01.pocFlakeIdGenerator01");
    }

    @Bean(name = "pocQueue01")
    public IQueue<QueuePocTask> pocQueue01() {
        Config config = hazelcastInstance.getConfig();

        // QueueStore Poc
        String name = "pocServer01.pocQueue01";
        QueueConfig queueConfig = config.getQueueConfig(name);
        QueueStoreConfig queueStoreConfig = new QueueStoreConfig();
        queueStoreConfig.setStoreImplementation(queueTaskQueueStore)
                .setEnabled(true)
                .setProperty("binary", "false")
                .setProperty("memory-limit", "1000")
                .setProperty("bulk-load", "50");
        queueConfig.setQueueStoreConfig(queueStoreConfig);
        config.addQueueConfig(queueConfig);

        return hazelcastInstance.getQueue(name);
    }

    @Bean(name = "scheduledExecutorService")
    public IScheduledExecutorService scheduledExecutorService() {
        Config config = hazelcastInstance.getConfig();

        ScheduledExecutorConfig scheduledExecutorConfig = config.getScheduledExecutorConfig("myScheduledExecSvc")
                .setPoolSize(16)
                .setCapacity(100)
                .setDurability(1)
                .setQuorumName("quorumname");
        config.addScheduledExecutorConfig(scheduledExecutorConfig);

        return hazelcastInstance.getScheduledExecutorService("pocServer01.scheduledExecutorService");
    }


    @Bean(name = "pocCounter01")
    public IAtomicLong pocCounter01() {
        return hazelcastInstance.getAtomicLong( "pocServer01.pocCounter01" );
    }


}
