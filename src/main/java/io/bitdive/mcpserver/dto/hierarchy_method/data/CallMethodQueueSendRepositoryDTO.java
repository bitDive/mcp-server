package io.bitdive.mcpserver.dto.hierarchy_method.data;

import java.util.UUID;

public interface CallMethodQueueSendRepositoryDTO {
    UUID getMessageId();
    UUID getSpanId();
    UUID getTraceId();
    UUID getCallIdMessage();
    String getMessageBody();
    String getCallTimeDateStart();
    String getCallTimeDateEnd();
    Long getCallTimeDelta();
    String getQueueServer();
    String getTopicName();
    String getLibraryVersion();
    String getErrorCall();
    Boolean getIsError();
}
