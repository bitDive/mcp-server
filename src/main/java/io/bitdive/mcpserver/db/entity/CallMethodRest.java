package io.bitdive.mcpserver.db.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Getter
@Table(name = "call_method_rest")
public class CallMethodRest {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

}
