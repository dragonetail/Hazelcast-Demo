package org.hazelcast.poc.controller;

import org.hazelcast.poc.model.QueuePocTask;
import com.hazelcast.core.IQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/device/status")
public class QueueTaskController {

    private final IQueue<QueuePocTask> pocQueue01;

    @Autowired
    QueueTaskController(IQueue<QueuePocTask> pocQueue01) {
        this.pocQueue01 = pocQueue01;
    }

    @GetMapping(value = "/offer/{type}/{value}")
    public String put(@PathVariable Integer type, @PathVariable String value ) {
        QueuePocTask queuePocTask = new QueuePocTask();
        queuePocTask.setType(type);
        queuePocTask.setValue(value);
        queuePocTask.setSubmittedTime(LocalDateTime.now());

        boolean result = pocQueue01.offer(queuePocTask);
        // offer(E e, long timeout, TimeUnit unit) & put(E e) will be blocking
        // put(E e) should throw IllegalStateException

        return result ? "OK": "NG";
    }

    @GetMapping(value = "/poll")
    public QueuePocTask poll() {
        QueuePocTask queuePocTask = pocQueue01.poll();
        return queuePocTask;
        // poll(long timeout, TimeUnit unit)  & take will be blocking
    }
}