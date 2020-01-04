package org.hazelcast.poc.store;

import org.hazelcast.poc.model.QueuePocTask;
import org.hazelcast.poc.repository.QueueTaskRepository;
import com.hazelcast.core.QueueStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class QueueTaskQueueStore implements QueueStore<QueuePocTask> {
    @Autowired
    private QueueTaskRepository queueTaskRepository;


    @Override
    public void delete(Long key) {
        queueTaskRepository.deleteByKey(key);
    }

    @Override
    public void store(Long key, QueuePocTask queuePocTask) {
        Optional<QueuePocTask> optionalQueuePocTask = queueTaskRepository.findByKey(key);
        queuePocTask = optionalQueuePocTask.orElse(queuePocTask);
        queueTaskRepository.save(queuePocTask);
    }

    @Override
    public void storeAll(Map<Long, QueuePocTask> map) {
        final List<QueuePocTask> dataList = new ArrayList<>();
        map.forEach((key, queuePocTask) -> {
            Optional<QueuePocTask> optionalQueuePocTask = queueTaskRepository.findByKey(key);
            queuePocTask = optionalQueuePocTask.orElse(queuePocTask);

            dataList.add(queuePocTask);
        });
        this.queueTaskRepository.saveAll(dataList);
    }

    @Override
    public void deleteAll(Collection<Long> keys) {
        queueTaskRepository.deleteAllByKey(keys);
    }

    @Override
    public QueuePocTask load(Long key) {
        return queueTaskRepository.findByKey(key).orElse(null);
    }

    @Override
    public Map<Long, QueuePocTask> loadAll(Collection<Long> keys) {
        Iterable<QueuePocTask> all = queueTaskRepository.findAllByKey(keys);

        HashMap<Long, QueuePocTask> result = new HashMap();
        all.forEach((queuePocTask) -> result.put(queuePocTask.getKey(), queuePocTask));

        return result;
    }

    @Override
    public Set<Long> loadAllKeys() {
        return queueTaskRepository.findAllKeys();
    }
}