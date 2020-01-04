package org.hazelcast.poc.repository;

import org.hazelcast.poc.model.Person;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonRepository extends CrudRepository<Person, Integer> {

    List<Person> findByIdCard(String idCard);

}
