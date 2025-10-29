package io.bitdive.mcpserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.transport.WebFluxSseServerTransportProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Configuration
@EnableScheduling                 // важно!
public class McpKeepAliveConfig {

    @Bean
    public Disposable heartbeatLoop(WebFluxSseServerTransportProvider provider) {
        return Flux.interval(Duration.ofSeconds(40))
                .flatMap(t -> provider.sendHeartbeat())
                .subscribe();
    }
}
