package io.bitdive.mcpserver.service;

import io.bitdive.mcpserver.component.MonitoringHeapMapComponent;
import io.bitdive.mcpserver.config.securety.ApiKeyComponent;
import io.bitdive.mcpserver.config.securety.ApiKeyReactiveAuthManager;
import io.bitdive.mcpserver.config.securety.dto.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class HeadMapTools {

    private final MonitoringHeapMapComponent monitoringHeapMapComponent;
    private final ApiKeyComponent apiKeyComponent;

    @Tool(name = "getCurrentHeapMapAllSystem", description = "Returns system performance metrics all system")
    public List<Map<String, Object>> getCurrentHeapMap(@ToolParam(description = "API key for mcp bitDive access") String apiKey) {
        log.info("Getting current heap map for all system");

        try {
            var currentUser = verificationApiKey(apiKey);
            return monitoringHeapMapComponent.getCurrentHeapMap(currentUser);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get current heap map for all system", e);
        }
    }

    @Tool(name = "getCurrentHeapMapForModule", description = "Returns system performance metrics for module")
    public List<Map<String, Object>> getCurrentHeapMapForModule(
            @ToolParam(description = "module name") String moduleName,
            @ToolParam(description = "API key for mcp bitDive access") String apiKey) {
        log.info("Getting current heap map for module: {}", moduleName);
        try {
            var currentUser = verificationApiKey(apiKey);
            return monitoringHeapMapComponent.getCurrentHeapMap(moduleName,currentUser);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to get current heap map for module '%s'", moduleName),e);
        }
    }

    @Tool(name = "getCurrentHeapMapForModuleAndForService", description = "Returns system performance metrics for module and for service")
    public List<Map<String, Object>> getCurrentHeapMapForModuleAndForService(
            @ToolParam(description = "module name") String moduleName,
            @ToolParam(description = "service name") String serviceName,
            @ToolParam(description = "API key for mcp bitDive access") String apiKey) {
        log.info("Getting current heap map for module: {} and service: {}", moduleName, serviceName);
        try {
            var currentUser = verificationApiKey(apiKey);
            return monitoringHeapMapComponent.getCurrentHeapMap(moduleName, serviceName,currentUser);
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Failed to get current heap map for module '%s' and service '%s'", moduleName, serviceName),
                    e
            );
        }

    }

    @Tool(name = "getCurrentHeapMapForModuleAndForServiceClass", description = "Returns system performance metrics for module and for service and class")
    public List<Map<String, Object>> getCurrentHeapMapForModuleAndForServiceClass(
            @ToolParam(description = "module name") String moduleName,
            @ToolParam(description = "service name") String serviceName,
            @ToolParam(description = "class name") String className,
            @ToolParam(description = "API key for mcp bitDive access") String apiKey) {
        log.info("Getting current heap map for module: {}, service: {}, class: {}", moduleName, serviceName, className);
        try {
            var currentUser = verificationApiKey(apiKey);
            return monitoringHeapMapComponent.getCurrentHeapMap(moduleName, serviceName, className,currentUser);
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Failed to get current heap map for module '%s', service '%s', class '%s'",
                            moduleName, serviceName, className),
                    e
            );
        }
    }


    private CurrentUser verificationApiKey(String apiKey) throws Exception {
        return apiKeyComponent.decryptApiKey(apiKey);
    }


}
