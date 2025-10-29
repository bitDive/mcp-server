package io.bitdive.mcpserver.config;

import io.bitdive.mcpserver.service.HeadMapTools;
import io.bitdive.mcpserver.service.LastCallTools;
import io.bitdive.mcpserver.service.TraceTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolCallbackConfig {
    @Bean
    public ToolCallbackProvider monitoringTools(TraceTools traceTools, HeadMapTools headMapTools, LastCallTools lastCallTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(traceTools, headMapTools, lastCallTools)
                .build();
    }
}
