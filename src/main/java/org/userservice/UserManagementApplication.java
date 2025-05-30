package org.userservice;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.retry.annotation.EnableRetry;


@SpringBootApplication
@EnableScheduling
@EnableRetry
public class UserManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserManagementApplication.class, args);
    }
    @Autowired
    private ConfigurableEnvironment environment;

    @PostConstruct
    public void printJwtExpiration() {
        System.out.println("jwt.refresh.expiration = " + environment.getProperty("jwt.refresh.expiration"));
    }
}