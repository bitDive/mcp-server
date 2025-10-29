package io.bitdive.mcpserver.db.repository;

import io.bitdive.mcpserver.db.entity.CallMethodSql;
import io.bitdive.mcpserver.dto.hierarchy_method.data.CallMethodSqlRepositoryDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface CallMethodSqlRepository extends JpaRepository<CallMethodSql, Integer> {

    @Query(value = """
                    SELECT 
                       id, spanid, traceid, messageid, 
                       decrypt_decompress(cmsv.callmethodsqlparam, :keyDecrypt) as callMethodSqlParam, 
                       decrypt_decompress(cmsv.sql, :keyDecrypt) as callMethodSqlText, 
                       calltimedatestart as callTimeDateStart, 
                       calltimedateend as callTimeDateEnd, 
                       calltimedelta, 
                       decrypt_decompress(cmsv.errorcallmessage, :keyDecrypt) as errorCallMessage, 
                       callidmessage, libraryversion 
                    FROM call_method_sql_full_data_view cmsv 
                    WHERE cmsv.spanid in (:traceAndSpanIdList) and cast(cmsv.calltimedatestart as timestamp with time zone) between :dateStart and :dateEnd
                  """,
            nativeQuery = true)
    List<CallMethodSqlRepositoryDTO> findSqlSpanIdAndTraceIdAndCallIdParent(
            @Param("traceAndSpanIdList") List<UUID> traceAndSpanIdList,
            @Param("keyDecrypt") String keyDecrypt,
            @Param("dateStart") OffsetDateTime dateStart,
            @Param("dateEnd")OffsetDateTime dateEnd
    );


    @Query(value = "SELECT " +
            "   *, " +
            "   calltimedatestart as callTimeDateStart, " +
            "   calltimedateend as callTimeDateEnd " +
            "FROM call_method_sql_full_data_view cmsv " +
            "WHERE cmsv.spanid in (:traceAndSpanIdList)",
            nativeQuery = true)
    List<CallMethodSqlRepositoryDTO> findBySpanIdAndTraceIdAndCallIdParent(
            @Param("traceAndSpanIdList") List<UUID> traceAndSpanIdList
    );
}
