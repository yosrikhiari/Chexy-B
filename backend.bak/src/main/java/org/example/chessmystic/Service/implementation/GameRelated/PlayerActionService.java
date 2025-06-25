package org.example.chessmystic.Service.implementation.GameRelated;

import org.example.chessmystic.Models.Interactions.ActionType;
import org.example.chessmystic.Models.Interactions.PlayerAction;
import org.example.chessmystic.Repository.PlayerActionRepository;
import org.example.chessmystic.Service.interfaces.GameRelated.IPlayerActionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PlayerActionService implements IPlayerActionService {

    private static final Logger logger = LoggerFactory.getLogger(PlayerActionService.class);
    private final PlayerActionRepository playerActionRepository;
    private final GameSessionService gameSessionService;
    private final GameHistoryService gameHistoryService;

    @Autowired
    public PlayerActionService(PlayerActionRepository playerActionRepository,
                               GameSessionService gameSessionService,
                               GameHistoryService gameHistoryService) {
        this.playerActionRepository = playerActionRepository;
        this.gameSessionService = gameSessionService;
        this.gameHistoryService = gameHistoryService;
    }

    @Override
    @Transactional
    public void recordAction(String gameSessionId, String playerId, ActionType actionType,
                             int fromX, int fromY, int toX, int toY, String gameHistoryId,
                             String rpgGameStateId, int roundNumber, String abilityUsed,
                             int damageDealt, boolean isCriticalHit, boolean b) {
        List<PlayerAction> actions = playerActionRepository.findByGameSessionIdOrderBySequenceNumberAsc(gameSessionId);
        int sequenceNumber = actions.isEmpty() ? 1 : actions.get(actions.size() - 1).getSequenceNumber() + 1;

        PlayerAction action = PlayerAction.builder()
                .gameSessionId(gameSessionId)
                .playerId(playerId)
                .actionType(actionType)
                .fromX(fromX)
                .fromY(fromY)
                .toX(toX)
                .toY(toY)
                .timestamp(LocalDateTime.now())
                .sequenceNumber(sequenceNumber)
                .rpgGameStateId(rpgGameStateId)
                .roundNumber(roundNumber)
                .abilityUsed(abilityUsed)
                .damageDealt(damageDealt)
                .isCriticalHit(isCriticalHit)
                .build();

        PlayerAction savedAction = playerActionRepository.save(action);

        var session = gameSessionService.findById(gameSessionId)
                .orElseThrow(() -> new RuntimeException("Game session not found"));
        if (session.getMoveHistoryIds() == null) {
            session.setMoveHistoryIds(new ArrayList<>());
        }
        session.getMoveHistoryIds().add(savedAction.getId());
        gameSessionService.updateGameStatus(gameSessionId, session.getStatus());

        if (gameHistoryId != null) {
            gameHistoryService.addPlayerAction(gameHistoryId, savedAction.getId());
        }

    }

    @Override
    public List<PlayerAction> getActionsForGameSession(String gameSessionId) {
        return playerActionRepository.findByGameSessionIdOrderBySequenceNumberAsc(gameSessionId);
    }

    @Override
    public List<PlayerAction> getActionsForPlayer(String playerId) {
        return playerActionRepository.findByPlayerId(playerId);
    }

    @Override
    public List<PlayerAction> getActionsForGameAndRound(String gameSessionId, int roundNumber) {
        return playerActionRepository.findByGameSessionIdAndRoundNumber(gameSessionId, roundNumber);
    }

    @Override
    public List<PlayerAction> getActionsByType(String gameSessionId, ActionType actionType) {
        return playerActionRepository.findByGameSessionIdAndActionType(gameSessionId, actionType);
    }
}