package com.spring.quartz.model;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.net.URI;


@FeignClient(value = "Angular", url = "http://localhost:8080")
public interface feignInterface {

    @RequestMapping(method = RequestMethod.POST ,consumes = "application/json")
    public void SendDataToBack(URI baseUrl, @RequestBody JsonNode json);

}
