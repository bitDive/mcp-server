package io.bitdive.mcpserver.component;

import io.bitdive.mcpserver.config.securety.dto.CurrentUser;
import io.bitdive.mcpserver.db.entity.ClassCallAnalytics;
import io.bitdive.mcpserver.db.entity.InPointCallAnalytics;
import io.bitdive.mcpserver.db.entity.ModuleCallAnalytics;
import io.bitdive.mcpserver.db.entity.ServiceCallAnalytics;
import io.bitdive.mcpserver.db.repository.ClassCallAnalyticsRepository;
import io.bitdive.mcpserver.db.repository.InPointCallAnalyticsRepository;
import io.bitdive.mcpserver.db.repository.ModuleCallAnalyticsRepository;
import io.bitdive.mcpserver.db.repository.ServiceCallAnalyticsRepository;
import io.bitdive.mcpserver.dto.hierarchy_method.heap_map.ClassAnalyticsDTO;
import io.bitdive.mcpserver.dto.hierarchy_method.heap_map.InPointAnalyticsDTO;
import io.bitdive.mcpserver.dto.hierarchy_method.heap_map.ModuleAnalyticsDTO;
import io.bitdive.mcpserver.dto.hierarchy_method.heap_map.ServiceAnalyticsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.bitdive.mcpserver.utils.UserViewModule.isViewForModule;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ModuleCallAnalyticsRepository moduleRepository;
    private final ServiceCallAnalyticsRepository serviceRepository;
    private final ClassCallAnalyticsRepository classRepository;
    private final InPointCallAnalyticsRepository inPointRepository;


    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public List<ModuleAnalyticsDTO> getAnalyticsData(Integer lastMinutes, CurrentUser currentUser) {
        OffsetDateTime end = OffsetDateTime.now();
        OffsetDateTime start = end.minusMinutes(lastMinutes);

        List<ModuleCallAnalytics> modules = moduleRepository.findByAnalysisTimestampBetween(start, end)
                .stream().filter(moduleCallAnalytics ->isViewForModule(currentUser, moduleCallAnalytics.getModuleName()))
                .toList();

        List<ServiceCallAnalytics> services = serviceRepository.findByAnalysisTimestampBetween(start, end);
        List<ClassCallAnalytics> classes = classRepository.findByAnalysisTimestampBetween(start, end);
        List<InPointCallAnalytics> inPoints = inPointRepository.findByAnalysisTimestampBetween(start, end);

        // Group services by moduleName and serviceName
        Map<String, List<ServiceCallAnalytics>> servicesGrouped = services.stream()
                .collect(Collectors.groupingBy(svc -> svc.getModuleName() + ":" + svc.getServiceName()));

        // Group classes by moduleName, serviceName, and className
        Map<String, List<ClassCallAnalytics>> classesGrouped = classes.stream()
                .collect(Collectors.groupingBy(cls -> cls.getModuleName() + ":" + cls.getServiceName() + ":" + cls.getClassName()));

        // Group inPoints by moduleName, serviceName, className, and inPointName
        Map<String, List<InPointCallAnalytics>> inPointsGrouped = inPoints.stream()
                .collect(Collectors.groupingBy(ip -> ip.getModuleName() + ":" + ip.getServiceName() + ":" + ip.getClassName() + ":" + ip.getMethodName()));

        // Build the modules DTO
        return modules.stream()
                .collect(Collectors.groupingBy(ModuleCallAnalytics::getModuleName))
                .entrySet().stream()
                .map(entry -> {
                    String moduleName = entry.getKey();
                    List<ModuleCallAnalytics> moduleRecords = entry.getValue();

                    ModuleCallAnalytics actualModule = moduleRecords.get(moduleRecords.size() - 1);
                    List<ModuleAnalyticsDTO> moduleHistory = moduleRecords.subList(0, moduleRecords.size()).stream()
                            .map(moduleCallAnalytics ->  mapToModuleDTO(moduleCallAnalytics,null,null))
                            .collect(Collectors.toList());

                    // Process services for this module
                    List<ServiceAnalyticsDTO> moduleServices = servicesGrouped.entrySet().stream()
                            .filter(svcEntry -> svcEntry.getKey().startsWith(moduleName + ":"))
                            .map(svcEntry -> {
                                List<ServiceCallAnalytics> serviceRecords = svcEntry.getValue();
                                ServiceCallAnalytics actualService = serviceRecords.get(serviceRecords.size() - 1);
                                List<ServiceAnalyticsDTO> serviceHistory = serviceRecords.subList(0, serviceRecords.size()).stream()
                                        .map(serviceCallAnalytics ->  mapToServiceDTO(serviceCallAnalytics,null,null))
                                        .collect(Collectors.toList());

                                // Process classes for this service
                                String serviceName = actualService.getServiceName();
                                List<ClassAnalyticsDTO> serviceClasses = classesGrouped.entrySet().stream()
                                        .filter(clsEntry -> clsEntry.getKey().startsWith(moduleName + ":" + serviceName + ":"))
                                        .map(clsEntry -> {
                                            List<ClassCallAnalytics> classRecords = clsEntry.getValue();
                                            ClassCallAnalytics actualClass = classRecords.get(classRecords.size() - 1);
                                            List<ClassAnalyticsDTO> classHistory = classRecords.subList(0, classRecords.size()).stream()
                                                    .map(classCallAnalytics -> mapToClassDTO(classCallAnalytics,null,null))
                                                    .collect(Collectors.toList());

                                            // Process inPoints for this class
                                            String className = actualClass.getClassName();
                                            List<InPointAnalyticsDTO> classInPoints = inPointsGrouped.entrySet().stream()
                                                    .filter(ipEntry -> ipEntry.getKey().startsWith(moduleName + ":" + serviceName + ":" + className + ":"))
                                                    .map(ipEntry -> {
                                                        List<InPointCallAnalytics> inPointRecords = ipEntry.getValue();
                                                        InPointCallAnalytics actualInPoint = inPointRecords.get(inPointRecords.size() - 1);
                                                        List<InPointAnalyticsDTO> inPointHistory = inPointRecords.subList(0, inPointRecords.size()).stream()
                                                                .map(inPointCallAnalytics ->  mapToInPointDTO(inPointCallAnalytics,null,null))
                                                                .collect(Collectors.toList());

                                                        InPointAnalyticsDTO inPointDTO = this.mapToInPointDTO(actualInPoint,
                                                                actualModule.getAnalysisTimestamp().minusSeconds(5),
                                                                actualModule.getAnalysisTimestamp().plusSeconds(5)
                                                        );
                                                        return inPointDTO;
                                                    })
                                                    .collect(Collectors.toList());

                                            ClassAnalyticsDTO classDTO = this.mapToClassDTO(actualClass,
                                                    actualModule.getAnalysisTimestamp().minusSeconds(5),
                                                    actualModule.getAnalysisTimestamp().plusSeconds(5)
                                            );
                                            classDTO.setInPoints(classInPoints);
                                            return classDTO;
                                        })
                                        .collect(Collectors.toList());

                                ServiceAnalyticsDTO serviceDTO = this.mapToServiceDTO(actualService,
                                        actualModule.getAnalysisTimestamp().minusSeconds(5),
                                        actualModule.getAnalysisTimestamp().plusSeconds(5)
                                );
                                serviceDTO.setClasses(serviceClasses);
                                return serviceDTO;
                            })
                            .collect(Collectors.toList());

                    ModuleAnalyticsDTO moduleDTO = this.mapToModuleDTO(actualModule,
                            actualModule.getAnalysisTimestamp().minusSeconds(5),
                            actualModule.getAnalysisTimestamp().plusSeconds(5)
                    );
                    moduleDTO.setServices(moduleServices);
                    return moduleDTO;
                })
                .collect(Collectors.toList());
    }

    private ModuleAnalyticsDTO mapToModuleDTO(ModuleCallAnalytics module, OffsetDateTime start, OffsetDateTime end) {
        ModuleAnalyticsDTO dto = new ModuleAnalyticsDTO(
                module.getModuleName(),
                module.getAnalysisTimestamp(),
                module.getErrorCount(),
                module.getCallCountWeb(),
                module.getAvgCallTimeWeb(),
                module.getCallCountScheduler(),
                module.getAvgCallTimeScheduler(),
                module.getSqlCallCount(),
                module.getAvgSqlCallTime(),
                module.getSqlErrorCount(),
                module.getRestCallCount(),
                module.getAvgRestCallTime(),
                module.getRestErrorCount(),
                module.getCount2xx(),
                module.getCount3xx(),
                module.getCount4xx(),
                module.getCount5xx(),
                module.getQueueSendCount(),
                module.getAvgQueueSendTime(),
                module.getQueueSendErrorCount(),
                module.getQueueConsumerCount(),
                module.getQueueConsumerAvgCallTime(),
                module.getQueueConsumerErrorCount(),
                null // services will be set later
        );
        return dto;
    }

    private ServiceAnalyticsDTO mapToServiceDTO(ServiceCallAnalytics service,OffsetDateTime start, OffsetDateTime end) {
        ServiceAnalyticsDTO dto = new ServiceAnalyticsDTO(
                service.getServiceName(),
                service.getAnalysisTimestamp(),
                service.getErrorCount(),
                service.getCallCountWeb(),
                service.getAvgCallTimeWeb(),
                service.getCallCountScheduler(),
                service.getAvgCallTimeScheduler(),
                service.getSqlCallCount(),
                service.getAvgSqlCallTime(),
                service.getSqlErrorCount(),
                service.getRestCallCount(),
                service.getAvgRestCallTime(),
                service.getRestErrorCount(),
                service.getCount2xx(),
                service.getCount3xx(),
                service.getCount4xx(),
                service.getCount5xx(),
                service.getQueueSendCount(),
                service.getAvgQueueSendTime(),
                service.getQueueSendErrorCount(),
                service.getQueueConsumerCount(),
                service.getQueueConsumerAvgCallTime(),
                service.getQueueConsumerErrorCount(),
                null // classes will be set later
        );
        return dto;
    }

    private ClassAnalyticsDTO mapToClassDTO(ClassCallAnalytics cls,OffsetDateTime start, OffsetDateTime end) {
        ClassAnalyticsDTO dto = new ClassAnalyticsDTO(
                cls.getClassName(),
                cls.getAnalysisTimestamp(),
                cls.getErrorCount(),
                cls.getCallCountWeb(),
                cls.getAvgCallTimeWeb(),
                cls.getCallCountScheduler(),
                cls.getAvgCallTimeScheduler(),
                cls.getSqlCallCount(),
                cls.getAvgSqlCallTime(),
                cls.getSqlErrorCount(),
                cls.getRestCallCount(),
                cls.getAvgRestCallTime(),
                cls.getRestErrorCount(),
                cls.getCount2xx(),
                cls.getCount3xx(),
                cls.getCount4xx(),
                cls.getCount5xx(),
                cls.getQueueSendCount(),
                cls.getAvgQueueSendTime(),
                cls.getQueueSendErrorCount(),
                cls.getQueueConsumerCount(),
                cls.getQueueConsumerAvgCallTime(),
                cls.getQueueConsumerErrorCount(),
                null // inPoints will be set later
        );
        return dto;
    }

    private InPointAnalyticsDTO mapToInPointDTO(InPointCallAnalytics inPoint,OffsetDateTime start, OffsetDateTime end) {
        InPointAnalyticsDTO dto = new InPointAnalyticsDTO(
                inPoint.getMethodName(),
                inPoint.getAnalysisTimestamp(),
                inPoint.getErrorCount(),
                inPoint.getCallCountWeb(),
                inPoint.getAvgCallTimeWeb(),
                inPoint.getCallCountScheduler(),
                inPoint.getAvgCallTimeScheduler(),
                inPoint.getSqlCallCount(),
                inPoint.getAvgSqlCallTime(),
                inPoint.getSqlErrorCount(),
                inPoint.getRestCallCount(),
                inPoint.getAvgRestCallTime(),
                inPoint.getRestErrorCount(),
                inPoint.getCount2xx(),
                inPoint.getCount3xx(),
                inPoint.getCount4xx(),
                inPoint.getCount5xx(),
                inPoint.getQueueSendCount(),
                inPoint.getAvgQueueSendTime(),
                inPoint.getQueueSendErrorCount(),
                inPoint.getQueueConsumerCount(),
                inPoint.getQueueConsumerAvgCallTime(),
                inPoint.getQueueConsumerErrorCount()
        );
        return dto;
    }
}
