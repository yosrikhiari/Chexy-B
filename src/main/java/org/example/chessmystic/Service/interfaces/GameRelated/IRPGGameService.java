package org.example.chessmystic.Service.interfaces.GameRelated;

import org.example.chessmystic.Models.Mechanics.RPGGameState;
import org.example.chessmystic.Models.rpg.BoardEffect;
import org.example.chessmystic.Models.Transactions.RPGModifier;
import org.example.chessmystic.Models.rpg.RPGPiece;

import java.util.List;
import java.util.Optional;

public interface IRPGGameService {
    RPGGameState createRPGGame(String userId, String gameSessionId, boolean isMultiplayer);
    Optional<RPGGameState> findById(String gameId);
    RPGGameState findByGameSessionId(String gameSessionId);
    RPGGameState progressToNextRound(String gameId);
    RPGGameState addPieceToArmy(String gameId, RPGPiece piece);
    RPGGameState addModifier(String gameId, RPGModifier modifier);
    RPGGameState addBoardEffect(String gameId, BoardEffect effect);
    RPGGameState updateScore(String gameId, int scoreToAdd);
    RPGGameState updateCoins(String gameId, int coinsToAdd);
    RPGGameState endGame(String gameId, boolean victory);
    List<RPGGameState> findActiveGamesByUser(String userId);
    List<RPGGameState> findGamesByUser(String userId);
    RPGGameState purchaseShopItem(String gameId, String shopItemId);
}