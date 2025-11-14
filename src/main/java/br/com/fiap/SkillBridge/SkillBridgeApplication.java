package br.com.fiap.SkillBridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SkillBridgeApplication {

	public static void main(String[] args) {
		SpringApplication.run(SkillBridgeApplication.class, args);
	}

}
