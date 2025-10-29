package io.bitdive.mcpserver.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.bitdive.mcpserver.config.securety.dto.CurrentUser;
import io.bitdive.mcpserver.dto.hierarchy_method.HierarchyMethodUIDto;
import io.bitdive.mcpserver.dto.hierarchy_method.data.CallMethodRepositoryDTO;
import io.bitdive.mcpserver.utils.FinalConvertObject;
import io.bitdive.mcpserver.utils.HierarchySearcher;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static io.bitdive.mcpserver.utils.FinalConvertObject.getStringObject;
import static io.bitdive.mcpserver.utils.FinalConvertObject.getStringObjectString;

@Component
@RequiredArgsConstructor
public class MonitoringComponent {

    private final InPointProvider inPointProvider;
    private final PiiMaskClientComponent piiMaskClientComponent;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    static {
        objectMapper.registerModule(new JavaTimeModule());
    }

    public Map<String, Object> findTraceForMethodBetweenTime(String className, String methodName, OffsetDateTime beginDate, OffsetDateTime endDate, CurrentUser currentUser) {
        String beginUtc = beginDate.withOffsetSameInstant(ZoneOffset.UTC).format(fmt);
        String endUtc = endDate.withOffsetSameInstant(ZoneOffset.UTC).format(fmt);

        return inPointProvider.findClassAndMethodBetweenDate(className, methodName, beginUtc, endUtc)
                .stream()
                .map(callMethodRepositoryDTO -> inPointProvider.getInPointOneCall(callMethodRepositoryDTO.getInPointMessageId(),currentUser))
                .collect(Collectors.toMap(
                                dto -> dto.getMessageId().toString(),
                                FinalConvertObject::getStringObject
                        )
                );
    }

    public Map<String, Object> findTraceForMethod(String callId, String className, String methodName, CurrentUser currentUser) {
        var inPointOneCall = inPointProvider.getInPointOneCall(UUID.fromString(callId),currentUser);

        return HierarchySearcher.findInHierarchy(inPointOneCall, className, methodName)
                .map(FinalConvertObject::getStringObject)
                .orElseThrow(() -> new RuntimeException("inPointOneCall not found"));
    }

    public String findTraceAll(String callId , CurrentUser currentUser) {
        var inPointOneCall = inPointProvider.getInPointOneCall(UUID.fromString(callId),currentUser);
        return  getStringObjectString(inPointOneCall) /*piiMaskClientComponent.maskText()*/;
    }


}
