package com.hawkins.xtreamjson;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import com.hawkins.xtreamjson.util.XstreamCredentials;

@SpringBootApplication
public class XtreamJsonApplication {
    public static void main(String[] args) {
        SpringApplication.run(XtreamJsonApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public CommandLineRunner callHomeEndpoint(RestTemplate restTemplate) {
        return args -> {
            try {
                String response = restTemplate.getForObject("http://localhost:8080/", String.class);
                System.out.println("Home endpoint called on startup. Response: " + response);
            } catch (Exception e) {
                System.err.println("Failed to call / endpoint on startup: " + e.getMessage());
            }
        };
    }

    @Bean
    public XstreamCredentials xstreamCredentials(
        @Value("${xtream.api-url}") String apiUrl,
        @Value("${xtream.username}") String username,
        @Value("${xtream.password}") String password
    ) {
        return new XstreamCredentials(apiUrl, username, password);
    }
}