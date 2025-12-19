package com.hawkins.xtreamjson.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.hawkins.xtreamjson.data.ApplicationProperties;
import com.hawkins.xtreamjson.service.ApplicationPropertiesService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/admin/properties")
public class ApplicationPropertiesController {
    private final ApplicationPropertiesService service;

    public ApplicationPropertiesController(ApplicationPropertiesService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("propertiesList", service.getAll());
        model.addAttribute("currentProperties", service.getCurrentProperties());
        // Add a new ApplicationProperties object for form binding
        model.addAttribute("properties", new ApplicationProperties());
        return "admin/properties";
    }

    @PostMapping
    public String save(@ModelAttribute ApplicationProperties properties) {
        service.updateProperties(properties);
        return "redirect:/admin/properties";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("currentProperties",
                service.getAll().stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null));
        model.addAttribute("propertiesList", service.getAll());
        return "admin/properties";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        service.deleteProperties(id);
        return "redirect:/admin/properties";
    }
}