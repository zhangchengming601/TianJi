package com.tianji.xxljob;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class XxlJobApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplicationBuilder(XxlJobApplication.class).build(args);
        Environment env = app.run(args).getEnvironment();
    }
}
