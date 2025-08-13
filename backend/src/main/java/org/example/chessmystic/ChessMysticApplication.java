package org.example.chessmystic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@EnableScheduling
@EnableMongoAuditing
@SpringBootApplication
public class ChessMysticApplication {

    public static void main(String[] args) {
        try {
            // Try to load .env file - make it optional to avoid crashes
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing() // This prevents crash if .env is missing
                    .load();

            // Set system properties from .env
            dotenv.entries().forEach(entry ->
                    System.setProperty(entry.getKey(), entry.getValue())
            );
        } catch (Exception e) {
            System.out.println("Warning: Could not load .env file: " + e.getMessage());
            System.out.println("Application will continue with default/system environment variables");
        }

        SpringApplication.run(ChessMysticApplication.class, args);
    }
}