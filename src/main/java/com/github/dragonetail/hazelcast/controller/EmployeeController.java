package com.github.dragonetail.hazelcast.controller;


import com.github.dragonetail.hazelcast.model.Employee;
import com.github.dragonetail.hazelcast.model.Person;
import com.github.dragonetail.hazelcast.repository.PersonRepository;
import com.github.dragonetail.hazelcast.service.EmployeeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

@Api(tags = "员工实体操作接口，通过Service操作IMap接口实现缓存访问和控制")
@RestController
@RequestMapping("/employees")
public class EmployeeController {

    private Logger logger = Logger.getLogger(EmployeeController.class.getName());

    @Autowired
    private EmployeeService service;
    @Autowired
    private PersonRepository repository;

    @ApiOperation("根据关联人员Id查找员工，这个没有使用索引")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "人员ID", required = true),
    })
    @GetMapping("/person/{id}")
    public Employee findByPersonId(@PathVariable("id") Integer personId) {
        logger.info(String.format("findByPersonId(%d)", personId));
        return service.findByPersonId(personId);
    }

    @ApiOperation("通过公司查找员工，使用公司字段索引")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "company", value = "公司", required = true),
    })
    @GetMapping("/company/{company}")
    public List<Employee> findByCompany(@PathVariable("company") String company) {
        logger.info(String.format("findByCompany(%s)", company));
        return service.findByCompany(company);
    }

    @ApiOperation("根据员工Id查找员工")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "人员ID", required = true),
    })
    @GetMapping("/{id}")
    public Employee findById(@PathVariable("id") Integer id) {
        logger.info(String.format("findById(%d)", id));
        return service.findById(id);
    }

    @ApiOperation("新增员工")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "employee", value = "员工实体", required = true, dataType = "Employee"),
    })
    @PostMapping("/employees")
    public Employee add(@RequestBody Employee employee) {
        logger.info(String.format("add(%s)", employee));
        return service.save(employee);
    }

    @ApiOperation("新增员工")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "personId", value = "人员Id", required = true),
            @ApiImplicitParam(name = "company", value = "公司", required = true),
    })
    @GetMapping("/new/{personId}/{company}")
    public Employee newEmployee(@PathVariable("personId") Integer personId, @PathVariable("company") String company) {
        Employee employee = new Employee();
        employee.setPersonId(personId);
        employee.setCompany(company);

        logger.info(String.format("newEmployee(%s)", employee));
        return service.save(employee);
    }

    @ApiOperation("更新员工")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "员工Id", required = true),
            @ApiImplicitParam(name = "personId", value = "员工实体", required = true),
            @ApiImplicitParam(name = "company", value = "公司", required = true),
    })
    @GetMapping("/update/{id}/{personId}/{company}")
    public Employee updateEmployee(@PathVariable("id") Integer id, @PathVariable("personId") Integer personId, @PathVariable("company") String company) {
        Employee employee  = service.findById(id);
        employee.setPersonId(personId);
        employee.setCompany(company);

        logger.info(String.format("updateEmployee(%s)", employee));
        return service.save(employee);
    }

    @ApiOperation("批量构造测试数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "counts", value = "数量", required = true),
    })
    @GetMapping("/prepareData/{counts}")
    public String prepareData(@PathVariable("counts") Integer counts) {
        Random random = new Random();
        for (int i = 0; i < counts; i++) {
            long surfix = random.nextLong();

            Person person = new Person();
            person.setFirstName("FN-" + surfix);
            person.setLastName("LN-" + surfix);
            person.setIdCard("IDCARD-" + surfix);
            person.setAge(random.nextInt(90) + 10);

            logger.info(String.format("updatePerson(%s)", person));
            person = repository.save(person);

            Employee employee = new Employee();
            employee.setPersonId(person.getId());
            employee.setCompany("Co." + surfix);

            logger.info(String.format("newEmployee(%s)", employee));
            service.save(employee);
        }
        return "OK";
    }
}