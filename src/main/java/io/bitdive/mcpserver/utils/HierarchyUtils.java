package io.bitdive.mcpserver.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;
import io.bitdive.mcpserver.dto.hierarchy_method.HierarchyMethodUIDto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

import static io.bitdive.mcpserver.utils.FinalConvertObject.getStringObjectString;

public class HierarchyUtils {
    private static final int MAX_CHILDREN = 30;
    private static final int MAX_TOKENS = 40000; // предел токенов
    private static final double TRIM_RATIO = 0.7; // оставляем 70%

    private static final int MAX_TRIM_PASSES = 15;

    private static final EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
    private static final Encoding encoding = registry.getEncoding(EncodingType.CL100K_BASE);

    /**
     * Универсальный метод: сначала проверяет лимит токенов, затем:
     * 1) обрезает childCalls до MAX_CHILDREN
     * 2) снова проверяет лимит токенов
     * 3) если всё ещё превышает, начинает укорачивать поля args/methodReturn
     */
    public static void enforceLimits(HierarchyMethodUIDto root) {
        if (root == null) {
            return;
        }
        int totalTokens = countTokens(root);
        if (totalTokens <= MAX_TOKENS) {
            return;
        }

        trimChildCallsIterative(root);
        totalTokens = countTokens(root);
        if (totalTokens <= MAX_TOKENS) {
            return;
        }

        trimToTokenLimit(root);
    }

    private static void trimChildCallsIterative(HierarchyMethodUIDto root) {
        if (root == null) {
            return;
        }
        Queue<HierarchyMethodUIDto> queue = new LinkedList<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            HierarchyMethodUIDto current = queue.poll();
            List<HierarchyMethodUIDto> children = current.getChildCalls();
            if (children.size() > MAX_CHILDREN) {
                children.subList(MAX_CHILDREN, children.size()).clear();
            }
            queue.addAll(children);
        }
    }

    private static int countTokens(String prompt) {
        if (prompt == null || prompt.isEmpty()) {
            return 0;
        }
        IntArrayList tokenIds = encoding.encode(prompt);
        return tokenIds.size();
    }

    private static int countTokens(HierarchyMethodUIDto node) {
        if (node == null) {
            return 0;
        }
        try {
            String json = getStringObjectString(node);
            return countTokens(json);
        } catch (Exception e) {
            return 0;
        }
    }


    private static void trimToTokenLimit(HierarchyMethodUIDto root) {
        if (root == null) {
            return;
        }
        int total = countTokens(root);
        int passes = 0;
        while (total > MAX_TOKENS && passes < MAX_TRIM_PASSES) {
            Optional<FieldRef> largest = findLargestField(root);
            if (largest.isEmpty()) {
                break;
            }
            FieldRef ref = largest.get();
            String original = ref.getValue();
            int newLen = Math.max(1, (int) Math.ceil(original.length() * TRIM_RATIO));
            ref.setValue(original.substring(0, newLen));
            total = countTokens(root);

            passes++;
        }
    }

    private static Optional<FieldRef> findLargestField(HierarchyMethodUIDto root) {
        return collectFields(root).stream()
                .max(Comparator.comparingInt(f -> f.getValue().length()));
    }

    private static List<FieldRef> collectFields(HierarchyMethodUIDto node) {
        List<FieldRef> fields = new ArrayList<>();
        collect(node, fields);
        return fields;
    }

    private static void collect(HierarchyMethodUIDto node, List<FieldRef> out) {
        if (node == null) {
            return;
        }
        if (node.getArgs() != null && !node.getArgs().isEmpty()) {
            out.add(new FieldRef(node, true));
        }
        if (node.getMethodReturn() != null && !node.getMethodReturn().isEmpty()) {
            out.add(new FieldRef(node, false));
        }
        for (HierarchyMethodUIDto child : node.getChildCalls()) {
            collect(child, out);
        }
    }

    private static class FieldRef {
        private final HierarchyMethodUIDto node;
        private final boolean isArgs;

        FieldRef(HierarchyMethodUIDto node, boolean isArgs) {
            this.node = node;
            this.isArgs = isArgs;
        }

        String getValue() {
            return isArgs ? node.getArgs() : node.getMethodReturn();
        }

        void setValue(String newVal) {
            if (isArgs) {
                node.setArgs(newVal);
            } else {
                node.setMethodReturn(newVal);
            }
        }
    }
}
