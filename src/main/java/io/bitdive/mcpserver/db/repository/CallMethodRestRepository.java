package io.bitdive.mcpserver.db.repository;

import io.bitdive.mcpserver.db.entity.CallMethodRest;
import io.bitdive.mcpserver.dto.hierarchy_method.data.CallMethodRestRepositoryDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface CallMethodRestRepository extends JpaRepository<CallMethodRest, Integer> {
    @Query(value = """
                SELECT id,
                       spanid,
                       traceid,
                       messageid,
                       callidmessage,
                       datestart as datestart,
                       dateend as dateend,
                       calltimedelta,
                       url,
                       methodname,
                       decrypt_decompress(headers , :keyDecrypt) as headers,
                       decrypt_decompress(body , :keyDecrypt) as body,
                       statuscode,
                       decrypt_decompress(responseheaders , :keyDecrypt) as responseHeaders,
                       decrypt_decompress(responsebody , :keyDecrypt) as responseBody,
                       decrypt_decompress(errorcall , :keyDecrypt) as errorcall,
                       libraryversion,
                       service_call_id as serviceCallId          
                FROM call_method_rest_full_data_view cmv 
                WHERE cmv.spanid in (:traceAndSpanIdList) and cast(cmv.datestart as timestamp with time zone) between :dateStart and :dateEnd
            """
            , nativeQuery = true)
    List<CallMethodRestRepositoryDTO> findSpanIdAndTraceIdAndCallIdParent(
            @Param("traceAndSpanIdList") List<UUID> traceAndSpanIdList,
            @Param("keyDecrypt") String keyDecrypt,
            @Param("dateStart") OffsetDateTime dateStart,
            @Param("dateEnd")OffsetDateTime dateEnd
    );

    @Query(value = "SELECT " +
            " * "+
            "FROM call_method_rest_full_data_view cmv " +
            "WHERE cmv.spanid in (:traceAndSpanIdList)", nativeQuery = true)
    List<CallMethodRestRepositoryDTO> findTraceAndSpanIdAndCallIdMessage(
            @Param("traceAndSpanIdList") List<UUID> traceAndSpanIdList
    );
}
