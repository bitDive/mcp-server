package io.bitdive.mcpserver.dto.hierarchy_method.data;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface CallMethodRepositoryDTO {

    UUID getMessageId();

    UUID getTraceId();

    UUID getSpanId();

    UUID getCallIdParent();

    String getModuleName();

    String getServiceName();

    String getClassName();

    String getMethodName();

    String getOperationType();

    Instant getCallTimeDateStart();

    Instant getCallTimeDateEnd();

    Boolean getInPointFlag();

    Double getCallTimeDelta();

    Integer getCodeResponse();

    String getDataInsert();

    String getLibraryVersion();

    String getArgs();

    String getErrorCallMessage();

    String getMethodReturn();

    String getServiceCallId();

}