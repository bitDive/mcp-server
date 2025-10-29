package io.bitdive.mcpserver.component;

import io.bitdive.mcpserver.config.securety.dto.CurrentUser;
import io.bitdive.mcpserver.dto.hierarchy_method.heap_map.ModuleAnalyticsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static io.bitdive.mcpserver.utils.FinalConvertObject.getListOfMaps;

@Component
@RequiredArgsConstructor
public class MonitoringHeapMapComponent {

    private final AnalyticsService analyticsService;

    public List<ModuleAnalyticsDTO> getAllModuleAnalytics(CurrentUser currentUser) {
        return analyticsService.getAnalyticsData(1,currentUser);
    }

    public List<Map<String, Object>> getCurrentHeapMap(CurrentUser currentUser) {
        return getListOfMaps(getAllModuleAnalytics(currentUser));
    }

    public List<Map<String, Object>> getCurrentHeapMap(String moduleName, CurrentUser currentUser) {
        var allModuleAnalytics = getAllModuleAnalytics(currentUser);
        var listRet = allModuleAnalytics.stream()
                .filter(moduleAnalyticsDTO -> moduleAnalyticsDTO.getModuleName().equalsIgnoreCase(moduleName))
                .toList();

        return getListOfMaps(listRet);
    }

    public List<Map<String, Object>> getCurrentHeapMap(String moduleName, String serviceName, CurrentUser currentUser) {
        var allModuleAnalytics = getAllModuleAnalytics(currentUser);
        var listRet = allModuleAnalytics.stream()
                .filter(moduleAnalyticsDTO -> moduleAnalyticsDTO.getModuleName().equalsIgnoreCase(moduleName))
                .flatMap(moduleAnalyticsDTO -> moduleAnalyticsDTO.getServices().stream())
                .filter(moduleAnalyticsDTO -> moduleAnalyticsDTO.getServiceName().equalsIgnoreCase(serviceName))
                .toList();
        return getListOfMaps(listRet);
    }

    public List<Map<String, Object>> getCurrentHeapMap(String moduleName, String serviceName, String className, CurrentUser currentUser) {
        var allModuleAnalytics = getAllModuleAnalytics(currentUser);
        var listRet = allModuleAnalytics.stream()
                .filter(moduleAnalyticsDTO -> moduleAnalyticsDTO.getModuleName().equalsIgnoreCase(moduleName))
                .flatMap(moduleAnalyticsDTO -> moduleAnalyticsDTO.getServices().stream())
                .filter(moduleAnalyticsDTO -> moduleAnalyticsDTO.getServiceName().equalsIgnoreCase(serviceName))
                .flatMap(moduleAnalyticsDTO -> moduleAnalyticsDTO.getClasses().stream())
                .filter(moduleAnalyticsDTO -> moduleAnalyticsDTO.getClassName().equalsIgnoreCase(className))
                .toList();
        return getListOfMaps(listRet);
    }

}
