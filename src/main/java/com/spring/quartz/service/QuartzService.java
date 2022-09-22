package com.spring.quartz.service;

import com.spring.quartz.model.Event;
import com.spring.quartz.model.JobDescriptor;
import org.quartz.JobDetail;

import java.util.List;
import java.util.Optional;

public interface QuartzService {

    JobDescriptor createJob( JobDescriptor descriptor);

    List<JobDescriptor> findAllJobs();
    List<Event>getEventsJobs(String key);

    Optional<JobDescriptor> findJob( String name);

    Optional<JobDetail> updateJob( String name, JobDescriptor descriptor);

    void deleteJob( String name);

    void pauseJob( String name);

    void resumeJob(String name);
    void resumeTrigger(String name);
    void notifyFrontEnd();
     void pauseTrigger( String name);
}
