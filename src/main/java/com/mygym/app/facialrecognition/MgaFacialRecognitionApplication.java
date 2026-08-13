package com.mygym.app.facialrecognition;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
	    org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration.class, 
	    org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration.class,
	    org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration.class,
	    org.springframework.boot.autoconfigure.data.mongo.MongoReactiveDataAutoConfiguration.class
	})
public class MgaFacialRecognitionApplication {

    public static void main(String[] args) {
        SpringApplication.run(MgaFacialRecognitionApplication.class, args);
    }
}