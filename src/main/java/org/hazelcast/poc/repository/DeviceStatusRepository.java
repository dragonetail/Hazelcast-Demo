package org.hazelcast.poc.repository;

import org.hazelcast.poc.model.DeviceStatus;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceStatusRepository extends CrudRepository<DeviceStatus, Integer> {

    Optional<DeviceStatus> findByNo(String no);

    void deleteByNo(String no);


}