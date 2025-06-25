package org.example.chessmystic.Repository;

import org.example.chessmystic.Models.chess.PieceColor;
import org.example.chessmystic.Models.chess.PieceType;
import org.example.chessmystic.Models.rpg.Rarity;
import org.example.chessmystic.Models.rpg.RPGPiece;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RPGPieceRepository extends MongoRepository<RPGPiece, String> {
    List<RPGPiece> findByType(PieceType type);
    List<RPGPiece> findByColor(PieceColor color);
    List<RPGPiece> findByRarity(Rarity rarity);
    List<RPGPiece> findByIsJokerTrue();

    @Query("{'attack': {$gte: ?0}}")
    List<RPGPiece> findStrongPieces(int minAttack);

    @Query("{'rarity': {$in: ?0}}")
    List<RPGPiece> findByRarities(List<Rarity> rarities);
}
