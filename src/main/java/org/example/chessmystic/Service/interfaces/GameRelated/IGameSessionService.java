package org.example.chessmystic.Service.interfaces.GameRelated;

import org.example.chessmystic.Models.GameStateandFlow.GameMode;
import org.example.chessmystic.Models.GameStateandFlow.GameStatus;
import org.example.chessmystic.Models.Tracking.GameSession;
import org.example.chessmystic.Models.UIUX.TieResolutionOption;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface IGameSessionService {
    GameSession createGameSession(String playerId, GameMode gameMode, boolean isPrivate, String inviteCode);
    Optional<GameSession> findById(String gameId);
    GameSession joinGame(String gameId, String playerId, String inviteCode);
    GameSession startGame(String gameId);

    @Transactional
    GameSession endGame(String gameId, String winnerId, boolean isDraw, TieResolutionOption tieOption);

    List<GameSession> findActiveGamesForPlayer(String playerId);
    List<GameSession> findAvailableGames();
    GameSession updateGameStatus(String gameId, GameStatus status);
    void updatePlayerLastSeen(String gameId, String playerId);
    GameSession reconnectPlayer(String gameId, String playerId);
    List<GameSession> findGamesByMode(GameMode gameMode);
    Optional<GameSession> findByInviteCode(String inviteCode);
}
