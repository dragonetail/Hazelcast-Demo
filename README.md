# Hazelcast-Demo
Hazelcast缓存及同Spring boot生态结合的预研Demo。

# 参考
https://medium.com/@igorkosandyak/spring-boot-with-hazelcast-b04d13927745

https://docs.hazelcast.org/docs/latest/manual/html-single/

https://www.baeldung.com/java-hazelcast



# 初始化项目
使用Spring的项目初始化工具（https://start.spring.io/）生成初始项目模板。  
主要选择了： Spring Web、JPA、Cache、H2。  
详细可以参照HELP.md。

# 追加Hazelcast的依赖
```xml
<dependency> <!-- Hazelcat核心包依赖 -->
  <groupId>com.hazelcast</groupId>
  <artifactId>hazelcast</artifactId>
</dependency>
<dependency> <!-- Hazelcat客户端包依赖 -->
  <groupId>com.hazelcast</groupId>
  <artifactId>hazelcast-client</artifactId>
</dependency>
<dependency> <!-- Hazelcat同Spring集成包依赖 -->
  <groupId>com.hazelcast</groupId>
  <artifactId>hazelcast-spring</artifactId>
</dependency>
<dependency> <!-- Hazelcat支持Hibernate包依赖 -->
  <groupId>com.hazelcast</groupId>
  <artifactId>hazelcast-hibernate53</artifactId>
  <version>1.3.2</version>
</dependency>
```

# 配置Hazelcast管理中心

使用Docker镜像运行管理中心：

```shell
docker run -p 8080:8080 hazelcast/management-center
```



# Spring应用配置和使用Hazelcast

根据Hazelcast在项目中的实践，抽象出Hazelcast常用参数，提炼为HazelcastServerProperties、HazelcastClientProperties和HazelcastProperties使用Spring参数进行配置：

```yaml
hazelcast:
  pocServer01:  # hazelcast服务器配置变量名字，支持定义多个
    instance_name: pocServer01  # 缓存实例名称，内存标识用
    group_name: pocHazelcastCache01 # 缓存网络Group名称
    port: 55100  # 服务侦听端口
    # interfaces: 10.10.1.*, 10.3.10.4-18  # 多网卡时候，标注服务侦听的网卡地址
    # members: 10.10.1.2:55100, server-03:55100  # 集群部署各个节点地址，建议使用域名配合底层部署
    management_center_enabled: true  #接入管理中心的参数
    management_center_url: http://localhost:8080/hazelcast-mancenter
    management_center_update_interval: 5
  pocClient01: # hazelcast客户端配置变量名字，支持定义多个
    instance_name: pocClient01  # 客户端缓存实例名称，内存标识用
    group_name: pocHazelcastCache01  # 缓存网络Group名称，同服务端保持一致，标识客户端接入服务端的标识
    server_address: 127.0.0.1:55100 #多个地址，支持逗号分隔
```



# 内嵌服务器端的配置方式

通过上面设置的服务器参数，构建HazelcastServerConfiguration实现服务器的动态配置。

根据项目需要，可以支持配置多个服务器缓存实例（实际场景应用需求不大）。通常一个应用服务只配置一个缓存服务。

```java
package com.github.dragonetail.hazelcast.config;

import com.github.dragonetail.hazelcast.common.hazelcast.HazelcastServerProperties;
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
```

通常情况，一个Spring的应用服务只部署一个缓存服务。

# Spring缓存实现配置

上面配置定义的缓存实例同时作为Spring全局的Cache实现，上面的CacheManager显示地声明了这个配置。

```Java
@Bean
    public CacheManager cacheManager() {
        final CacheManager cacheManager =
                new com.hazelcast.spring.cache.HazelcastCacheManager(this.hazelcastInstance());
        return cacheManager;
    }
```

# Hibernate的二级缓存实现配置

通过配置Hibernate的Spring JPA参数，可以明确声明使用Hazelcast作为Hibernate的二级缓存实现。

```yaml
spring:
  application:
    name: employee-service
  datasource:
    url: jdbc:h2:mem:example-app;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password: h2
    driverClassName: org.h2.Driver
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        show_sql: true
        format_sql: true
        use_sql_comments: true
        cache:
          use_query_cache: true
          use_second_level_cache: true
          hazelcast:
            instance_name: pocServer01  # 内存中查找对应的Hazelcast实例的名称
            # native_client_instance_name: pocClient01 # 客户端的有情况，内存中对应实例名称
          region:
            factory_class: com.hazelcast.hibernate.HazelcastCacheRegionFactory # Hibernate实体对应缓存分区构造方法，可以选择使用HazelcastLocalCacheRegionFactory，详细参见后文解释
```

# Hazelcast客户端配置实现

上面配置定义的缓存实例同时作为Spring全局的Cache实现，上面的CacheManager显示地声明了这个配置。

```Java
package com.github.dragonetail.hazelcast.config;

import com.github.dragonetail.hazelcast.common.hazelcast.HazelcastClientProperties;
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
```

# 测试验证

1、使用Hazelcast服务器配置访问缓存，参见HazelcastMapServerPocController。

具体使用http://localhost:5000/swagger-ui.html访问[hazelcast-map-server-poc-controller](http://localhost:5000/swagger-ui.html#/hazelcast-map-server-poc-controller)进行测试。

当使用put接口存储数据后，通过http://localhost:8080/hazelcast-mancenter/pocHazelcastCache01/maps可以查看到[pocServer01.map01](http://localhost:8080/hazelcast-mancenter/pocHazelcastCache01/maps/pocServer01.map01)缓存中有对应的数据，由于这个Map缓存的过期时间设置为60秒，一分钟后，数据将会被自动删除。

可以通过get和getMap接口查看缓存中的数据。

这种模式等价于EHCache的经典用法，JVM内嵌缓存服务实现。

同时对于集群环境，支持backup-count和async-backup-count，以及read-backup-data等集群备份和加速访问机制。



2、缓存客户端访问缓存，参见HazelcastMapClientPocController。

访问[hazelcast-map-client-poc-controller](http://localhost:5000/swagger-ui.html#/hazelcast-map-client-poc-controller)的get和put接口，可以操作缓存中的数据，由于这个client和上面服务器使用的一个缓存，因此访问的是同一数据内容。

这种模式等价于Redis等C/S模式的缓存访问实现，同时Hazelcast还支持near-cache本地加速缓存模式。



3、Spring缓存集成，参见SpringCachePocController。

访问[spring-cache-poc-controller](http://localhost:5000/swagger-ui.html#/spring-cache-poc-controller)的getTime和clear接口，可以操作缓存中的数据。

由于对应缓存的配置在HazelcastServerBeansConfiguration中，是配置60秒超时。

```Java
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
```

因此在60秒内，重复访问getTime接口会返回相同的结果内容。

这在某些业务场景是非常有实用价值的，能够极大增强系统的抗压能力，例如机票查询结果。



4、****** Spring缓存集成，参见SpringCachePocController。

访问[spring-cache-poc-controller](http://localhost:5000/swagger-ui.html#/spring-cache-poc-controller)的getTime和clear接口，可以操作缓存中的数据。

由于对应缓存的配置在HazelcastServerBeansConfiguration中，是配置60秒超时。

```Java
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
```

因此在60秒内，重复访问getTime接口会返回相同的结果内容。

这在某些业务场景是非常有实用价值的，能够极大增强系统的抗压能力，例如机票查询结果。



5、HIberante二级缓存，参见PersonController。

在Person的Model上设置了二级缓存之后，后续通过ID访问实体，会减少SQL的访问次数。

```Java
@Cache(region="model.Person", usage = CacheConcurrencyStrategy.READ_WRITE)
@Data
@Entity
public class Person implements Serializable {
    private static final long serialVersionUID = 1964134334737487195L;
```

有关Hibernate的二级缓存，可以参考：

https://dzone.com/articles/pitfalls-hibernate-second-0

https://www.baeldung.com/hibernate-second-level-cache

https://github.com/hazelcast/hazelcast-hibernate5

https://github.com/hazelcast/hazelcast-hibernate/blob/master/README.md

缓存的级别有：

- **NONE**： 不使用二级缓存。
- **READ_ONLY**：数据库数据不会更改，简单高效。对于固定数据字典类数据可以使用。
- **NONSTRICT_READ_WRITE**:  数据事务提交后，会更新缓存，期间会出现数据不一致可能。对于变更很少，数据一致要求不高的，例如数据字典、基础参考类数据可以使用。
- **READ_WRITE**: 会在缓存级别上追加软锁，控制在事务变更数据的时候让缓存访问直接访问数据库。分布式情况缓存不一定能够完整保证，普通事务要求不是非常严格的**读多写少**的数据可以使用。Hazelcast支持集群情况的变更通知机制，一定程度能够保证集群中数据的一致性。
- **TRANSACTIONAL**：对分布式集群实施强事务一致性约束，目前Hazelcast不支持。



6、使用缓存IMap手动实现数据的缓存，参见EmployeeController和EmployeeService。

主要展示了再Hazelcast的缓存对象的不同属性上的查找方法，可以设置属性查找索引，通过缓存没有找到再到数据库中查找。

这种实现模式适合特别复杂的对象，需要根据属性进行多级查询的缓存场景，可以有效减少缓存Map的数量。

如果不涉及多属性多级缓存，只是基础的ID缓存的话，建议直接使用Spring的Cachable、CacheEvict、CachePut注解组直接在Repository或者Service方法上即可完成缓存对象的管理实现。



7、ID生成，参见IdGeneratorController。

Hazelcast提供了用时间、节点、序列在集群上提供全局唯一ID的实现，能够实现基本有序的序列，序列中会跳号。

相关提供了：

- generate： 生成全局Long的ID。伪顺序长整数唯一序列，常用业务ID使用
- generateCode： 根据全局Long的ID，生成伪乱序编码（目前为12字符），常用作业务代码、编码使用，已考虑HBase等数据分区。
- generateNonceToken： 根据随机函数生成一次性Token码（32字符），常用做Token。



8、MapStore的使用，参见DeviceStatusController。

Hazelcast提供了MapStore，可以实现缓存到DB（可以是任何持久化后端）的机制。

```Java
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
```

配置参数提供了延迟写，批量写，合并写，透传读的能力。

```Java
@Slf4j
@Component
public class DeviceStatusMapStore extends MapStoreAdapter<String, DeviceStatus> {
    @Autowired
    private DeviceStatusRepository deviceStatusRepository;

    @Override
    public DeviceStatus load(final String no) {
        Assert.notNull(no, "No should not be null.");

        Optional<DeviceStatus> optionalDeviceStatus = deviceStatusRepository.findByNo(no);
        if(optionalDeviceStatus.isPresent()){
            return optionalDeviceStatus.get();
        }
        return null;
    }

    @Override
    public void delete(String no) {
        Assert.notNull(no, "No should not be null.");
        deviceStatusRepository.deleteByNo(no);
    }

    @Override
    public void store(String no, DeviceStatus deviceStatus) {
        Assert.notNull(no, "No should not be null.");
        Assert.notNull(deviceStatus, "deviceStatus should not be null.");

        DeviceStatus deviceStatusForSave = deviceStatusRepository.findByNo(no).orElse(deviceStatus);
        deviceStatusForSave.setValue(deviceStatus.getValue());
        deviceStatusForSave.setLastUpdated(deviceStatus.getLastUpdated());

        deviceStatusRepository.save(deviceStatusForSave);
    }

    @Override
    public void storeAll(final Map<String, DeviceStatus> map) {
        final List<DeviceStatus> dataList = new ArrayList<>();
        map.forEach((no, deviceStatus) -> {
            DeviceStatus deviceStatusForSave = deviceStatusRepository.findByNo(no).orElse(deviceStatus);
            deviceStatusForSave.setValue(deviceStatus.getValue());
            deviceStatusForSave.setLastUpdated(deviceStatus.getLastUpdated());

            dataList.add(deviceStatusForSave);
        });
        this.deviceStatusRepository.saveAll(dataList);
    }
}
```

目前IoT的监控数据指标的部分，非常适合这个场景，实现会更加简单可控。



9、队列的使用，参见QueueTaskController。

Hazelcast提供了可持久化的队列实现，可以实现缓存到DB（可以是任何持久化后端）的机制。

目前IoT的任务下发工作场景比较适合这个机制。

本来想直接使用Queue的Key作为数据库Model的ID，这个目前不知道为什么在往队列添加数据的时候，保存到数据时底层会触发HIbernate的Query的Cache，需要把JPA中的参数【use_query_cache: false】才能好用，另外一个规避方案就是不使用Queue中提供的Key作为ID，使用数据中默认的ID（注： 已经更改成Key作为唯一列，ID为DB自动生成的模式，结果也是不行，问题应该是出在了Hazelcast在一个Thread中两次调用嵌套的问题。。。。）。

因此建议这个模式之前使用Hazelcast的Queue，需要【use_query_cache: false】才行。

这个在DeviceStatus使用MapStore的时候没有问题，估计应该是Queue对应的一个Bug吧。



10、调度的使用，参见EchoTaskSchduler。

```Java
@Slf4j
@Component
public class EchoTaskSchduler {
    @Autowired
    private EchoTask echoTask;

    @Autowired
    private IScheduledExecutorService scheduledExecutorService;

    private IScheduledFuture<?> scheduledFuture;

    @PostConstruct
    public void init() {
        scheduledFuture= scheduledExecutorService.scheduleAtFixedRate(
                TaskUtils.named("echoTask", echoTask), 10, 30, TimeUnit.SECONDS);
        log.info("EchoTask has been started.");
    }

    @PreDestroy
    public void shutdown() {
        scheduledFuture.cancel(true);
        scheduledFuture.dispose();
        log.info("EchoTask has been shutdown.");
    }
}
```

调度固定间隔的任务，如果任务执行过长，下一次激活时发现任务还在执行，会自动跳过任务触发，保证任务不累积调度。



10、其他说明，以上所有测试可以配合使用[http://localhost:5000/console ](http://localhost:5000/console )进行数据库确认，另外，日志中数据库SQL日志已经打开。



11、测试多节点启动
```
java -Dhazelcast.pocServer01.port=55100 \
 -Dhazelcast.pocServer01.members=127.0.0.1:55100,127.0.0.1:55101,127.0.0.1:55102,127.0.0.1:55103,127.0.0.1:55104 \
 -jar target/Hazelcast-Demo-0.0.1-SNAPSHOT.jar

java -Dhazelcast.pocServer01.port=55101 \
    -Dserver.port=8081 \
 -Dhazelcast.pocServer01.members=127.0.0.1:55100,127.0.0.1:55101,127.0.0.1:55102,127.0.0.1:55103,127.0.0.1:55104 \
 -jar target/Hazelcast-Demo-0.0.1-SNAPSHOT.jar
 
 java -Dhazelcast.pocServer01.port=55102 \
    -Dserver.port=8082 \
  -Dhazelcast.pocServer01.members=127.0.0.1:55100,127.0.0.1:55101,127.0.0.1:55102,127.0.0.1:55103,127.0.0.1:55104 \
  -jar target/Hazelcast-Demo-0.0.1-SNAPSHOT.jar
  
 java -Dhazelcast.pocServer01.port=55103 \
    -Dserver.port=8083 \
  -Dhazelcast.pocServer01.members=127.0.0.1:55100,127.0.0.1:55101,127.0.0.1:55102,127.0.0.1:55103,127.0.0.1:55104 \
  -jar target/Hazelcast-Demo-0.0.1-SNAPSHOT.jar
  
 java -Dhazelcast.pocServer01.port=55104 \
    -Dserver.port=8084 \
  -Dhazelcast.pocServer01.members=127.0.0.1:55100,127.0.0.1:55101,127.0.0.1:55102,127.0.0.1:55103,127.0.0.1:55104 \
  -jar target/Hazelcast-Demo-0.0.1-SNAPSHOT.jar
```

