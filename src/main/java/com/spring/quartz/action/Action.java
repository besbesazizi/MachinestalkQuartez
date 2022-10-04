package com.spring.quartz.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.spring.quartz.model.feignInterface;
import com.spring.quartz.service.QuartzService;
import com.spring.quartz.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.context.ApplicationContext;
import com.spring.quartz.model.feignInterface;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import com.spring.quartz.service.QuartzServiceImpl;

import java.net.URI;

@Slf4j
@Service
public class Action implements Job {
    @Autowired
    private WebSocketService webSocketService;
    public static final String TENANT_ID = "tenantid";
    public static final String URL = "url";
    private ObjectMapper objectMapper;
    @Autowired
     feignInterface feignInterface ;

    public Action() {
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            ApplicationContext applicationContext = (ApplicationContext) context.getScheduler().getContext().get("applicationContext");
            Gson gson = new Gson();
            String json = gson.toJson(context.getJobDetail().getJobDataMap());
            System.out.println(json);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(json);

            objectMapper = applicationContext.getBean(ObjectMapper.class);
            String Url = objectMapper.writeValueAsString(context.getJobDetail().getJobDataMap().get("url")) ;
            StringBuilder sb = new StringBuilder(Url);
            sb.deleteCharAt(sb.length() - 1);
            sb.deleteCharAt(0);
            sb.toString();
            URI determinedBasePathUri = URI.create(sb.toString());
            System.out.println(determinedBasePathUri);
            log.info(objectMapper.writeValueAsString(context.getJobDetail().getJobDataMap().get(TENANT_ID)));
           feignInterface.SendDataToBack( determinedBasePathUri,jsonNode);
            webSocketService.sendMessage("action");

        } catch (Exception e) {

            throw new JobExecutionException(e);
        }

        log.info("Action ** {} ** completed.  Next action scheduled @ {}", context.getJobDetail().getKey().getName(), context.getNextFireTime());
    }
}