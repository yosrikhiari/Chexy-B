package org.example.chessmystic.Models.chess;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Piece {
    private PieceType type;
    private PieceColor color;
    private boolean hasMoved = false;
    private boolean enPassantTarget = false;
}