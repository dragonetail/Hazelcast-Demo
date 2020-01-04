package org.hazelcast.poc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;

@Slf4j
@SpringBootApplication
@EnableCaching
public class HazelcastDemoApplication implements CommandLineRunner {
	@Autowired
	private CacheManager cacheManager;

	public static void main(String[] args) {
		SpringApplication.run(HazelcastDemoApplication.class, args);
	}

	@Override
	public void run(String... args)  {
		log.info("Spring Boot Hazelcast Caching Example Configuration");
		log.info("Using cache manager: " + cacheManager.getClass().getName());
	}

}
