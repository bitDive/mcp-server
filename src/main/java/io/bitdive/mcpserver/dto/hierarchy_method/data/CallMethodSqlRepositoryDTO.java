package io.bitdive.mcpserver.dto.hierarchy_method.data;

import java.util.UUID;

public interface CallMethodSqlRepositoryDTO {

    Integer getId();

    UUID getSpanId();

    UUID getTraceId();

    UUID getMessageId();

    UUID getCallIdMessage();

    String getCallMethodSqlText();

    String getCallMethodSqlParam();

    String getCallTimeDateStart();

    String getCallTimeDateEnd();

    Double getCallTimeDelta();

    String getErrorCallMessage();

    String getLibraryVersion();

    Integer getUrlConnectId();
}
