package com.github.senocak.csv;

import com.github.senocak.csv.core.annotation.EnableCsvRepositories;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableCsvRepositories(basePackages = "com.github.senocak.csv")
public class CsvMainApplication {
    public static void main(String[] args) {
        SpringApplication.run(CsvMainApplication.class, args);
    }
}
