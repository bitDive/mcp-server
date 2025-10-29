package io.bitdive.mcpserver.db.repository;

import io.bitdive.mcpserver.db.entity.CallMethod;
import io.bitdive.mcpserver.dto.hierarchy_method.data.CallMethodRepositoryDTO;
import io.bitdive.mcpserver.dto.hierarchy_method.data.FindClassAndMethodBetweenDateDTO;
import io.bitdive.mcpserver.dto.last_call.LastCallModuleAndServiceDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CallMethodRepository extends JpaRepository<CallMethod, String>, JpaSpecificationExecutor<CallMethod> {


    @Query(value = """
            with method_call_base as (SELECT v.module_id,
                                                                                                          v.service_id,
                                                                                                          v.class_id,
                                                                                                          v.method_id,
                                                                                                          v.call_time_date_start
            
                                                                                                   FROM call_method v
                                                                                                   WHERE v.span_id = :span_id
                                                                                                     AND v.trace_id = :trace_id
                                                                                                     AND v.message_id = :message_id
                                                                                                    ),
                                                                              method_call_between as (select cm.* , mcb.call_time_date_start as call_time_date_start_base
                                                                                                      from call_method cm,
                                                                                                           method_call_base mcb
                                                                                                      where cm.module_id = mcb.module_id
                                                                                                        and cm.service_id = mcb.service_id
                                                                                                        and cm.class_id = mcb.class_id
                                                                                                        and cm.method_id = mcb.method_id
                                                                                                        and cm.call_time_date_start BETWEEN
                                                                                                          mcb.call_time_date_start - (INTERVAL '1 min' * :deltaFind)
                                                                                                          AND
                                                                                                          mcb.call_time_date_start + (INTERVAL '1 min' * :deltaFind))
                                                                  select distinct mm.* from (           (select mcb.module_id as moduleId,
                                                                                     mcb.service_id as serviceId,
                                                                                     mcb.class_id as classId,
                                                                                     mcb.method_id as methodId ,
                                                                                     mcb.call_time_delta as callTimeDelta,
                                                                                     mcb.call_time_date_start as callTimeDateStart,
                                                                                     mcb.span_id as spanId,
                                                                                     mcb.trace_id as traceId,
                                                                                     mcb.message_id as messageId
                                                                              from method_call_between mcb
                                                                              where mcb.call_time_date_start
                                                                                        between  mcb.call_time_date_start- (INTERVAL '1 min' * :deltaFind) and mcb.call_time_date_start_base
                                                                              order by mcb.call_time_date_start desc LIMIT 10)
                                                                         union all
                                                                         (select mcb.module_id as moduleId,
                                                                                 mcb.service_id as serviceId,
                                                                                 mcb.class_id as classId,
                                                                                 mcb.method_id as methodId ,
                                                                                 mcb.call_time_delta as callTimeDelta,
                                                                                 mcb.call_time_date_start as callTimeDateStart,
                                                                                 mcb.span_id as spanId,
                                                                                 mcb.trace_id as traceId,
                                                                                 mcb.message_id as messageId
                                                                          from method_call_between mcb
                                                                          where mcb.call_time_date_start
                                                                                    between mcb.call_time_date_start_base and mcb.call_time_date_start + (INTERVAL '1 min' * :deltaFind)
                                                                          order by mcb.call_time_date_start  LIMIT 10)) as mm
            """, nativeQuery = true)
    List<CallMethodRepositoryDTO> findHistCallMethod(
            UUID span_id,
            UUID trace_id,
            UUID message_id,
            Integer deltaFind
    );


    @Query(value = """
            WITH RECURSIVE call_hierarchy AS (
                SELECT *
                FROM call_method c
                WHERE message_id = :messageId
                  AND in_point_flag = TRUE
                  AND c.call_time_date_start BETWEEN
                      CAST(:dateCall AS TIMESTAMPTZ) AND CAST(:dateCall AS TIMESTAMPTZ) + INTERVAL '1 day'
                UNION ALL
                SELECT cm.*
                FROM call_method cm
                         INNER JOIN call_hierarchy ch
                                    ON cm.call_id_parent = ch.message_id
                WHERE cm.call_time_date_start BETWEEN
                      CAST(:dateCall AS TIMESTAMPTZ) AND CAST(:dateCall AS TIMESTAMPTZ) + INTERVAL '1 day'
            )
            SELECT
                c.message_id               AS message_id,
                c.trace_id               AS trace_id,
                c.span_id                AS span_id,
                c.call_id_parent           AS call_id_parent,
                m.name                     AS module_name,
                sm.name                    AS service_name,
                cns.name                   AS class_name,
                mcn.method_name            AS method_name,
                ot.operation_type          AS operation_type,
                c.args                     AS args,
                c.method_return            AS method_return,
                c.error_call_message       AS error_call_message,
                c.call_time_date_start     AS call_time_date_start,
                c.call_time_date_end       AS call_time_date_end,
                c.in_point_flag            AS in_point_flag,
                c.call_time_delta          AS call_time_delta,
                c.code_response            AS code_response,
                c.date_insert              AS date_insert,
                lv.library_version         AS library_version
            FROM call_hierarchy c
                     JOIN module m                ON c.module_id = m.id
                     JOIN service_for_module sm   ON c.service_id = sm.id
                     JOIN class_name_for_service cns  ON c.class_id = cns.id
                     JOIN method_for_class_name mcn    ON c.method_id = mcn.id
                     JOIN operation_type ot       ON c.operation_type_id = ot.id
                     JOIN library_version lv      ON c.library_version_id = lv.id
            """, nativeQuery = true)
    List<CallMethodRepositoryDTO> findTreeCall(UUID messageId);


    @Query(value = """
            WITH RECURSIVE call_hierarchy AS (
                SELECT *
                FROM call_method c
                WHERE message_id = :messageId
                  AND in_point_flag = TRUE
                UNION ALL
                SELECT cm.*
                FROM call_method cm
                         INNER JOIN call_hierarchy ch
                                    ON cm.call_id_parent = ch.message_id
            )
            SELECT
                c.message_id as messageId,
                c.trace_id as traceId,
                c.span_id as spanId,
                cns.name as className,
                mcn.method_name as methodName,
                c.call_id_parent as callIdParent,
                decrypt_decompress(c.args , :keyDecrypt) as args,
                c.call_time_date_start as callTimeDateStart,
                c.call_time_date_end as callTimeDateEnd,
                decrypt_decompress(c.method_return , :keyDecrypt) as methodReturn,
                decrypt_decompress(c.error_call_message , :keyDecrypt) as errorCallMessage,
                m.name as moduleName,
                sm.name as serviceName,
                c.in_point_flag as inPointFlag,
                c.call_time_delta as callTimeDelta,
                ot.operation_type as operationType,
                c.code_response as codeResponse,
                c.date_insert as dateInsert,
                lv.library_version as libraryVersion,
                c.service_call_id as serviceCallId
            FROM call_hierarchy c
                     JOIN module m                ON c.module_id = m.id
                     JOIN service_for_module sm   ON c.service_id = sm.id
                     JOIN class_name_for_service cns  ON c.class_id = cns.id
                     JOIN method_for_class_name mcn    ON c.method_id = mcn.id
                     JOIN operation_type ot       ON c.operation_type_id = ot.id
                     JOIN library_version lv      ON c.library_version_id = lv.id
            """, nativeQuery = true)
    List<CallMethodRepositoryDTO> findTreeCall(UUID messageId, String keyDecrypt);

    @Query(value = """
            SELECT
                c.in_point_message_id as inPointMessageId
            FROM call_method c
                     JOIN module m                ON c.module_id = m.id
                     JOIN service_for_module sm   ON c.service_id = sm.id
                     JOIN class_name_for_service cns  ON c.class_id = cns.id
                     JOIN method_for_class_name mcn    ON c.method_id = mcn.id
            where
               c.call_time_date_start BETWEEN CAST(:dateCallStart AS TIMESTAMPTZ) AND CAST(:dateCallEnd AS TIMESTAMPTZ) and
               cns.name=:className and
               mcn.method_name=:methodName
                    """,
            nativeQuery = true)
    List<FindClassAndMethodBetweenDateDTO> findClassAndMethodBetweenDate(String className, String methodName, String dateCallStart, String dateCallEnd);

    @Query(value = """
                    select cm.message_id as messageId ,
                           m.name as methodName ,
                           sfm.name as serviceName ,
                           cnfs.name as className ,
                           mfcn.method_name as methodName ,
                           cm.call_time_date_start as callDateTime
                    from call_method cm
                             left join module m on cm.module_id=m.id
                             left join service_for_module sfm on sfm.module_id=cm.module_id and sfm.id=cm.service_id
                             left join class_name_for_service cnfs on cnfs.service_id=sfm.id and cnfs.id=cm.class_id
                             left join method_for_class_name mfcn on mfcn.class_id=cnfs.id and mfcn.id=cm.method_id and mfcn.isinpoint=true
                    where cm.service_node_id=:nodeId and cm.in_point_flag=true
                   """,nativeQuery = true)
    List<LastCallModuleAndServiceDTO> lastCallModuleAndService(Long nodeId);
}
