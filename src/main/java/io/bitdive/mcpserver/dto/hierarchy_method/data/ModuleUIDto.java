package io.bitdive.mcpserver.dto.hierarchy_method.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@Getter
@AllArgsConstructor
public class ModuleUIDto {
    private String name;
    private Set<ServiceUIDto> serviceUI;
}
