package io.bitdive.mcpserver.dto.hierarchy_method.data;

public interface ModuleRepositoryDTO {

    String getModule();

    String getService();

    String getClassName();

    String getMethod();

    Boolean getIsInPoint();

    Long getInPointId();

}
