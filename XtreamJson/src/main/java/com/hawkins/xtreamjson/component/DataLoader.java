package com.hawkins.xtreamjson.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.hawkins.xtreamjson.service.JsonService;

@Component
public class DataLoader implements CommandLineRunner {
    
	@Autowired
    private JsonService jsonService;

    @Override
    public void run(String... args) throws Exception {
        jsonService.retreiveJsonData();    }
}
