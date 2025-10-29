package io.bitdive.mcpserver.service;

import io.bitdive.mcpserver.component.MonitoringComponent;
import io.bitdive.mcpserver.config.securety.ApiKeyAuthenticationToken;
import io.bitdive.mcpserver.config.securety.ApiKeyComponent;
import io.bitdive.mcpserver.config.securety.ApiKeyReactiveAuthManager;
import io.bitdive.mcpserver.config.securety.dto.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
//import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TraceTools {

    private final MonitoringComponent monitoringComponent;
    private final ApiKeyComponent apiKeyComponent;

    @Tool(name = "findTraceAll", description = "Returns the full call trace for the specified call ID")
    public String findTraceAll(
            @ToolParam(description = "Call ID") String callId,
            @ToolParam(description = "API key for mcp bitDive access") String apiKey) {
        try {
            var currentUser= verificationApiKey(apiKey);
            return monitoringComponent.findTraceAll(callId,currentUser);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get trace for callId: " + callId, e);
        }
    }

    @Tool(name = "findTraceForMethod", description = "Returns the call trace for a specific method within the given call ID")
    public Map<String, Object> findTraceForMethod(
            @ToolParam(description = "Call ID") String callId,
            @ToolParam(description = "Fully qualified class name") String className,
            @ToolParam(description = "Method name") String methodName,
            @ToolParam(description = "API key for mcp bitDive access") String apiKey) {
        
        try {
            var currentUser= verificationApiKey(apiKey);
            return monitoringComponent.findTraceForMethod(callId, className, methodName,currentUser);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get trace for method " + className + "." + methodName + " in call " + callId, e);
        }
    }

    @Tool(name = "findTraceForMethodBetweenTime",
            description = "Returns the call trace for the specified class and method between the given start and end timestamps")
    public Map<String, Object> findTraceForMethodBetweenTime(
            @ToolParam(description = "Fully qualified class name") String className,
            @ToolParam(description = "Method name") String methodName,
            @ToolParam(description = "Start timestamp in ISO-8601 format including the client's current local timezone offset") OffsetDateTime beginDate,
            @ToolParam(description = "End timestamp in ISO-8601 format including the client's current local timezone offset") OffsetDateTime endDate,
            @ToolParam(description = "API key for mcp bitDive access") String apiKey) {

        try {
            var currentUser= verificationApiKey(apiKey);
            return monitoringComponent.findTraceForMethodBetweenTime(className, methodName, beginDate, endDate,currentUser);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get trace for method " + className + "." + methodName + " between " + beginDate + " and " + endDate, e);
        }
    }

    private CurrentUser verificationApiKey(String apiKey) throws Exception {
        return apiKeyComponent.decryptApiKey(apiKey);
    }

}
