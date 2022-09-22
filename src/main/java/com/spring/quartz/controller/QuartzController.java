package com.spring.quartz.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.quartz.model.Event;
import com.spring.quartz.model.JobDescriptor;
import com.spring.quartz.model.feignInterface;
import com.spring.quartz.service.QuartzService;
import lombok.RequiredArgsConstructor;
import org.quartz.JobDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.spring.quartz.model.feignInterface;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/quartz")
@RequiredArgsConstructor
public class QuartzController {
    private final QuartzService quartzService;
    private ObjectMapper objectMapper;
    @Autowired
    feignInterface feignInterface ;

    @GetMapping(path = "/groups/jobs")
    public ResponseEntity<JobDescriptor> findAllJobs() {
        List<JobDescriptor> jobs = quartzService.findAllJobs();
        if (jobs.isEmpty()) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        } else {
            return new ResponseEntity(jobs, HttpStatus.OK);
        }
    }
    @GetMapping(path = "/groups/eventsjobs/{key}")
    public List<Event>getEventsJobs(@PathVariable("key") String key){
        return quartzService.getEventsJobs(key);
    }


    @PostMapping(path = "/groups/jobs")
    public ResponseEntity<JobDescriptor> createJob( @RequestBody JobDescriptor descriptor) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        return new ResponseEntity(quartzService.createJob(descriptor), HttpStatus.CREATED);
    }

    @PostMapping(path = "/groups/jobss")
    public void createJobs( @RequestBody JsonNode json) {


        feignInterface.SendDataToBack(URI.create("http://localhost:8089/api/scheduler/test"),json);
    }

    @GetMapping(path = "/groups/jobs/{name}")
    public ResponseEntity<JobDescriptor> findJob( @PathVariable String name) {
        return quartzService.findJob( name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping(path = "/groups/jobs/{name}")
    public ResponseEntity<JobDetail> updateJob(@PathVariable String group, @PathVariable String name, @RequestBody JobDescriptor descriptor) {
        return quartzService.updateJob( name, descriptor).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping(path = "/groups/jobs/delete/{name}")
    public ResponseEntity<Void> deleteJob( @PathVariable String name) {
        quartzService.deleteJob( name);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(path = "/groups/jobs/pause/{name}")
    public ResponseEntity<Void> pauseJob( @PathVariable String name) {
        quartzService.pauseJob( name);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(path = "/groups/jobs/resume/{name}")
    public ResponseEntity<Void> resumeJob( @PathVariable String name) {
        quartzService.resumeJob( name);
        return ResponseEntity.noContent().build();
    }
    @GetMapping(path = "/groups/jobs/pausetrigger/{name}")
    public ResponseEntity<Void> pauseTrigger( @PathVariable String name){
        quartzService.pauseTrigger( name);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(path = "/groups/jobs/resumetrigger/{name}")
    public ResponseEntity<Void> resumeTrigger( @PathVariable String name) {
        quartzService.resumeTrigger( name);
        return ResponseEntity.noContent().build();
    }
}
