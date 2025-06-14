package org.example.chessmystic.Models.chess;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PieceColor {
    white("white"),
    black("black"),
    NULL("null"); // If you really need this null value

    private final String value;

    PieceColor(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static PieceColor fromString(String value) {
        if (value == null) return NULL;

        for (PieceColor color : PieceColor.values()) {
            if (color.value.equalsIgnoreCase(value.trim())) {
                return color;
            }
        }
        throw new IllegalArgumentException("Invalid color: " + value);
    }
}