package io.bitdive.mcpserver.component;

import io.bitdive.mcpserver.config.securety.dto.CurrentUser;
import io.bitdive.mcpserver.db.repository.CallMethodRepository;
import io.bitdive.mcpserver.db.repository.ServiceNodeForModuleRepository;
import io.bitdive.mcpserver.dto.last_call.LastCallReturn;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.List;

import static io.bitdive.mcpserver.utils.UserViewModule.isViewForModule;


@Component
@RequiredArgsConstructor
public class LastCallComponent {
    private final ServiceNodeForModuleRepository serviceNodeForModuleRepository;
    private final CallMethodRepository callMethodRepository;

    public List<LastCallReturn> lastCallService(String moduleName , String serviceName , CurrentUser currentUser) {

        if (!isViewForModule(currentUser,moduleName)) return List.of();

        var lastNodeIdOptional = serviceNodeForModuleRepository.findNativeLastNodeForModuleAndService(moduleName.toLowerCase(), serviceName.toLowerCase());
        if (lastNodeIdOptional.isPresent()) {
            var lastCall = callMethodRepository.lastCallModuleAndService(lastNodeIdOptional.get().getId());
            return lastCall.stream().map(lastCallModuleAndServiceDTO ->
                    new LastCallReturn(
                            lastCallModuleAndServiceDTO.getMessageId(),
                            lastCallModuleAndServiceDTO.getModuleName(),
                            lastCallModuleAndServiceDTO.getServiceName(),
                            lastCallModuleAndServiceDTO.getClassName(),
                            lastCallModuleAndServiceDTO.getMethodName(),
                            lastCallModuleAndServiceDTO.getCallDateTime().atOffset(ZoneOffset.UTC)
                    )
            ).toList();

        }
        return List.of();

    }
}
