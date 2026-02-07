
package com.yin.cita.model;

import java.util.HashMap;
import java.util.Map;

public class DocumentElement {
    public enum Type {
        TITLE,
        NARRATIVE_TEXT,
        TABLE,
        LIST_ITEM,
        UNCATEGORIZED
    }

    private Type type;
    private String text;
    private Map<String, Object> metadata;

    public DocumentElement() {
        this.metadata = new HashMap<>();
    }

    public DocumentElement(Type type, String text) {
        this.type = type;
        this.text = text;
        this.metadata = new HashMap<>();
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    @Override
    public String toString() {
        return "DocumentElement{" +
                "type=" + type +
                ", text='" + (text.length() > 50 ? text.substring(0, 50) + "..." : text) + '\'' +
                ", metadata=" + metadata +
                '}';
    }
}
