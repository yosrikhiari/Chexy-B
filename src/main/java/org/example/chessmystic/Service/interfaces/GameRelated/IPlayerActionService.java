package org.example.chessmystic.Service.interfaces.GameRelated;

import org.example.chessmystic.Models.Interactions.ActionType;
import org.example.chessmystic.Models.Interactions.PlayerAction;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface IPlayerActionService {
    @Transactional
    PlayerAction recordAction(String gameSessionId, String playerId, ActionType actionType,
                              int fromX, int fromY, int toX, int toY, String gameHistoryId,
                              String rpgGameStateId, int roundNumber, String abilityUsed,
                              int damageDealt, boolean isCriticalHit);

    List<PlayerAction> getActionsForGameSession(String gameSessionId);

    List<PlayerAction> getActionsForPlayer(String playerId);

    List<PlayerAction> getActionsForGameAndRound(String gameSessionId, int roundNumber);

    List<PlayerAction> getActionsByType(String gameSessionId, ActionType actionType);
}


