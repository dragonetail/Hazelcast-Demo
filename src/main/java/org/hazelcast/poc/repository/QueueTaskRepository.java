package org.hazelcast.poc.repository;

import org.hazelcast.poc.model.QueuePocTask;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface QueueTaskRepository extends org.springframework.data.repository.Repository<QueuePocTask, Long> {
    void deleteAllByKey(Iterable<Long> keys);

    @Query("select p.key from #{#entityName} p")
    Set<Long> findAllKeys();

    QueuePocTask save(QueuePocTask var1);

    Iterable<QueuePocTask> saveAll(Iterable<QueuePocTask> var1);

    Optional<QueuePocTask> findByKey(Long var1);


    Iterable<QueuePocTask> findAllByKey(Iterable<Long> var1);

    void deleteByKey(Long key);
}