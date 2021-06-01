package org.example;


import org.example.spring.ComponentScan;
import org.example.spring.Configuration;

@Configuration
@ComponentScan(basePackages = "org.example.business")
public class RootConfig {
}
