package org.hazelcast.poc.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/hazelcast/client/map")
public class HazelcastMapClientPocController {

    private final Map<String, String> pocClient01Map01;

    @Autowired
    HazelcastMapClientPocController(Map<String, String> pocClient01Map01) {
        this.pocClient01Map01 = pocClient01Map01;
    }

    @GetMapping(value = "/put/{key}")
    public String put(@PathVariable String key, @RequestParam String value) {
        pocClient01Map01.put(key, value);
        return "Data is stored: " + key + " -> " + value;
    }

    @GetMapping(value = "/get/{key}")
    public String get(@PathVariable String key) {
        return pocClient01Map01.get(key);
    }

    @GetMapping(value = "/getMap")
    public Map<String, String> getMap() {
        return pocClient01Map01;
    }
}