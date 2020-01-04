package org.hazelcast.poc.controller;

import org.hazelcast.poc.model.Person;
import org.hazelcast.poc.repository.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.logging.Logger;


@RestController
@RequestMapping("/persons")
public class PersonController {

    protected Logger logger = Logger.getLogger(PersonController.class.getName());

    @Autowired
    private PersonRepository repository;
    @Autowired
    private CacheManager cacheManager;

    @PostConstruct
    public void init() {
        logger.info("Cache manager: " + cacheManager);
        logger.info("Cache manager names: " + cacheManager.getCacheNames());
    }

    @GetMapping("/idCard/{idCard}")
    public List<Person> findByIdCard(@PathVariable("idCard") String idCard) {
        return repository.findByIdCard(idCard);
    }

    @GetMapping("/{id}")
    public Person findById(@PathVariable("id") Integer id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Person not found: " + id));
    }

    @GetMapping("/persons")
    public List<Person> findAll() {
        return (List<Person>) repository.findAll();
    }


    @GetMapping("/new/{firstName}/{lastName}/{idCard}/{age}")
    public Person newPerson(@PathVariable("firstName") String firstName, @PathVariable("lastName") String lastName,
                                @PathVariable("idCard") String idCard, @PathVariable("age") Integer age) {
        Person person = new Person();
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setIdCard(idCard);
        person.setAge(age);

        logger.info(String.format("newPerson(%s)", person));
        return repository.save(person);
    }

    @GetMapping("/update/{id}/{firstName}/{lastName}/{idCard}/{age}")
    public Person updatePerson(@PathVariable("id") Integer id, @PathVariable("firstName") String firstName, @PathVariable("lastName") String lastName,
                            @PathVariable("idCard") String idCard, @PathVariable("age") Integer age) {
        Person person = repository.findById(id).orElseThrow(()-> new IllegalStateException("Person not found: " + id));
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setIdCard(idCard);
        person.setAge(age);

        logger.info(String.format("updatePerson(%s)", person));
        return repository.save(person);
    }
}