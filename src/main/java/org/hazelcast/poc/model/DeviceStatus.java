package org.hazelcast.poc.model;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Data
public class DeviceStatus implements Serializable {
    private static final long serialVersionUID = -367919113244086739L;

    @Id
    @GeneratedValue
    private Integer id;

    @Column(unique = true)
    private String no;

    private Integer value;
    private LocalDateTime lastUpdated;
}