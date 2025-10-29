package io.bitdive.mcpserver.utils;

import io.bitdive.mcpserver.dto.hierarchy_method.HierarchyMethodUIDto;

import java.util.Optional;

public class HierarchySearcher {

    public static Optional<HierarchyMethodUIDto> findInHierarchy(
            HierarchyMethodUIDto node,
            String className,
            String methodName
    ) {
        if (className.equals(node.getClassName()) &&
                methodName.equals(node.getMethodName())) {
            return Optional.of(node);
        }

        for (HierarchyMethodUIDto child : node.getChildCalls()) {
            Optional<HierarchyMethodUIDto> found = findInHierarchy(child, className, methodName);
            if (found.isPresent()) {
                return found;
            }
        }

        return Optional.empty();
    }
}