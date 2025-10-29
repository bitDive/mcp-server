package io.bitdive.mcpserver.dto.last_call;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@AllArgsConstructor
@Getter
public class LastCallReturn {
        private UUID traceId;
        private String moduleName;
        private String serviceName;
        private String className;
        private String methodName;
        private OffsetDateTime callDateTime;
}
