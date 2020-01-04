package org.hazelcast.poc.repository;

import org.hazelcast.poc.model.Employee;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Repository
public interface EmployeeRepository extends CrudRepository<Employee, Integer> {

    @Cacheable(value= "pocServer01.employees", key = "#personId")
    Employee findByPersonId(Integer personId);

    List<Employee> findByCompany(String company);

}