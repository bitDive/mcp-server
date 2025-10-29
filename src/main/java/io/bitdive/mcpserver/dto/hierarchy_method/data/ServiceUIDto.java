package io.bitdive.mcpserver.dto.hierarchy_method.data;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Set;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class ServiceUIDto {
    @EqualsAndHashCode.Exclude
    private String name;
    private Set<ClassNameUIDto> classNameUI;
}
