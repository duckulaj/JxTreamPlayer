package com.hawkins.xtreamjson.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Controller
public class LogController {
    @GetMapping("/admin/viewLog")
    public String viewLog(Model model) {
        String logContent;
        long lastModified = 0L;
        try {
            // Adjust the path if your log file is elsewhere
            Path logPath = Path.of("XstreamJson.log");
            logContent = Files.readString(logPath, StandardCharsets.UTF_8);
            try {
                lastModified = Files.getLastModifiedTime(logPath).toMillis();
            } catch (Exception e) {
                // ignore and leave lastModified as 0
            }
        } catch (IOException e) {
            logContent = "Could not read log file: " + e.getMessage();
        }
        model.addAttribute("logContent", logContent);
        model.addAttribute("lastModified", lastModified);
        return "fragments/viewLog :: logView";
    }

    @GetMapping("/admin/logContent")
    @ResponseBody
    public ResponseEntity<String> logContent(HttpServletRequest request) {
        try {
            Path logPath = Path.of("XstreamJson.log");
            if (!Files.exists(logPath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Log file not found");
            }
            long lastModified = Files.getLastModifiedTime(logPath).toMillis();

            // Check If-Modified-Since header
            String ifModifiedSince = request.getHeader(HttpHeaders.IF_MODIFIED_SINCE);
            if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
                try {
                    long ifModifiedSinceMillis = request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE);
                    if (ifModifiedSinceMillis >= lastModified) {
                        return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
                    }
                } catch (IllegalArgumentException ignored) {
                    // malformed header - fall through and return content
                }
            }

            String log = Files.readString(logPath, StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .lastModified(lastModified)
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .body(log);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not read log file: " + e.getMessage());
        }
    }

    @GetMapping(path = "/admin/logStream", produces = "text/event-stream")
    @ResponseBody
    public SseEmitter streamLog() {
        final SseEmitter emitter = new SseEmitter(0L); // no timeout
        final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "log-sse-watcher");
            t.setDaemon(true);
            return t;
        });

        exec.scheduleAtFixedRate(new Runnable() {
            private long lastSent = 0L;

            @Override
            public void run() {
                try {
                    Path logPath = Path.of("XstreamJson.log");
                    if (!Files.exists(logPath)) {
                        try {
                            emitter.send(SseEmitter.event().name("notfound").data("Log file not found"));
                        } catch (IOException ex) {
                            // client likely disconnected
                            cleanup();
                        }
                        return;
                    }
                    long lm = Files.getLastModifiedTime(logPath).toMillis();
                    if (lm > lastSent || lastSent == 0L) {
                        String content = Files.readString(logPath, StandardCharsets.UTF_8);
                        try {
                            emitter.send(SseEmitter.event().name("log").data(content));
                            lastSent = lm;
                        } catch (IOException ex) {
                            // client disconnected or send failure
                            cleanup();
                        }
                    }
                } catch (IOException e) {
                    try {
                        emitter.send(SseEmitter.event().name("error").data("Could not read log: " + e.getMessage()));
                    } catch (IOException ex) {
                        cleanup();
                    }
                }
            }

            private void cleanup() {
                try {
                    emitter.complete();
                } catch (Exception ignored) {}
                exec.shutdown();
            }
        }, 0, 2, TimeUnit.SECONDS);

        emitter.onCompletion(() -> exec.shutdown());
        emitter.onTimeout(() -> exec.shutdown());
        emitter.onError((ex) -> exec.shutdown());

        return emitter;
    }
}