package org.example.chessmystic.Service.interfaces.GameRelated;

import org.example.chessmystic.Models.Mechanics.RPGGameState;
import org.example.chessmystic.Models.rpg.BoardEffect;
import org.example.chessmystic.Models.Transactions.RPGModifier;
import org.example.chessmystic.Models.rpg.RPGPiece;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface IRPGGameService {

    @Transactional
    RPGGameState createRPGGame(String userId, String gameSessionId, boolean isMultiplayer);

    Optional<RPGGameState> findById(String gameId);

    RPGGameState findByGameSessionId(String gameSessionId);

    @Transactional
    RPGGameState progressToNextRound(String gameId);

    @Transactional
    RPGGameState addPieceToArmy(String gameId, RPGPiece piece, String playerId);

    @Transactional
    RPGGameState addModifier(String gameId, RPGModifier modifier, String playerId);

    @Transactional
    RPGGameState addBoardEffect(String gameId, BoardEffect effect, String playerId);

    @Transactional
    RPGGameState updateScore(String gameId, int scoreToAdd, String playerId);

    @Transactional
    RPGGameState updateCoins(String gameId, int coinsToAdd, String playerId);

    @Transactional
    RPGGameState endGame(String gameId, boolean victory);

    List<RPGGameState> findActiveGamesByUser(String userId);

    List<RPGGameState> findGamesByUser(String userId);

    @Transactional
    RPGGameState purchaseShopItem(String gameId, String shopItemId, String playerId);
}