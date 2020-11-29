package com.att.training.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    CommandLineRunner printInfo() {
        return args -> {
            var runtime = Runtime.getRuntime();
            var cpus = runtime.availableProcessors();
            long totalMemory = toMB(runtime.totalMemory());
            long maxMemory = toMB(runtime.maxMemory());

            System.out.println("This is where we're at:");
            System.out.println("         Java: " + System.getProperty("java.version"));
            System.out.println(" JVM 'vendor': " + System.getProperty("java.vendor"));
            System.out.println("   # of cores: " + cpus);
            System.out.println("JVM heap (MB): " + totalMemory + "/" + maxMemory);
        };
    }

    private long toMB(long valueInBytes) {
        return valueInBytes / 1024 / 1024;
    }
}
