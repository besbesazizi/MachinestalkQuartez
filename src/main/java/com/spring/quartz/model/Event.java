package com.spring.quartz.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import org.quartz.Trigger.TriggerState;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Event {
    private Long Id;
    private String name;
    private String tenantId;
    private Long Customerid;
    private String typeEvent;
    private Target target;
    private String fireTime;
    private String cron;
    private Boolean Repeat;
    private  String repeats;
    private LocalDateTime EndEvent;
    private String time ;
    private TriggerState etat;
    private String datecreated;
    private String description;
    private  String triggername;
}
