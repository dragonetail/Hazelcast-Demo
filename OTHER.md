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



4、Spring缓存集成，参见SpringCachePocController。

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





以上所有测试可以配合使用http://localhost:5000/console进行数据库确认，另外，日志中数据库SQL日志已经打开。









其他：

Map: async-backup-count for a map is 1, and read-backup-data is true
https://stackoverflow.com/questions/48480100/hazelcast-read-backup-data-vs-near-cache
near cache has a bunch of configuration options, and you can decide how much data you need to access locally at any type of setup (including client-server topology). 


https://docs.hazelcast.org/docs/3.3-RC1/manual/html/map-backups.html
https://docs.hazelcast.org/docs/latest/manual/html-single/#getting-a-topic-and-publishing-messages

http://localhost:5000/v2/api-docs
http://localhost:5000/swagger-ui.html
http://localhost:5000/console



docker run -p 8080:8080 hazelcast/management-center



https://dzone.com/articles/pitfalls-hibernate-second-0

https://www.baeldung.com/hibernate-second-level-cache



https://github.com/hazelcast/spring-data-hazelcast
@EnableHazelcastRepositories(basePackages={"example.springdata.keyvalue.chemistry"}) 
public interface SpeakerRepository extends HazelcastRepository<Speaker, Long> {}

https://github.com/hazelcast/hazelcast-hibernate5
https://github.com/hazelcast/hazelcast-hibernate/blob/master/README.md

# Table of Contents

* [Hibernate Second Level Cache](#hibernate-second-level-cache)
  * [Sample Code for Hibernate](#sample-code-for-hibernate)
  * [Supported Hibernate and Hazelcast Versions](#supported-hibernate-and-hazelcast-versions)
  * [Configuring Hibernate for Hazelcast](#configuring-hibernate-for-hazelcast)
    * [Enabling Second Level Cache](#enabling-second-level-cache)
    * [Configuring RegionFactory](#configuring-regionfactory)
      * [HazelcastCacheRegionFactory](#hazelcastcacheregionfactory)
      * [HazelcastLocalCacheRegionFactory](#hazelcastlocalcacheregionfactory)
    * [Configuring Query Cache and Other Settings](#configuring-query-cache-and-other-settings)
  * [Configuring Hazelcast for Hibernate](#configuring-hazelcast-for-hibernate)
  * [Setting P2P for Hibernate](#setting-p2p-for-hibernate)
  * [Setting Client/Server for Hibernate](#setting-client-server-for-hibernate)
  * [Configuring Cache Concurrency Strategy](#configuring-cache-concurrency-strategy)
  * [Advanced Settings](#advanced-settings)

# Hibernate Second Level Cache

Hazelcast provides distributed second level cache for your Hibernate entities, collections and queries.

## Sample Code for Hibernate

Please see our <a href="https://github.com/hazelcast/hazelcast-code-samples/tree/master/hazelcast-integration/hibernate-2ndlevel-cache" target="_blank">sample application</a> for Hibernate Second Level Cache.

## Supported Hibernate and Hazelcast Versions

- Hibernate 3.3+
- Hibernate 4.x
- Hazelcast 3.6+

## Configuring Hibernate for Hazelcast

To configure Hibernate for Hazelcast:

- Add `hazelcast-hibernate3-<`*hazelcastversion*`>.jar` or `hazelcast-
hibernate4-<`*hazelcastversion*`>.jar` into your classpath depending on your Hibernate version.
- Then add the following properties into your Hibernate configuration file, e.g., `hibernate.cfg.xml`.

### Enabling Second Level Cache

```xml
<property name="hibernate.cache.use_second_level_cache">true</property>
```

### Configuring RegionFactory

You can configure Hibernate RegionFactory with `HazelcastCacheRegionFactory` or `HazelcastLocalCacheRegionFactory`.

#### HazelcastCacheRegionFactory

`HazelcastCacheRegionFactory` uses standard Hazelcast Distributed Maps to cache the data, so all cache operations go through the wire.

```xml    
<property name="hibernate.cache.region.factory_class">
   com.hazelcast.hibernate.HazelcastCacheRegionFactory
</property>
```

All operations like `get`, `put`, and `remove` will be performed using the Distributed Map logic. The only downside of using `HazelcastCacheRegionFactory` may be lower performance compared to `HazelcastLocalCacheRegionFactory` since operations are handled as distributed calls.

![image](images/NoteSmall.jpg) ***NOTE:*** *If you use `HazelcastCacheRegionFactory`, you can see your maps on [Management Center](http://docs.hazelcast.org/docs/management-center/latest/manual/html/index.html).*

With `HazelcastCacheRegionFactory`, all of the following caches are distributed across Hazelcast Cluster.

- Entity Cache
- Collection Cache
- Timestamp Cache

#### HazelcastLocalCacheRegionFactory

You can use `HazelcastLocalCacheRegionFactory` which stores data in a local member and sends invalidation messages when an entry is updated/deleted locally.

```xml
<property name="hibernate.cache.region.factory_class">
  com.hazelcast.hibernate.HazelcastLocalCacheRegionFactory
</property>
```

With `HazelcastLocalCacheRegionFactory`, each cluster member has a local map and each of them is registered to a Hazelcast Topic (ITopic). Whenever a `put` or `remove` operation is performed on a member, an invalidation message is generated on the ITopic and sent to the other members. Those other members remove the related key-value pair on their local maps as soon as they get these invalidation messages. The new value is only updated on this member when a `get` operation runs on that key. In the case of `get` operations, invalidation messages are not generated and reads are performed on the local map.

An illustration of the above logic is shown below.

![Invalidation with Local Cache Region Factory](images/HZLocalCacheRgnFactory.jpg)

If your operations are mostly reads, then this option gives better performance.

![image](images/NoteSmall.jpg) ***NOTE:*** *If you use `HazelcastLocalCacheRegionFactory`, you cannot see your maps on [Management Center](https://docs.hazelcast.org/docs/management-center/latest/manual/html/index.html).*

With `HazelcastLocalCacheRegionFactory`, all of the following caches are not distributed and are kept locally in the Hazelcast member.

- Entity Cache
- Collection Cache
- Timestamp Cache

Entity and Collection are invalidated on update. When they are updated on a member, an invalidation message is sent to all other members in order to remove the entity from their local cache. When needed, each member reads that data from the underlying DB. 

Timestamp cache is replicated. On every update, a replication message is sent to all the other members.

Eviction support is limited to maximum size of the map (defined by `max-size` configuration element) and TTL only. When maximum size is hit, 20% of the entries will be evicted automatically.

### Configuring Query Cache and Other Settings

- To enable use of query cache:

	```xml
	<property name="hibernate.cache.use_query_cache">true</property>
	```

- To force minimal puts into query cache:

	```xml
	<property name="hibernate.cache.use_minimal_puts">true</property>
	```

- To avoid `NullPointerException` when you have entities that have composite keys (using `@IdClass`):

    ```xml
	<property name="hibernate.session_factory_name">yourFactoryName</property>
	```
	

![image](images/NoteSmall.jpg) ***NOTE:*** *QueryCache is always LOCAL to the member and never distributed across Hazelcast Cluster.*

## Configuring Hazelcast for Hibernate

To configure Hazelcast for Hibernate, put the configuration file named `hazelcast.xml` into the root of your classpath. If Hazelcast cannot find `hazelcast.xml`, then it will use the default configuration from `hazelcast.jar`.

You can define a custom-named Hazelcast configuration XML file with one of these Hibernate configuration properties. 

```xml
<property name="hibernate.cache.provider_configuration_file_resource_path">
  hazelcast-custom-config.xml
</property>
```


```xml
<property name="hibernate.cache.hazelcast.configuration_file_path">
  hazelcast-custom-config.xml
</property>
```

If you're using Hazelcast client (`hibernate.cache.hazelcast.use_native_client=true`), you can specify a custom Hazelcast client configuration file by using the same parameters.

Hazelcast creates a separate distributed map for each Hibernate cache region. You can easily configure these regions via Hazelcast map configuration. You can define **backup**, **eviction**, **TTL** and **Near Cache** properties.

## Setting P2P for Hibernate

Hibernate Second Level Cache can use Hazelcast in two modes: Peer-to-Peer (P2P) and Client/Server (next section).

With P2P mode, each Hibernate deployment launches its own Hazelcast Instance. You can also configure Hibernate to use an existing instance, instead of creating a new `HazelcastInstance` for each `SessionFactory`. To do this, set the `hibernate.cache.hazelcast.instance_name` Hibernate property to the `HazelcastInstance`'s name. For more information, please see <a href="http://docs.hazelcast.org/docs/latest-dev/manual/html-single/index.html#binding-to-a-named-instance" target="_blank">Named Instance Scope</a>

**Disabling shutdown during SessionFactory.close()**

You can disable shutting down `HazelcastInstance` during `SessionFactory.close()`. To do this, set the Hibernate property `hibernate.cache.hazelcast.shutdown_on_session_factory_close` to false. *(In this case, you should not set the Hazelcast property `hazelcast.shutdownhook.enabled` to false.)* The default value is `true`.


## Setting Client-Server for Hibernate

You can set up Hazelcast to connect to the cluster as Native Client. Native client is not a member; it connects to one of the cluster members and delegates all cluster wide operations to it. Client instance started in the Native Client mode uses Smart Routing: when the relied cluster member dies, the client transparently switches to another live member. All client operations are Retry-able, meaning that the client resends the request as many as 10 times in case of a failure. After the 10th retry, it throws an exception. You cannot change the routing mode and retry-able operation configurations of the Native Client instance used by Hibernate 2nd Level Cache. Please see the <a href="http://docs.hazelcast.org/docs/latest/manual/html-single/index.html#setting-smart-routing" target="_blank">Smart Routing section</a> and <a href="http://docs.hazelcast.org/docs/latest-dev/manual/html-single/index.html##handling-retry-able-operation-failure" target="_blank">Retry-able Operation Failure section</a> for more details.

```xml   
<property name="hibernate.cache.hazelcast.use_native_client">true</property>
```

To set up Native Client, add the Hazelcast **group-name**, **group-password** and **cluster member address** properties. Native Client will connect to the defined member and will get the addresses of all members in the cluster. If the connected member dies or leaves the cluster, the client will automatically switch to another member in the cluster.

```xml  
<property name="hibernate.cache.hazelcast.native_client_address">10.34.22.15</property>
<property name="hibernate.cache.hazelcast.native_client_group">dev</property>
<property name="hibernate.cache.hazelcast.native_client_password">dev-pass</property>
```

You can use an existing client instead of creating a new one by adding the following property.

```xml
<property name="hibernate.cache.hazelcast.native_client_instance_name">my-client</property>
```

![image](images/NoteSmall.jpg) ***NOTE***: *To use Native Client, add `hazelcast-client-<version>.jar` into your classpath. Refer to <a href="http://docs.hazelcast.org/docs/latest/manual/html-single/index.html#hazelcast-java-client" target="_blank">Hazelcast Java Client chapter</a> for more information. Moreover, to configure a Hazelcast Native Client for Hibernate, put the configuration file named `hazelcast-client.xml` into the root of your classpath.*


![image](images/NoteSmall.jpg) ***NOTE***: *To use Native Client, add `hazelcast-<version>.jar`,`hazelcast-hibernate(3,4)-<version>.jar` and `hibernate-core-<version>.jar` into your remote cluster's classpath.*

![image](images/NoteSmall.jpg) ***NOTE***: *If your domain (persisted) classes only have Java primitive type fields, you do not need to add your domain classes into your remote cluster's classpath. However, if your classes have non-primitive type fields, you need to add only these fields' classes (not your domain class) to your cluster's classpath.*

## Configuring Cache Concurrency Strategy

Hibernate has four cache concurrency strategies: *read-only*, *read-write*, *nonstrict-read-write* and *transactional*. Hibernate does not force cache providers to support all those strategies. Hazelcast supports the first three: *read-only*, *read-write*, and *nonstrict-read-write*. It does not yet support *transactional* strategy.

If you are using XML based class configurations, add a *cache* element into your configuration with the *usage* attribute set to one of the *read-only*, *read-write*, or *nonstrict-read-write* strategies.

```xml
<class name="eg.Immutable" mutable="false">
  <cache usage="read-only"/>
  .... 
</class>

<class name="eg.Cat" .... >
  <cache usage="read-write"/>
  ....
  <set name="kittens" ... >
    <cache usage="read-write"/>
    ....
  </set>
</class>
```
If you are using Hibernate-Annotations, then you can add a *class-cache* or *collection-cache* element into your Hibernate configuration file with the *usage* attribute set to *read only*, *read/write*, or *nonstrict read/write*.

```xml    
<class-cache usage="read-only" class="eg.Immutable"/>
<class-cache usage="read-write" class="eg.Cat"/>
<collection-cache collection="eg.Cat.kittens" usage="read-write"/>
```

Or alternatively, you can put Hibernate Annotation's *@Cache* annotation on your entities and collections.

```java    
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Cat implements Serializable {
  ...
}
```

## Advanced Settings

**Changing/setting lock timeout value of *read-write* strategy**

You can set a lock timeout value using the `hibernate.cache.hazelcast.lock_timeout_in_seconds` Hibernate property. The value should be in seconds. The default value is 300 seconds.


