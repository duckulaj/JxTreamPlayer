package com.hawkins.xtreamjson.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

@Controller
public class LogController {
    @GetMapping("/admin/viewLog")
    public String viewLog(Model model) {
        String logContent;
        try {
            // Adjust the path if your log file is elsewhere
            Path logPath = Path.of("XstreamJson.log");
            logContent = Files.readString(logPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logContent = "Could not read log file: " + e.getMessage();
        }
        model.addAttribute("logContent", logContent);
        return "fragments/viewLog :: logView";
    }
}
