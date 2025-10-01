package com.hawkins.xtreamjson.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hawkins.xtreamjson.service.JsonService;

@RestController
@RequestMapping("/api/admin")
public class JsonAdminController {
    private final JsonService jsonService;

    
    public JsonAdminController(JsonService jsonService) {
        this.jsonService = jsonService;
    }

    @PostMapping("/refresh")
    public String refreshJsonData() {
        jsonService.retreiveJsonData();
        return "Triggered retreiveJsonData";
    }
}
