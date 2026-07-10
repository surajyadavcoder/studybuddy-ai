package com.studybuddy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StudybuddyApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudybuddyApplication.class, args);
    }
}
