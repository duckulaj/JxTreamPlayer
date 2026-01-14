package com.hawkins.xtreamjson;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.config.BootstrapMode;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootApplication
@ComponentScan(basePackages = { "com.hawkins.xtreamjson" })
@EnableAsync
@EnableJpaRepositories(bootstrapMode = BootstrapMode.DEFAULT)
@EnableScheduling
@org.springframework.cache.annotation.EnableCaching
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class XtreamJsonApplication {
    public static void main(String[] args) {
        SpringApplication.run(XtreamJsonApplication.class, args);
    }

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}