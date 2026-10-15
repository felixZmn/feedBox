package de._0x2b.model;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Allowed CSS colour classes for folder icons. Rejects free-text values that
 * would otherwise be echoed into the sidebar HTML as a class name.
 */
@RegisterForReflection
public enum FolderColor {
    F_BASE("f-base"),
    F_GREY("f-grey"),
    F_RED("f-red"),
    F_ORANGE("f-orange"),
    F_YELLOW("f-yellow"),
    F_GREEN("f-green"),
    F_BLUE("f-blue"),
    F_VIOLET("f-violet");

    private static final Set<String> VALUES = Arrays.stream(values())
            .map(FolderColor::cssClass)
            .collect(Collectors.toUnmodifiableSet());

    private final String cssClass;

    FolderColor(String cssClass) {
        this.cssClass = cssClass;
    }

    @JsonValue
    public String cssClass() {
        return cssClass;
    }

    @JsonCreator
    public static FolderColor from(String value) {
        if (value == null || value.isBlank()) {
            return F_BASE;
        }
        for (FolderColor color : values()) {
            if (color.cssClass.equals(value)) {
                return color;
            }
        }
        throw new IllegalArgumentException("Unknown folder color: " + value);
    }

    public static boolean isAllowed(String value) {
        return value != null && VALUES.contains(value);
    }
}
