package com.cryptocraft.inventoryservice.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan(basePackages = "com.cryptocraft.inventoryservice")
@EnableJpaRepositories(basePackages = "com.cryptocraft.inventoryservice")
public class AppConfig {

}