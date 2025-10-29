package io.bitdive.mcpserver.utils;

import io.bitdive.mcpserver.dto.hierarchy_method.data.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CallMethodConverter {

    public static CallMethodDTO toDTO(CallMethodRepositoryDTO callMethod,
                                      List<CallMethodSqlRepositoryDTO> callMethodSqlList,
                                      List<CallMethodRestRepositoryDTO> callMethodRests,
                                      List<CallMethodRestRepositoryDTO> callsRestServiceList,
                                      List<CallMethodQueueSendRepositoryDTO> callQueueSendList
    ) {
        if (callMethod == null) {
            return null;
        }

        List<CallMethodRestDTO> callMethodRestDTOS = convertCallMethodRestDTOS(callMethodRests);
        CallMethodRestDTO callServiceRestDTOS = convertCallMethodRestDTOS(callsRestServiceList).stream().findFirst().orElse(null);
        List<CallMethodSqlDTO> callMethodSqlDTOS = convertCallMethodSqlDTOS(callMethodSqlList);
        List<CallMethodQueueSendDTO> callMethodQueueDTOS = convertCallMethodQueueDTOS(callQueueSendList);

        return CallMethodDTO.builder()
                .id(callMethod.getMessageId())
                .traceId(callMethod.getTraceId())
                .spanId(callMethod.getSpanId())
                .className(callMethod.getClassName())
                .methodName(callMethod.getMethodName())
                .callIdParent(callMethod.getCallIdParent())
                .args(callMethod.getArgs())
                .callTimeDateStart(callMethod.getCallTimeDateStart().atOffset(ZoneOffset.UTC))
                .callTimeDateEnd(callMethod.getCallTimeDateEnd().atOffset(ZoneOffset.UTC))
                .methodReturn(callMethod.getMethodReturn())
                .errorCallMessage(callMethod.getErrorCallMessage())
                .moduleName(callMethod.getModuleName())
                .serviceName(callMethod.getServiceName())
                .inPointFlag(callMethod.getInPointFlag())
                .callTimeDelta(callMethod.getCallTimeDelta())
                .operationType(callMethod.getOperationType())
                .codeResponse(callMethod.getCodeResponse())
                .callMethodSqlList(callMethodSqlDTOS)
                .callMethodRests(callMethodRestDTOS)
                .callMethodQueue(callMethodQueueDTOS)
                .restCallMethod(callServiceRestDTOS)
                .libraryVersion(callMethod.getLibraryVersion())
                .build();
    }

    public static List<CallMethodQueueSendDTO> convertCallMethodQueueDTOS(List<CallMethodQueueSendRepositoryDTO> callMethodQueueList) {
        List<CallMethodQueueSendDTO> callMethodQueueDTOS = new ArrayList<>();
        if (Optional.ofNullable(callMethodQueueList).isPresent()) {
            callMethodQueueDTOS = callMethodQueueList.stream()
                    .map(callMethodQueueSendDTO ->
                            CallMethodQueueSendDTO.builder()
                                    .messageId(callMethodQueueSendDTO.getMessageId())
                                    .spanId(callMethodQueueSendDTO.getSpanId())
                                    .traceId(callMethodQueueSendDTO.getTraceId())
                                    .callIdMessage(callMethodQueueSendDTO.getCallIdMessage())
                                    .messageBody(callMethodQueueSendDTO.getMessageBody())
                                    .callTimeDateStart(OffsetDateTime.parse(callMethodQueueSendDTO.getCallTimeDateStart()))
                                    .callTimeDateEnd(OffsetDateTime.parse(callMethodQueueSendDTO.getCallTimeDateEnd()))
                                    .callTimeDelta(callMethodQueueSendDTO.getCallTimeDelta())
                                    .queueServer(callMethodQueueSendDTO.getQueueServer())
                                    .topicName(callMethodQueueSendDTO.getTopicName())
                                    .libraryVersion(callMethodQueueSendDTO.getLibraryVersion())
                                    .errorCall(callMethodQueueSendDTO.getErrorCall())
                                    .isError(callMethodQueueSendDTO.getIsError())
                                    .build()
                    ).toList();
        }
        return callMethodQueueDTOS;
    }

    private static List<CallMethodSqlDTO> convertCallMethodSqlDTOS(List<CallMethodSqlRepositoryDTO> callMethodSqlList) {
        List<CallMethodSqlDTO> callMethodSqlDTOS = new ArrayList<>();
        if (Optional.ofNullable(callMethodSqlList).isPresent()) {
            callMethodSqlDTOS = callMethodSqlList.stream()
                    .map(callMethodSqlRepositoryDTO ->
                            CallMethodSqlDTO.builder()
                                    .id(callMethodSqlRepositoryDTO.getId())
                                    .spanId(callMethodSqlRepositoryDTO.getSpanId())
                                    .traceId(callMethodSqlRepositoryDTO.getTraceId())
                                    .messageId(callMethodSqlRepositoryDTO.getMessageId())
                                    .callTimeDateStart(OffsetDateTime.parse(callMethodSqlRepositoryDTO.getCallTimeDateStart()))
                                    .callTimeDateEnd(OffsetDateTime.parse(callMethodSqlRepositoryDTO.getCallTimeDateEnd()))
                                    .callTimeDelta(callMethodSqlRepositoryDTO.getCallTimeDelta())
                                    .callIdMessage(callMethodSqlRepositoryDTO.getCallIdMessage())
                                    .libraryVersion(callMethodSqlRepositoryDTO.getLibraryVersion())
                                    .callMethodSqlParam(callMethodSqlRepositoryDTO.getCallMethodSqlParam())
                                    .callMethodSqlText(callMethodSqlRepositoryDTO.getCallMethodSqlText())
                                    .errorCallMessage(callMethodSqlRepositoryDTO.getErrorCallMessage())
                                    .build()
                    ).toList();
        }
        return callMethodSqlDTOS;
    }

    private static List<CallMethodRestDTO> convertCallMethodRestDTOS(List<CallMethodRestRepositoryDTO> callMethodRests) {
        List<CallMethodRestDTO> callMethodRestDTOS = new ArrayList<>();
        if (Optional.ofNullable(callMethodRests).isPresent()) {
            callMethodRestDTOS = callMethodRests.stream()
                    .map(callMethodRestRepositoryDTO ->
                            CallMethodRestDTO.builder()
                                    .traceId(callMethodRestRepositoryDTO.getTraceId())
                                    .messageId(callMethodRestRepositoryDTO.getMessageId())
                                    .callIdMessage(callMethodRestRepositoryDTO.getCallIdMessage())
                                    .dateStart(OffsetDateTime.parse(callMethodRestRepositoryDTO.getDateStart()))
                                    .dateEnd(OffsetDateTime.parse(callMethodRestRepositoryDTO.getDateEnd()))
                                    .callTimeDelta(callMethodRestRepositoryDTO.getCallTimeDelta())
                                    .uri(callMethodRestRepositoryDTO.getUrl())
                                    .methodId(callMethodRestRepositoryDTO.getMethodName())
                                    .headers(callMethodRestRepositoryDTO.getHeaders())
                                    .body(callMethodRestRepositoryDTO.getBody())
                                    .statusCode(callMethodRestRepositoryDTO.getStatusCode())
                                    .responseHeaders(callMethodRestRepositoryDTO.getResponseHeaders())
                                    .responseBody(callMethodRestRepositoryDTO.getResponseBody())
                                    .errorCall(callMethodRestRepositoryDTO.getErrorCall())
                                    .libraryVersion(callMethodRestRepositoryDTO.getLibraryVersion())
                                    .build())
                    .toList();
        }
        return callMethodRestDTOS;
    }
}
