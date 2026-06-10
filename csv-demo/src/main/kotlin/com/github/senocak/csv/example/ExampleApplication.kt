package com.github.senocak.csv.example

import com.github.senocak.csv.core.annotation.EnableCsvRepositories
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

fun main(args: Array<String>) {
    runApplication<ExampleApplication>(*args)
}

@SpringBootApplication
@EnableCsvRepositories(basePackages = ["com.github.senocak"])
//@EnableCsvRepositories(basePackageClasses = [ProductRepository::class])
class ExampleApplication
