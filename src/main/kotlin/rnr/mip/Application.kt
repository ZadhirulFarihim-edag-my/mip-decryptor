package rnr.mip

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(exclude = [UserDetailsServiceAutoConfiguration::class])
@ComponentScan(basePackages = ["com.rnr"])
internal class SpringBootApp

fun main(args: Array<String>) {
    runApplication<SpringBootApp>(*args)
}