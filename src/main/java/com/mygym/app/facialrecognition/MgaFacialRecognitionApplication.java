package com.mygym.app.facialrecognition;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.resilience.annotation.EnableResilientMethods;

@SpringBootApplication
@EnableResilientMethods
public class MgaFacialRecognitionApplication {

    public static void main(String[] args) {
        SpringApplication.run(MgaFacialRecognitionApplication.class, args);
    }
}
