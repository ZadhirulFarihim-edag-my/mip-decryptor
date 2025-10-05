package com.rnr.aip

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = ["com.rnr"])
class AipApplication

fun main(args: Array<String>) {
    runApplication<AipApplication>(*args)
}