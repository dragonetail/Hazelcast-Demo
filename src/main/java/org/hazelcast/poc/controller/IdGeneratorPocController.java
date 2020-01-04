package org.hazelcast.poc.controller;

import lombok.extern.slf4j.Slf4j;
import org.hazelcast.poc.service.IdGeneratorPocService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/id/generator")
public class IdGeneratorPocController {
    private final IdGeneratorPocService idGeneratorPocService;

    @Autowired
    IdGeneratorPocController( IdGeneratorPocService idGeneratorPocService) {
        this.idGeneratorPocService = idGeneratorPocService;
    }

    @GetMapping(value = "/new")
    public Long newId( ) {
        return idGeneratorPocService.generate();
    }

    @GetMapping(value = "/newCode")
    public String newCode( ) {
        return idGeneratorPocService.generateCode();
    }

    @GetMapping(value = "/newCode/{prefix}")
    public String newCodeWithPrefix(@PathVariable("prefix") String prefix ) {
        return idGeneratorPocService.generateCode(prefix);
    }

    @GetMapping(value = "/nonceToken")
    public String nonceToken( ) {
        return idGeneratorPocService.generateNonceToken();
    }
}