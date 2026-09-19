package io.github.decadedx.springaiagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.security.SecureRandom;
import java.util.Base64;

@SpringBootApplication
public class SpringAIagentApplication {

    public static void main(String[] args) {
//        SecureRandom random = new SecureRandom();
//        byte[] key = new byte[32];
//        random.nextBytes(key);
//        String secret = Base64.getEncoder().encodeToString(key);
//        System.out.println(secret);
        SpringApplication.run(SpringAIagentApplication.class, args);
    }

}
