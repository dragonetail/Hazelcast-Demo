package org.hazelcast.poc.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/hazelcast/spring")
public class SpringCachePocController {

    private final Map<String, String> springCachePoc01;

    @Autowired
    SpringCachePocController(Map<String, String> springCachePoc01) {
        this.springCachePoc01 = springCachePoc01;
    }

    @Cacheable(value = "pocServer01.springCachePoc01", key = "#key.toString().concat('_time')")
    @GetMapping(value = "getTime/{key}")
    public String getTime(@PathVariable String key) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY/MM/DD HH:mm:SS.SSS");
        return dateFormat.format(new Date());
    }

    @CacheEvict(value = "pocServer01.springCachePoc01", key = "#key.toString().concat('_time')")
    @GetMapping(value = "/clear/{key}")
    public String clear(@PathVariable String key) {
        return "Cache for <" + key + "> has been cleared.";
    }

    @GetMapping(value = "/getMap")
    public Map<String, String> getMap() {
        return springCachePoc01;
    }
}