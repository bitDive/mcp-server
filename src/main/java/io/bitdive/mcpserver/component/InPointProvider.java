package io.bitdive.mcpserver.component;

import io.bitdive.mcpserver.config.securety.dto.CurrentUser;
import io.bitdive.mcpserver.config.vault.VaultService;
import io.bitdive.mcpserver.db.repository.CallMethodQueueSendRepository;
import io.bitdive.mcpserver.db.repository.CallMethodRepository;
import io.bitdive.mcpserver.db.repository.CallMethodRestRepository;
import io.bitdive.mcpserver.db.repository.CallMethodSqlRepository;
import io.bitdive.mcpserver.dto.hierarchy_method.HierarchyMethodUIDto;
import io.bitdive.mcpserver.dto.hierarchy_method.data.*;
import io.bitdive.mcpserver.utils.CallMethodConverter;
import io.bitdive.mcpserver.utils.HierarchyUtils;
import io.bitdive.mcpserver.utils.UtilsCovert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.apache.commons.lang3.ObjectUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import static io.bitdive.mcpserver.utils.UserViewModule.isViewForModule;

@Component
@RequiredArgsConstructor
public class InPointProvider {

    private final VaultService vaultService ;
    private final CallMethodRepository callMethodRepository;
    private final CallMethodRestRepository callMethodRestRepository;
    private final CallMethodSqlRepository callMethodSqlRepository;
    private final CallMethodQueueSendRepository callMethodQueueSendRepository;

    public List<FindClassAndMethodBetweenDateDTO> findClassAndMethodBetweenDate(String className , String methodName , String dateCallStart, String dateCallEnd){
       return callMethodRepository.findClassAndMethodBetweenDate(className, methodName, dateCallStart, dateCallEnd);
    }

    public HierarchyMethodUIDto getInPointOneCall(UUID callId, CurrentUser currentUser) {

        List<CallMethodRepositoryDTO> callList = null;

        long startTime = System.currentTimeMillis();
        if (vaultService.isSLLConnected())
            callList = callMethodRepository.findTreeCall(callId, vaultService.decryptKeyForData());
        else
            callList = callMethodRepository.findTreeCall(callId);

        callList=callList.stream()
                .filter(callMethodRepositoryDTO -> isViewForModule(currentUser,callMethodRepositoryDTO.getModuleName()))
                .toList();

        long endTime = System.currentTimeMillis();
        System.out.println("Call method time: " + (endTime - startTime));

        List<CallMethodRestRepositoryDTO> callsRest = new ArrayList<>();

        var spanIdList = callList.stream()
                .map(CallMethodRepositoryDTO::getSpanId)
                .distinct()
                .toList();

        var dataStart = callList.stream()
                .map(callMethodRepositoryDTO -> callMethodRepositoryDTO.getCallTimeDateStart().atOffset(ZoneOffset.UTC))
                .min((OffsetDateTime::compareTo))
                .orElse(OffsetDateTime.now());

        var dataEnd = callList.stream()
                .map(callMethodRepositoryDTO -> callMethodRepositoryDTO.getCallTimeDateEnd().atOffset(ZoneOffset.UTC))
                .max((OffsetDateTime::compareTo))
                .orElse(OffsetDateTime.now());

        if (vaultService.isSLLConnected())
            callsRest = callMethodRestRepository.findSpanIdAndTraceIdAndCallIdParent(spanIdList, vaultService.decryptKeyForData(), dataStart, dataEnd);
        else
            callsRest = callMethodRestRepository.findTraceAndSpanIdAndCallIdMessage(spanIdList);

        List<CallMethodSqlRepositoryDTO> callsSQL = new ArrayList<>();

        if (vaultService.isSLLConnected())
            callsSQL = callMethodSqlRepository.findSqlSpanIdAndTraceIdAndCallIdParent(spanIdList, vaultService.decryptKeyForData(), dataStart, dataEnd);
        else
            callsSQL = callMethodSqlRepository.findBySpanIdAndTraceIdAndCallIdParent(spanIdList);


        List<CallMethodQueueSendRepositoryDTO> callsQueue =
                callMethodQueueSendRepository.findSqlSpanIdAndTraceIdAndCallIdParent(spanIdList, vaultService.decryptKeyForData(), dataStart, dataEnd);

        List<CallMethodRestRepositoryDTO> finalCallsRest = callsRest;
        List<CallMethodSqlRepositoryDTO> finalCallsSQL = callsSQL;

        var listServiceCallId = callList.stream().map(CallMethodRepositoryDTO::getServiceCallId).filter(Objects::nonNull).toList();

        var CallMethods = callList.stream().map(callMethod -> {

            var callsRestList = finalCallsRest.stream()
                    .filter(callMethodRestRest -> {

                        if (ObjectUtils.isEmpty(callMethodRestRest.getServiceCallId()) ||

                                (ObjectUtils.isNotEmpty(callMethodRestRest.getServiceCallId()) && !listServiceCallId.contains(callMethodRestRest.getServiceCallId()))

                        ) {
                            return callMethodRestRest.getCallIdMessage().equals(callMethod.getMessageId());
                        }
                        return false;
                    })
                    .toList();

            var callsRestServiceList = finalCallsRest.stream()
                    .filter(callMethodRestRest -> {
                        if (!ObjectUtils.isEmpty(callMethodRestRest.getServiceCallId())) {
                            return callMethodRestRest.getServiceCallId().equals(callMethod.getServiceCallId());
                        }
                        return false;
                    })
                    .toList();

            var callsSQLList = finalCallsSQL.stream()
                    .filter(callMethodSql -> callMethodSql.getCallIdMessage().equals(callMethod.getMessageId()))
                    .toList();

            var CallQueueLsit = callsQueue.stream()
                    .filter(callMethodSql -> callMethodSql.getCallIdMessage().equals(callMethod.getMessageId()))
                    .toList();


            return CallMethodConverter.toDTO(callMethod, callsSQLList, callsRestList, callsRestServiceList, CallQueueLsit);
        }).toList();

        var hierarchyMethodUIDto= UtilsCovert.buildDependencyTree(CallMethods);
        HierarchyUtils.enforceLimits(hierarchyMethodUIDto);
        return hierarchyMethodUIDto;

    }

}
