package com.hawkins.xtreamjson.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.hawkins.xtreamjson.service.JsonService;

@Controller
public class HomeController {
    private final JsonService jsonService;

    @Autowired
    public HomeController(JsonService jsonService) {
        this.jsonService = jsonService;
    }

    @GetMapping("/")
    public String home(Model model) {
        jsonService.retreiveJsonData();
        model.addAttribute("message", "Welcome to XtreamJson Home Page!");
        return "home";
    }
}