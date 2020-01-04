package org.hazelcast.poc.scheduler;

import com.hazelcast.core.IAtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Slf4j
@Component
public class EchoTask implements Runnable {

    @Autowired
    private IAtomicLong pocCounter01;

    @Override
    public void run()  {
        Long count = pocCounter01.incrementAndGet();
        log.info("EchoTask with count: {}", count);
    }
}