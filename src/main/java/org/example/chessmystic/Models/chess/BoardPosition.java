package org.example.chessmystic.Models.chess;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BoardPosition {
    private int row;
    private int col;

    private int torow;
    private int tocol;

    public BoardPosition(int row, int col) {
        this.row = row;
        this.col = col;
        this.torow = -1; // Indicate unused
        this.tocol = -1; // Indicate unused
    }

}