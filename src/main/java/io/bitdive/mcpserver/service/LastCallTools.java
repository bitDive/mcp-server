package io.bitdive.mcpserver.service;

import io.bitdive.mcpserver.component.LastCallComponent;
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

import static io.bitdive.mcpserver.utils.FinalConvertObject.getListOfMaps;

@Service
@RequiredArgsConstructor
@Slf4j
public class LastCallTools {

    private final LastCallComponent lastCallComponent;
    private final ApiKeyComponent apiKeyComponent;

    @Tool(name = "getLastCallService", description = "returns a list of recent runs with id trace")
    public List<Map<String, Object>> lastCallService(
            @ToolParam(description = "module name") String moduleName,
            @ToolParam(description = "service name") String serviceName,
            @ToolParam(description = "API key for mcp bitDive access") String apiKey) {
        log.info("Getting last call service for module: {} and service: {}", moduleName, serviceName);

        try {
            var currentUser= verificationApiKey(apiKey);
            return getListOfMaps(lastCallComponent.lastCallService(moduleName, serviceName,currentUser));
        }catch (Exception e) {
            throw new RuntimeException("Failed to get returns a list of recent runs with id trace" + moduleName + "." + serviceName , e);
        }

    }

    private CurrentUser verificationApiKey(String apiKey) throws Exception {
        return apiKeyComponent.decryptApiKey(apiKey);
    }

}
