package io.bitdive.mcpserver.dto.hierarchy_method;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HierarchyMethodAlertUI {
    private String alertValue=null;
    private Boolean alertMeaning=false;
}
