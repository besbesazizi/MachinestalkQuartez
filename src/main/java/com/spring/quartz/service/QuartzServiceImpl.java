package com.spring.quartz.service;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.spring.quartz.model.Event;
import com.spring.quartz.model.JobDescriptor;
import com.spring.quartz.model.TriggerDescriptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.quartz.JobKey.jobKey;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class QuartzServiceImpl implements QuartzService {

    @Autowired
    private WebSocketService webSocketService;
    private final Scheduler scheduler;

    public JobDescriptor createJob( JobDescriptor descriptor) {
        JobDetail jobDetail = descriptor.buildJobDetail();
        Set<Trigger> triggersForJob = descriptor.buildTriggers();
        log.info("About to save job with key - {}", jobDetail.getKey());
        try {
            scheduler.scheduleJob(jobDetail, triggersForJob, false);
            log.info("Job with key - {} saved successfully", jobDetail.getKey());
            notifyFrontEnd();
        } catch (SchedulerException e) {
            log.error("Could not save job with key - {} due to error - {}", jobDetail.getKey(), e.getLocalizedMessage());
            throw new IllegalArgumentException(e.getLocalizedMessage());
        }

        return descriptor;
    }




    public List<JobDescriptor> findAllJobs() {
        List<JobDescriptor> jobList = new ArrayList<>();
        try {
            for (String groupName : scheduler.getJobGroupNames()) {
                for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                    String name = jobKey.getName();
                    String group = jobKey.getGroup();
                    JobDetail jobDetail = scheduler.getJobDetail(jobKey(name, group));
                    List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobDetail.getKey());
                    jobList.add(JobDescriptor.buildDescriptor(jobDetail, triggers, scheduler));
                }
            }
        } catch (SchedulerException e) {
            log.error("Could not find all jobs due to error - {}", e.getLocalizedMessage());
        }
        return jobList;
    }

    @Override
    public List<Event> getEventsJobs(String key) {
        List<Event> jobList = new ArrayList<>();
        List<JobDescriptor> jobDescriptorList = new ArrayList<>();
        jobDescriptorList= findAllJobs();
        for(JobDescriptor job :jobDescriptorList){
            if(key.equals(job.getData().get("tenantid"))){
                jobList.addAll(convertEvent(job));
            }
        }
        return jobList;
    }
    public TriggerDescriptor convertCronToDate(TriggerDescriptor TriggerDescriptor,Event event){
      String cronExpresion = TriggerDescriptor.getCron();

        int year = cronExpresion.charAt(1);
        int month = cronExpresion.charAt(2);
        int day = cronExpresion.charAt(5);
        int hour =cronExpresion.charAt(1);
        int minute=cronExpresion.charAt(3);
        Date date = new GregorianCalendar(year, month, day).getTime();
        log.info(String.valueOf(date));
        return null;
    }

    public List<Event> convertEvent(JobDescriptor JobDescriptor) {
        List<Event> events = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeformatter = DateTimeFormatter.ofPattern("HH:mm");
        for (int i = 0; JobDescriptor.getTriggerDescriptors().size() > i; i++) {
            LocalDateTime datetime = JobDescriptor.getTriggerDescriptors().get(i).getFireTime();
            Event event = new Event();
            event.setName(JobDescriptor.getName());
            event.setTriggername(JobDescriptor.getTriggerDescriptors().get(i).getName());

            event.setEtat(JobDescriptor.getTriggerDescriptors().get(i).getTriggerState());
            event.setTenantId((String) JobDescriptor.getData().get("tenantid"));
            event.setTypeEvent((String) JobDescriptor.getData().get("type"));
            event.setDatecreated((String) JobDescriptor.getData().get("createdAt"));
            if (datetime != null) {

                String formattedDateTime = datetime.format(formatter);
                String time = datetime.format(timeformatter);
                event.setFireTime(formattedDateTime);
                event.setTime(time);

            } else {

                ZonedDateTime now = ZonedDateTime.now();
                CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ);
                CronParser parser = new CronParser(cronDefinition);
                ExecutionTime executionTime = ExecutionTime.forCron(parser.parse(JobDescriptor.getTriggerDescriptors().get(i).getCron()));
                Duration nextRunDate = executionTime.timeToNextExecution(now).get();
                ZonedDateTime instant = now.plus(nextRunDate);
                // Create Date instance out of Instant
                Date dateToRun = Date.from(instant.toInstant());
                LocalDateTime date = dateToRun.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                String formattedDateTime = date.format(formatter);
                String time = date.format(timeformatter);
                event.setFireTime(formattedDateTime);

                event.setTime(time);
                System.out.println(formattedDateTime);




            }
            events.add(event);
        }
        return events;
    }

    @Transactional(readOnly = true)
    public Optional<JobDescriptor> findJob( String name) {
        try {
            JobDetail jobDetail = scheduler.getJobDetail(jobKey(name));
            if(Objects.nonNull(jobDetail)) {
                List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobDetail.getKey());
                return Optional.of(
                        JobDescriptor.buildDescriptor(jobDetail, triggers, scheduler));
            }
        } catch (SchedulerException e) {
            log.error("Could not find job with key - {}.{} due to error - {}", name, e.getLocalizedMessage());
        }
        log.warn("Could not find job with key - {}.{}",  name);
        return Optional.empty();
    }

    public Optional<JobDetail> updateJob( String name, JobDescriptor descriptor) {
        try {
            JobDetail oldJobDetail = scheduler.getJobDetail(jobKey(name));
            if(Objects.nonNull(oldJobDetail)) {
                JobDataMap jobDataMap = oldJobDetail.getJobDataMap();
                for(Map.Entry<String,Object> entry : descriptor.getData().entrySet()){
                    jobDataMap.put(entry.getKey(), entry.getValue());
                }
                JobBuilder jb = oldJobDetail.getJobBuilder();
                JobDetail newJobDetail = jb.usingJobData(jobDataMap).storeDurably().build();
                scheduler.addJob(newJobDetail, true);

                log.info("Updated job with key - {}", newJobDetail.getKey());
                return Optional.of(newJobDetail);
            }
            log.warn("Could not find job with key - {}.{} to update",  name);
        } catch (SchedulerException e) {
            log.error("Could not find job with key - {}.{} to update due to error - {}",  name, e.getLocalizedMessage());
        }
        return Optional.empty();
    }

    public void deleteJob( String name) {
        try {
            scheduler.deleteJob(jobKey(name));
            log.info("Deleted job with key - {}.{}", name);
            notifyFrontEnd();

        } catch (SchedulerException e) {
            log.error("Could not delete job with key - {}.{} due to error - {}",  name, e.getLocalizedMessage());
        }
    }

    public void pauseJob( String name) {
        try {
            scheduler.pauseJob(jobKey(name));
            log.info("Paused job with key - {}.{}",  name);
            notifyFrontEnd();

        } catch (SchedulerException e) {
            log.error("Could not pause job with key - {}.{} due to error - {}", name, e.getLocalizedMessage());
        }
    }

    public void pauseTrigger( String name) {
        try {
            scheduler.pauseTrigger(new TriggerKey(name));
            log.info("Paused job with key - {}.{}",  name);
            notifyFrontEnd();

        } catch (SchedulerException e) {
            log.error("Could not pause job with key - {}.{} due to error - {}", name, e.getLocalizedMessage());
        }
    }

    public void resumeJob(String name) {
        try {
            scheduler.resumeJob(jobKey(name));
            log.info("Resumed job with key - {}.{}", name);
            notifyFrontEnd();
        } catch (SchedulerException e) {
            log.error("Could not resume job with key - {}.{} due to error - {}",  name, e.getLocalizedMessage());
        }
    }
    public void resumeTrigger(String name){
        try {
            scheduler.resumeTrigger(new TriggerKey(name));
            log.info("Resumed trigger with key - {}.{}", name);
            notifyFrontEnd();

        } catch (SchedulerException e) {
            log.error("Could not resume trigger with key - {}.{} due to error - {}",  name, e.getLocalizedMessage());
        }
    }
    @Override
    public void notifyFrontEnd(){
        final  String entityTopic= "action";
        if(entityTopic==null){
            log.error("failed to get topic");
            return;
        }
        webSocketService.sendMessage(entityTopic);
    }


}
