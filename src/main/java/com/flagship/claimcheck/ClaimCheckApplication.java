package com.flagship.claimcheck;

import com.flagship.claimcheck.shadow.ShadowProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ShadowProperties.class)
public class ClaimCheckApplication {
    public static void main(String[] args) { SpringApplication.run(ClaimCheckApplication.class, args); }
}
