package com.cryptocraft.pricestreamingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class PriceStreamingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PriceStreamingServiceApplication.class, args);
    }

}
