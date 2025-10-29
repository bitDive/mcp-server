package io.bitdive.mcpserver;

import org.springframework.ai.mcp.server.autoconfigure.McpServerAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import reactor.core.publisher.Hooks;

@SpringBootApplication()
@EnableScheduling
public class McpServerApplication {
	public static void main(String[] args) {
		SpringApplication.run(McpServerApplication.class, args);
	}
}
