package org.example.chessmystic.Models.chess;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
public class BoardPosition {
    private int row;
    private int col;
    private int torow;
    private int tocol;

    @JsonCreator
    public BoardPosition(@JsonProperty("row") int row,
                         @JsonProperty("col") int col,
                         @JsonProperty("torow") int torow,
                         @JsonProperty("tocol") int tocol) {
        this.row = row;
        this.col = col;
        this.torow = torow;
        this.tocol = tocol;
    }

    public BoardPosition(int row, int col) {
        this.row = row;
        this.col = col;
        this.torow = -1; // Indicate unused
        this.tocol = -1; // Indicate unused
    }
}