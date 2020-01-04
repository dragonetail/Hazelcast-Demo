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
public class QueuePocTask implements Serializable {
    private static final long serialVersionUID = 1050906124905703553L;

    @Id
    @GeneratedValue
    private Integer id;

    @Column(unique = true)
    private Long key;

    private Integer type;

    private String value;
    private LocalDateTime submittedTime;
}