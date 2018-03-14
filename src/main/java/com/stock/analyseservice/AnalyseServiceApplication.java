package com.stock.analyseservice;

import com.stock.analyseservice.algo.nlp.CommentClassifier;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AnalyseServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AnalyseServiceApplication.class, args);
	}
}
