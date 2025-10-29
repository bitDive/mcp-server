package io.bitdive.mcpserver.db.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "call_method_queue_send")
public class CallMethodQueueSendEntity {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;
}
