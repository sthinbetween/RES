package de.dhge.resapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ResApiApplication {
	
	public static String version = "0.0.1";

	public static void main(String[] args) {
		SpringApplication.run(ResApiApplication.class, args);
	}

}
