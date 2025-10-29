package io.bitdive.mcpserver.dto.hierarchy_method.data;

import java.util.UUID;

public interface CallMethodRestRepositoryDTO {

    String getId();

    UUID getTraceId();

    UUID getSpanId();

    UUID getMessageId();

    UUID getCallIdMessage();

    String getDateStart();

    String getDateEnd();

    Double getCallTimeDelta();

    String getUrl();

    String getMethodName();

    String getHeaders();

    String getBody();

    String getResponseHeaders();

    String getResponseBody();

    String getStatusCode();

    String getErrorCall();

    String getLibraryVersion();

    String getServiceCallId();

}