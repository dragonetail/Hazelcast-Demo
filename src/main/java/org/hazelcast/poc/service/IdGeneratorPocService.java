package org.hazelcast.poc.service;


import org.hazelcast.poc.model.Employee;
import org.hazelcast.poc.repository.EmployeeRepository;
import com.hazelcast.core.IMap;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EmployeeService {
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private IMap<Integer, Employee> employeeCacheMap;

    @PostConstruct
    public void init() {
        log.info("Employees cache: " + employeeCacheMap.size());
    }

    public Employee findByPersonId(Integer personId) {
        Predicate predicate = Predicates.equal("personId", personId);
        log.info("Employee cache find by personId");
        Collection<Employee> collection = employeeCacheMap.values(predicate);
        log.info("Employee cached: " + collection);
        Optional<Employee> optionalEmployee = collection.stream().findFirst();
        if (optionalEmployee.isPresent())
            return optionalEmployee.get();


        log.info("Employee db find by personId");
        Employee employee = employeeRepository.findByPersonId(personId);
        log.info("Employee: " + employee);
        employeeCacheMap.put(employee.getId(), employee);
        return employee;
    }

    public List<Employee> findByCompany(String company) {
        Predicate predicate = Predicates.equal("company", company);
        log.info("Employees cache find by company");
        Collection<Employee> collection = employeeCacheMap.values(predicate);
        log.info("Employees cache size: " + collection.size());
        if (collection.size() > 0) {
            return collection.stream().collect(Collectors.toList());
        }


        log.info("Employees db find by company");
        List<Employee> employees = employeeRepository.findByCompany(company);
        log.info("Employees size: " + employees.size());
        employees.parallelStream().forEach(it -> employeeCacheMap.putIfAbsent(it.getId(), it));
        return employees;
    }

    public Employee findById(Integer id) {
        Employee employee = employeeCacheMap.get(id);
        if (employee != null)
            return employee;

        employee = employeeRepository.findById(id).orElseThrow(() -> new RuntimeException("Employee not found: " + id));
        employeeCacheMap.put(id, employee);
        return employee;
    }

    public Employee save(Employee employee) {
        employee = employeeRepository.save(employee);
        employeeCacheMap.put(employee.getId(), employee);
        return employee;
    }

}