package io.github.decadedx.springaiagent;

import org.springframework.boot.SpringApplication;

public class TestSpringAIagentApplication {

    public static void main(String[] args) {
        SpringApplication.from(SpringAIagentApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
