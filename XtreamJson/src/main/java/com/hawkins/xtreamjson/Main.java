package com.hawkins.xtreamjson;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import com.hawkins.xtreamjson.service.JsonService;

@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(Main.class, args);
        JsonService jsonService = context.getBean(JsonService.class);
        jsonService.retreiveJsonData();
    }
}