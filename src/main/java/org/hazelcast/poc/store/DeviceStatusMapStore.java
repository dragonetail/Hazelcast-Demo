package com.github.dragonetail.hazelcast.store;

import com.github.dragonetail.hazelcast.model.DeviceStatus;
import com.github.dragonetail.hazelcast.repository.DeviceStatusRepository;
import com.hazelcast.core.MapStoreAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class DeviceStatusMapStore extends MapStoreAdapter<String, DeviceStatus> {
    @Autowired
    private DeviceStatusRepository deviceStatusRepository;

    @Override
    public DeviceStatus load(final String no) {
        Assert.notNull(no, "No should not be null.");

        Optional<DeviceStatus> optionalDeviceStatus = deviceStatusRepository.findByNo(no);
            return optionalDeviceStatus.orElse(null);
    }

    @Override
    public void delete(String no) {
        Assert.notNull(no, "No should not be null.");
        deviceStatusRepository.deleteByNo(no);
    }

    @Override
    public void store(String no, DeviceStatus deviceStatus) {
        Assert.notNull(no, "No should not be null.");
        Assert.notNull(deviceStatus, "deviceStatus should not be null.");

        DeviceStatus deviceStatusForSave = deviceStatusRepository.findByNo(no).orElse(deviceStatus);
        deviceStatusForSave.setValue(deviceStatus.getValue());
        deviceStatusForSave.setLastUpdated(deviceStatus.getLastUpdated());

        deviceStatusRepository.save(deviceStatusForSave);
    }

    @Override
    public void storeAll(final Map<String, DeviceStatus> map) {
        final List<DeviceStatus> dataList = new ArrayList<>();
        map.forEach((no, deviceStatus) -> {
            DeviceStatus deviceStatusForSave = deviceStatusRepository.findByNo(no).orElse(deviceStatus);
            deviceStatusForSave.setValue(deviceStatus.getValue());
            deviceStatusForSave.setLastUpdated(deviceStatus.getLastUpdated());

            dataList.add(deviceStatusForSave);
        });
        this.deviceStatusRepository.saveAll(dataList);
    }

}