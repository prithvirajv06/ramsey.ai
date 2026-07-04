package com.athen.ramsey;

import com.athen.ramsey.llm.LlmProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing
@EnableConfigurationProperties(LlmProperties.class)
public class RamseyApplication {

	public static void main(String[] args) {
		SpringApplication.run(RamseyApplication.class, args);
	}

}
