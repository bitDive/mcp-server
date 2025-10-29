package io.bitdive.mcpserver.db.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "service_node_for_module")
public class ServiceNodeForModule {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;
}
