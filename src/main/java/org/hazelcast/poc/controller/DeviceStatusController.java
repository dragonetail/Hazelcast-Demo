package org.hazelcast.poc.controller;

import org.hazelcast.poc.model.DeviceStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/device/status")
public class DeviceStatusController {

    private final Map<String, DeviceStatus> deviceStatusCacheMap;

    @Autowired
    DeviceStatusController(Map<String, DeviceStatus> deviceStatusCacheMap) {
        this.deviceStatusCacheMap = deviceStatusCacheMap;
    }

    @GetMapping(value = "/put/{no}/{value}")
    public String put(@PathVariable String no, @PathVariable Integer value ) {
        DeviceStatus deviceStatus = new DeviceStatus();
        deviceStatus.setNo(no);
        deviceStatus.setValue(value);
        deviceStatus.setLastUpdated(LocalDateTime.now());

        deviceStatusCacheMap.put(no, deviceStatus);
        return "DeviceStatus is stored: " + no + " -> " + value;
    }

    @GetMapping(value = "/get/{no}")
    public String get(@PathVariable String no) {
        DeviceStatus deviceStatus = deviceStatusCacheMap.get(no);
        if(deviceStatus == null){
            return "DeviceStatus not found for: " + no;
        }

        return deviceStatus.toString();
    }
}