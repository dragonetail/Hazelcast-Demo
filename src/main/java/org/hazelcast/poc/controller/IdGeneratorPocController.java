package org.hazelcast.poc.controller;

import com.hazelcast.flakeidgen.FlakeIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/id/generator")
public class IdGeneratorController {
    private final FlakeIdGenerator pocFlakeIdGenerator01;

    @Autowired
    IdGeneratorController(FlakeIdGenerator pocFlakeIdGenerator01) {
        this.pocFlakeIdGenerator01 = pocFlakeIdGenerator01;
    }

    @GetMapping(value = "/new")
    public Long newId( ) {
        return pocFlakeIdGenerator01.newId();
    }
}