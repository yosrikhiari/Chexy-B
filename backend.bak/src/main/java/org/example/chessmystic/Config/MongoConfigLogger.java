package org.example.chessmystic.Config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MongoConfigLogger {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @PostConstruct
    public void logMongoUri() {
        System.out.println("MongoDB URI: " + mongoUri);
    }
}