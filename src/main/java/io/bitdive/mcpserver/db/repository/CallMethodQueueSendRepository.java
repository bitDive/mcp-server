package io.bitdive.mcpserver.db.repository;

import io.bitdive.mcpserver.db.entity.CallMethodQueueSendEntity;
import io.bitdive.mcpserver.dto.hierarchy_method.data.CallMethodQueueSendRepositoryDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CallMethodQueueSendRepository extends JpaRepository<CallMethodQueueSendEntity, Long> {

    @Query(value = """
                   SELECT
                       cmqs.message_id as messageId,                   
                       cmqs.span_id as spanId , cmqs.trace_id as traceId,  cmqs.call_id_message as callIdMessage,
                       decrypt_decompress(cmqs.message_body, :keyDecrypt) as messageBody,
                       to_char(cmqs.call_time_date_start, 'YYYY-MM-DD"T"HH24:MI:SS.USOF')  as callTimeDateStart,
                       to_char(cmqs.call_time_date_end, 'YYYY-MM-DD"T"HH24:MI:SS.USOF') as callTimeDateEnd,
                       cmqs.call_time_delta as callTimeDelta,
                       cmqser.queue_server as queueServer,
                       cmqt.name as topicName,
                       lv.library_version as libraryVersion,
                       decrypt_decompress(cmqs.error_call, :keyDecrypt) as errorCall,
                       cmqs.is_error as isError
                   FROM call_method_queue_send cmqs
                          left join public.library_version lv on cmqs.library_version_id = lv.id
                          left join call_method_queue_server cmqser on cmqser.id=cmqs.queue_server_id
                          left join call_method_queue_topic cmqt on cmqt.id=cmqs.queue_topic_id
                   WHERE cmqs.span_id in (:traceAndSpanIdList) and cmqs.call_time_date_start between :dateStart and :dateEnd
                   """,nativeQuery = true)
    List<CallMethodQueueSendRepositoryDTO> findSqlSpanIdAndTraceIdAndCallIdParent(
            @Param("traceAndSpanIdList") List<UUID> traceAndSpanIdList,
            @Param("keyDecrypt") String keyDecrypt,
            @Param("dateStart") OffsetDateTime dateStart,
            @Param("dateEnd")OffsetDateTime dateEnd
    );
}
