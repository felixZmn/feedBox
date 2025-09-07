package de._0x2b.models;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Outline {
    private final Map<String, String> attributes;
    private Outline[] children;

    public Outline(String text) {
        attributes = new HashMap<>();
        this.attributes.put("text", text);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Outline outline = (Outline) o;
        return Objects.equals(attributes, outline.attributes) && Objects.deepEquals(children, outline.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributes, Arrays.hashCode(children));
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }
}
