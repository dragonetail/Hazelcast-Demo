package com.github.dragonetail.hazelcast.scheduler;


import com.hazelcast.scheduledexecutor.IScheduledExecutorService;
import com.hazelcast.scheduledexecutor.IScheduledFuture;
import com.hazelcast.scheduledexecutor.TaskUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

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