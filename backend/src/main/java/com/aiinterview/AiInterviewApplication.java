package com.aiinterview;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com.aiinterview.mapper")
@EnableAsync
public class AiInterviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiInterviewApplication.class, args);
    }
}
