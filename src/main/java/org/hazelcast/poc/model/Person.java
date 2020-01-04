package org.hazelcast.poc.model;

import lombok.Data;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.io.Serializable;

@Cache(region="model.Person", usage = CacheConcurrencyStrategy.READ_WRITE)
@Data
@Entity
public class Person implements Serializable {
    private static final long serialVersionUID = 1964134334737487195L;

    @Id
    @GeneratedValue
    private Integer id;
    private String firstName;
    private String lastName;
    private String idCard;
    private int age;

}