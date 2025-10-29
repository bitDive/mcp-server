package io.bitdive.mcpserver.utils;

import io.bitdive.mcpserver.dto.hierarchy_method.HierarchyMethodUIDto;
import io.bitdive.mcpserver.dto.hierarchy_method.QueueCall;
import io.bitdive.mcpserver.dto.hierarchy_method.RestCall;
import io.bitdive.mcpserver.dto.hierarchy_method.SQLCall;
import io.bitdive.mcpserver.dto.hierarchy_method.data.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class UtilsCovert {


    public static List<ModuleUIDto> convertModuleToUI(List<ModuleRepositoryDTO> ModuleList){

        // Группируем данные по модулям, сервисам и классам
        Map<String, Map<String, Map<String, List<InPointUIDto>>>> groupedData = new HashMap<>();

        for (ModuleRepositoryDTO result : ModuleList) {
            String moduleName = result.getModule();
            String serviceName = result.getService();
            String className = result.getClassName();
            String methodName = result.getMethod();
            Long isPointId = result.getInPointId();
            boolean isInPoint = result.getIsInPoint();

            groupedData
                    .computeIfAbsent(moduleName, k -> new HashMap<>())
                    .computeIfAbsent(serviceName, k -> new HashMap<>())
                    .computeIfAbsent(className, k -> new ArrayList<>())
                    .add(new InPointUIDto(isPointId, methodName, isInPoint));
        }

        return groupedData.entrySet().stream()
                .map(moduleEntry -> {
                    String moduleName = moduleEntry.getKey();
                    Set<ServiceUIDto> serviceUISet = moduleEntry.getValue().entrySet().stream()
                            .map(serviceEntry -> {
                                String serviceName = serviceEntry.getKey();
                                Set<ClassNameUIDto> classNameUISet = serviceEntry.getValue().entrySet().stream()
                                        .map(classEntry -> {
                                            String className = classEntry.getKey();
                                            Set<InPointUIDto> inPointUISet = new HashSet<>(classEntry.getValue());
                                            return new ClassNameUIDto(className, inPointUISet);
                                        })
                                        .collect(Collectors.toSet());

                                return new ServiceUIDto(serviceName, classNameUISet);
                            })
                            .collect(Collectors.toSet());

                    return new ModuleUIDto(moduleName, serviceUISet);
                })
                .collect(Collectors.toList());
    }


    public static CallMethodUIDto convertCallMethodToUI(CallMethodRepositoryDTO callMethod) {
        if (callMethod == null) {
            return null;
        }
        var dto = new CallMethodUIDto();
        dto.setId(callMethod.getMessageId());
        dto.setTraceId(callMethod.getTraceId());
        dto.setClassName(callMethod.getClassName());
        dto.setMethodName(callMethod.getMethodName());
        dto.setArgs(callMethod.getArgs()); // Assuming ParamMethodDto is already suitable for DTO
        // dto.setMessageKafkaCallTime(new MessageKafkaCallTimeUIDto(callMethod.getCallTimeDateStart(), callMethod.getCallTimeDateEnd(), callMethod.getCallTimeDelta()));

        // dto.setParentMethod(toDto(callMethod.getParentMethod()));
        dto.setMethodReturn(callMethod.getMethodReturn());
        dto.setErrorCallMessage(callMethod.getErrorCallMessage());
        dto.setLibraryVersion(callMethod.getLibraryVersion());
        dto.setLibraryVersion(callMethod.getLibraryVersion());
        return dto;
    }


    public static HierarchyMethodUIDto buildDependencyTree(List<CallMethodDTO> callMethods) {
        if (callMethods == null || callMethods.isEmpty()) {
            return null;
        }

        Map<UUID, HierarchyMethodUIDto> dtoMap = new HashMap<>();
        for (CallMethodDTO cm : callMethods) {
            HierarchyMethodUIDto dto = HierarchyMethodUIDto.builder()
                    .messageId(cm.getId())
                    .className(cm.getClassName())
                    .methodName(cm.getMethodName())
                    .args(cm.getArgs())
                    .traceId(cm.getTraceId())
                    .spanId(cm.getSpanId())
                    .callTimeDateStart(cm.getCallTimeDateStart())
                    .callTimeDateEnd(cm.getCallTimeDateEnd())
                    .callTimeDelta(cm.getCallTimeDelta())
                    .methodReturn(cm.getMethodReturn())
                    .errorCallMessage(cm.getErrorCallMessage())
                    .moduleName(cm.getModuleName())
                    .serviceName(cm.getServiceName())
                    .inPointFlag(cm.getInPointFlag())
                    .childCalls(new ArrayList<>())
                    .sqlCalls(cm.getCallMethodSqlList().stream()
                            .map(callMethodSql ->
                                    SQLCall.builder()
                                            .messageId(callMethodSql.getMessageId())
                                            .sql(combineSqlAndParams(callMethodSql.getCallMethodSqlText(), callMethodSql.getCallMethodSqlParam()))
                                            .callTimeDateStart(callMethodSql.getCallTimeDateStart())
                                            .callTimeDateEnd(callMethodSql.getCallTimeDateEnd())
                                            .callTimeDelta(callMethodSql.getCallTimeDelta())
                                            .errorCallMessage(callMethodSql.getErrorCallMessage())
                                            .libraryVersion(callMethodSql.getLibraryVersion())
                                            .build()
                            ).toList())
                    .restCalls(cm.getCallMethodRests().stream()
                            .map(callMethodRest -> RestCall.builder()
                                    .messageId(callMethodRest.getMessageId())
                                    .uri(callMethodRest.getUri())
                                    .callTimeDateStart(callMethodRest.getDateStart())
                                    .callTimeDateEnd(callMethodRest.getDateEnd())
                                    .callTimeDelta(callMethodRest.getCallTimeDelta())
                                    .method(callMethodRest.getMethodId())
                                    .headers(callMethodRest.getHeaders())
                                    .body(callMethodRest.getBody())
                                    .statusCode(callMethodRest.getStatusCode())
                                    .responseHeaders(callMethodRest.getResponseHeaders())
                                    .responseBody(callMethodRest.getResponseBody())
                                    .errorCallMessage(callMethodRest.getErrorCall())
                                    .libraryVersion(callMethodRest.getLibraryVersion())
                                    .build())
                            .toList())
                    .restCallMethod(
                            cm.getRestCallMethod()==null?null: RestCall.builder()
                                    //.messageId(cm.getRestCallMethod().getMessageId())
                                    .uri(cm.getRestCallMethod().getUri())
                                    .callTimeDateStart(cm.getRestCallMethod().getDateStart())
                                    .callTimeDateEnd(cm.getRestCallMethod().getDateEnd())
                                    .callTimeDelta(cm.getRestCallMethod().getCallTimeDelta())
                                    .method(cm.getRestCallMethod().getMethodId())
                                    .headers(cm.getRestCallMethod().getHeaders())
                                    .body(cm.getRestCallMethod().getBody())
                                    .statusCode(cm.getRestCallMethod().getStatusCode())
                                    .responseHeaders(cm.getRestCallMethod().getResponseHeaders())
                                    .responseBody(cm.getRestCallMethod().getResponseBody())
                                    .errorCallMessage(cm.getRestCallMethod().getErrorCall())
                                    .libraryVersion(cm.getRestCallMethod().getLibraryVersion())
                                    .build()
                    )
                    .queueCalls(cm.getCallMethodQueue().stream().map(callMethodQueueSendDTO ->
                            QueueCall.builder()
                                    .messageId(callMethodQueueSendDTO.getMessageId())
                                    .callTimeDateStart(callMethodQueueSendDTO.getCallTimeDateStart())
                                    .callTimeDateEnd(callMethodQueueSendDTO.getCallTimeDateEnd())
                                    .callTimeDelta(callMethodQueueSendDTO.getCallTimeDelta().doubleValue())
                                    .messageBody(callMethodQueueSendDTO.getMessageBody())
                                    .queueTopic(callMethodQueueSendDTO.getTopicName())
                                    .queueServer(callMethodQueueSendDTO.getQueueServer())
                                    .libraryVersion(callMethodQueueSendDTO.getLibraryVersion())
                                    .errorCall(callMethodQueueSendDTO.getErrorCall())
                                    .isError(callMethodQueueSendDTO.getIsError())
                                    .build()
                    ).toList())
                    .codeResponse(cm.getCodeResponse())
                    .operationType(cm.getOperationType())
                    .libraryVersion(cm.getLibraryVersion())
                    .build();
            dtoMap.put(cm.getId(), dto);
        }

        HierarchyMethodUIDto root = null;

        for (CallMethodDTO cm : callMethods) {
            HierarchyMethodUIDto currentDto = dtoMap.get(cm.getId());
            var parentId = cm.getCallIdParent();

            if (parentId == null) {
                if (root == null) {
                    root = currentDto;
                } else {
                    System.err.println("Обнаружено несколько корневых узлов. Дополнительные корни будут игнорироваться.");
                }
            } else {
                HierarchyMethodUIDto parentDto = dtoMap.get(parentId);
                if (parentDto != null) {
                    parentDto.getChildCalls().add(currentDto);
                } else {
                    System.err.println("Родитель с messageId " + parentId + " не найден для messageId " + cm.getId());
                    if (root == null) {
                        root = currentDto;
                    }
                }
            }
        }

        return root;
    }

    private static String combineSqlAndParams(String sql, String paramString) {
        try {
            if (paramString == null) paramString = "";
            if (sql == null) sql = "";

            paramString = paramString.trim();

            if (paramString.startsWith("{")) {
                paramString = paramString.substring(1);
            }
            if (paramString.endsWith("}")) {
                paramString = paramString.substring(0, paramString.length() - 1);
            }

            String[] chunks = paramString.split("\\},\\{");

            // 2. Парсим каждый кусочек в (index, value, type)
            List<Param> paramList = new ArrayList<>();

            for (String chunk : chunks) {
                String[] parts = chunk.split(",", 3);

                if (parts.length < 3) {
                    continue;
                }

                String indexStr = parts[0].trim();
                String value = parts[1].trim();
                String type = parts[2].trim();

                int paramIndex = Integer.parseInt(indexStr);
                String paramType = type.toUpperCase();

                paramList.add(new Param(paramIndex, value, paramType));
            }

            paramList.sort(Comparator.comparingInt(Param::getIndex));


            for (Param p : paramList) {
                String replacedValue;
                switch (p.type) {
                    case "INT":
                    case "INTEGER":
                    case "LONG":
                    case "DOUBLE":
                    case "NULL":
                    case "FLOAT":
                        replacedValue = p.value;
                        break;
                    default:
                        replacedValue = "'" + p.value + "'";
                        break;
                }
                sql = sql.replaceFirst("\\?", replacedValue);
            }

            return sql;
        } catch (Exception e) {
            return sql + "\n" + paramString;
        }
    }

    @Getter
    private static class Param {
        private int index;
        private String value;
        private String type;

        public Param(int index, String value, String type) {
            this.index = index;
            this.value = value;
            this.type = type;
        }
    }

}
