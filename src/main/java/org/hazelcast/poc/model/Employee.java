package org.hazelcast.poc.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.io.Serializable;

@Entity
@Data
public class Employee implements Serializable {
    private static final long serialVersionUID = 6454847536392358665L;

    @Id
    @GeneratedValue
    private Integer id;
    private Integer personId;
    private String company;
}