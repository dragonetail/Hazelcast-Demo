package org.hazelcast.poc.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/hazelcast/server/map")
public class HazelcastMapServerPocController {

    private final Map<String, String> pocServer01Map01;

    @Autowired
    HazelcastMapServerPocController(Map<String, String> pocServer01Map01) {
        this.pocServer01Map01 = pocServer01Map01;
    }

    @GetMapping(value = "/put/{key}")
    public String put(@PathVariable String key, @RequestParam String value) {
        pocServer01Map01.put(key, value);
        return "Data is stored: " + key + " -> " + value;
    }

    @GetMapping(value = "/get/{key}")
    public String get(@PathVariable String key) {
        return pocServer01Map01.get(key);
    }

    @GetMapping(value = "/getMap")
    public Map<String, String> getMap() {
        return pocServer01Map01;
    }
}