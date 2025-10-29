package io.bitdive.mcpserver.dto.last_call;

import java.time.Instant;
import java.util.UUID;

public interface LastCallModuleAndServiceDTO {
    public UUID getMessageId();

    public String getModuleName();

    public String getServiceName();

    public String getClassName();

    public String getMethodName();

    public Instant getCallDateTime();
}
