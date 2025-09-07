package org.example.chessmystic.Service.implementation.GameRelated;

import org.example.chessmystic.Models.GameStateandFlow.*;
import org.example.chessmystic.Models.Mechanics.RPGGameState;
import org.example.chessmystic.Models.Stats.PlayerGameStats;
import org.example.chessmystic.Models.Stats.PlayerProfile;
import org.example.chessmystic.Models.UIUX.TieResolutionOption;
import org.example.chessmystic.Models.chess.Piece;
import org.example.chessmystic.Models.chess.PieceColor;
import org.example.chessmystic.Models.chess.PieceType;
import org.example.chessmystic.Models.Tracking.GameSession;
import org.example.chessmystic.Models.Tracking.GameHistory;
import org.example.chessmystic.Models.Tracking.GameResult;
import org.example.chessmystic.Models.Tracking.PlayerSessionInfo;
import org.example.chessmystic.Repository.GameSessionRepository;
import org.example.chessmystic.Repository.PlayerProfileRepository;
import org.example.chessmystic.Repository.RPGGameStateRepository;
import org.example.chessmystic.Repository.UserRepository;
import org.example.chessmystic.Service.implementation.UserService;
import org.example.chessmystic.Service.interfaces.GameRelated.IGameSessionService;
import org.example.chessmystic.Controller.TimerWebSocketController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class GameSessionService implements IGameSessionService {

    public static final Logger logger = LoggerFactory.getLogger(GameSessionService.class);
    private final GameSessionRepository gameSessionRepository;
    private final UserService userService;
    private final GameHistoryService gameHistoryService;
    private final PlayerProfileRepository playerProfileRepository;
    private final UserRepository userRepository;
    private final RPGGameStateRepository rpgGameStateRepository;
    private final TimerWebSocketController timerWebSocketController;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public GameSessionService(GameSessionRepository gameSessionRepository,
                              UserService userService,
                              GameHistoryService gameHistoryService,
                              ChessGameService chessGameService,
                              PlayerProfileRepository playerProfileRepository,
                              UserRepository userRepository,
                              RPGGameStateRepository rpgGameStateRepository,
                              TimerWebSocketController timerWebSocketController,
                              SimpMessagingTemplate messagingTemplate) {
        this.gameSessionRepository = gameSessionRepository;
        this.userService = userService;
        this.gameHistoryService = gameHistoryService;
        this.playerProfileRepository = playerProfileRepository;
        this.userRepository = userRepository;
        this.rpgGameStateRepository = rpgGameStateRepository;
        this.timerWebSocketController = timerWebSocketController;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    @Transactional
    public GameSession createGameSession(String playerId, GameMode gameMode, boolean isPrivate, String inviteCode) {
        var user = userService.findById(playerId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + playerId));

        PlayerSessionInfo playerInfo = PlayerSessionInfo.builder()
                .id(UUID.randomUUID().toString())
                .userId(playerId)
                .keycloakId(user.getKeycloakId())
                .username(user.getUsername())
                .displayName(user.getFirstName() + " " + user.getLastName())
                .isConnected(true)
                .lastSeen(LocalDateTime.now())
                .build();

        GameSession session = GameSession.builder()
                .gameId(UUID.randomUUID().toString())
                .whitePlayer(playerInfo)
                .blackPlayer(new ArrayList<>())
                .gameMode(gameMode)
                .isRankedMatch(gameMode == GameMode.TOURNAMENT || gameMode == GameMode.CLASSIC_MULTIPLAYER)
                .isPrivate(isPrivate)
                .inviteCode(isPrivate ? (inviteCode != null ? inviteCode : generateInviteCode()) : null)
                .status(GameStatus.WAITING_FOR_PLAYERS)
                .createdAt(LocalDateTime.now())
                .lastActivity(LocalDateTime.now())
                .isActive(true)
                .playerLastSeen(new HashMap<>(Map.of(playerId, LocalDateTime.now())))
                .timeControlMinutes(10)
                .incrementSeconds(0)
                .allowSpectators(true)
                .spectatorIds(new ArrayList<>())
                .moveHistoryIds(new ArrayList<>())
                .board(initializeStandardChessBoard())
                .BotId(gameMode == GameMode.CLASSIC_SINGLE_PLAYER ? inviteCode : null)
                .build();
        if (gameMode == GameMode.CLASSIC_SINGLE_PLAYER) {
            PlayerSessionInfo botInfo = PlayerSessionInfo.builder()
                    .id("BOT_" + inviteCode)
                    .userId("BOT")
                    .keycloakId(null)
                    .username("ChessBot")
                    .displayName("Chess Bot")
                    .isConnected(true)
                    .lastSeen(LocalDateTime.now())
                    .build();
            session.setBlackPlayer(List.of(botInfo));
        }
        if (gameMode == GameMode.CLASSIC_SINGLE_PLAYER) {
            initializeGameState(session);
            session.setStatus(GameStatus.ACTIVE);
            session.setStartedAt(LocalDateTime.now());
            GameHistory gameHistory = gameHistoryService.createGameHistory(session);
            session.setGameHistoryId(gameHistory.getId());
            session = gameSessionRepository.save(session);
        }

        GameSession savedSession = gameSessionRepository.save(session);
        logger.info("Game session created and saved: ID={}, Status={}, Mode={}", 
                   savedSession.getGameId(), savedSession.getStatus(), savedSession.getGameMode());
        return savedSession;
    }

    @Override
    public Optional<GameSession> findById(String gameId) {
        logger.info("Searching for game session with ID: {}", gameId);
        Optional<GameSession> result = gameSessionRepository.findById(gameId);
        if (result.isPresent()) {
            logger.info("Game session found: ID={}, Status={}, Mode={}", 
                       result.get().getGameId(), result.get().getStatus(), result.get().getGameMode());
        } else {
            logger.warn("Game session not found for ID: {}", gameId);
        }
        return result;
    }

    @Override
    public Optional<GameSession> findByInviteCode(String inviteCode) {
        return gameSessionRepository.findByInviteCode(inviteCode);
    }

    @Override
    @Transactional
    public GameSession joinGame(String gameId, String playerId, String inviteCode) {
        GameSession session = gameSessionRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game session not found with id: " + gameId));

        if (session.getStatus() != GameStatus.WAITING_FOR_PLAYERS) {
            throw new RuntimeException("Game is not accepting new players");
        }

        if (session.isPrivate() && (inviteCode == null || !inviteCode.equals(session.getInviteCode()))) {
            throw new RuntimeException("Invalid invite code");
        }

        if (session.getPlayerIds().contains(playerId)) {
            throw new RuntimeException("Player is already in the game");
        }

        if (session.getGameMode() != GameMode.MULTIPLAYER_RPG && !session.getBlackPlayer().isEmpty()) {
            throw new RuntimeException("Game is already full");
        }

        var user = userService.findById(playerId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + playerId));

        PlayerSessionInfo playerInfo = PlayerSessionInfo.builder()
                .id(UUID.randomUUID().toString())
                .userId(playerId)
                .keycloakId(user.getKeycloakId())
                .username(user.getUsername())
                .displayName(user.getFirstName() + " " + user.getLastName())
                .isConnected(true)
                .lastSeen(LocalDateTime.now())
                .build();

        if (session.getBlackPlayer() == null) {
            session.setBlackPlayer(new ArrayList<>());
        }
        session.getBlackPlayer().add(playerInfo);
        session.getPlayerLastSeen().put(playerId, LocalDateTime.now());

        return gameSessionRepository.save(session);
    }

    @Override
    @Transactional
    public GameSession startGame(String gameId) {
        GameSession session = gameSessionRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game session not found with id: " + gameId));

        if (session.getGameMode() == GameMode.CLASSIC_SINGLE_PLAYER) {
            if (session.getWhitePlayer() == null) {
                throw new RuntimeException("White player not assigned for single-player game");
            }
        } else {
            if (session.getWhitePlayer() == null || session.getBlackPlayer().isEmpty()) {
                throw new RuntimeException("Cannot start game without at least two players");
            }
        }

        if (session.getStatus() == GameStatus.ACTIVE) {
            logger.warn("Game {} is already active, no action needed.", gameId);
            return session;
        }

        if (session.getStatus() != GameStatus.WAITING_FOR_PLAYERS) {
            throw new RuntimeException("Game is not in WAITING_FOR_PLAYERS state");
        }

        session.setStatus(GameStatus.ACTIVE);
        session.setStartedAt(LocalDateTime.now());
        session.setLastActivity(LocalDateTime.now());

        initializeGameState(session);

        GameHistory gameHistory = gameHistoryService.createGameHistory(session);
        session.setGameHistoryId(gameHistory.getId());

        GameSession updatedSession = gameSessionRepository.save(session);
        timerWebSocketController.broadcastTimerUpdate(gameId, updatedSession);
        logger.info("Game started: {}", gameId);
        return updatedSession;
    }

    @Override
    @Transactional
    public GameSession endGame(String gameId, String winnerId, boolean isDraw, TieResolutionOption tieOption) {
        GameSession session = gameSessionRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game session not found with id: " + gameId));
        
        logger.info("endGame called for game {} - current status: {}, requested winnerId: {}, isDraw: {}", 
                   gameId, session.getStatus(), winnerId, isDraw);
        
        if (session.getStatus() == GameStatus.COMPLETED) {
            logger.info("Game {} already completed, skipping update", gameId);
            return session;
        }
        if (session.getStatus() != GameStatus.ACTIVE) {
            throw new RuntimeException("Game is not active and cannot be ended");
        }
        session.setStatus(GameStatus.COMPLETED);
        session.setLastActivity(LocalDateTime.now());
        session.setActive(false);

        // Deactivate timers to prevent further updates
        if (session.getTimers() != null) {
            session.getTimers().getWhite().setActive(false);
            session.getTimers().getBlack().setActive(false);
        }

        GameResult result = buildGameResult(session, isDraw ? null : winnerId, isDraw, tieOption);
        updatePlayerStats(session, winnerId, isDraw, result);
        gameHistoryService.updateGameHistory(session.getGameHistoryId(), result, LocalDateTime.now());
        GameSession updatedSession = gameSessionRepository.save(session);
        logger.info("Game ended: {} with status {}", gameId, updatedSession.getStatus());

        // Notify all subscribers that this game has ended so clients can close/unsubscribe
        try {
            messagingTemplate.convertAndSend("/exchange/amq.topic/game." + gameId + ".ended",
                    Map.of(
                            "gameId", gameId,
                            "status", updatedSession.getStatus().name(),
                            "winnerId", winnerId,
                            "isDraw", isDraw
                    ));
        } catch (Exception ex) {
            logger.warn("Failed to broadcast game end event for {}: {}", gameId, ex.getMessage());
        }
        return updatedSession;
    }

    @Scheduled(fixedRate = 1000) // Every second
    public void updateTimers() {
        List<GameSession> activeSessions = gameSessionRepository.findByStatus(GameStatus.ACTIVE);
        if (!activeSessions.isEmpty()) {
            logger.debug("Processing {} active game sessions for timer updates", activeSessions.size());
        }
        
        for (GameSession session : activeSessions) {
            // Double-check that the session is still active before processing
            if (session.getStatus() != GameStatus.ACTIVE || session.isActive() == false) {
                logger.debug("Skipping game {} - status: {}, active: {}", 
                           session.getGameId(), session.getStatus(), session.isActive());
                continue; // Skip if status changed or game is not active
            }
            
            GameTimers timers = session.getTimers();
            GameState gameState = session.getGameState();
            if (timers == null || gameState == null) continue;

            if (gameState.getCurrentTurn() == PieceColor.white && timers.getWhite().isActive()) {
                int newTime = timers.getWhite().getTimeLeft() - 1;
                timers.getWhite().setTimeLeft(Math.max(0, newTime));
                // Frontend handles timeout events, backend only updates timer
            } else if (gameState.getCurrentTurn() == PieceColor.black && timers.getBlack().isActive()) {
                int newTime = timers.getBlack().getTimeLeft() - 1;
                timers.getBlack().setTimeLeft(Math.max(0, newTime));
                // Frontend handles timeout events, backend only updates timer
            }

            session.setTimers(timers);
            gameSessionRepository.save(session);
            timerWebSocketController.broadcastTimerUpdate(session.getGameId(), session);
        }
    }

    private GameResult buildGameResult(GameSession session, String winnerId, boolean isDraw, TieResolutionOption tieOption) {
        GameResult.GameResultBuilder resultBuilder = GameResult.builder()
                .gameresultId(UUID.randomUUID().toString())
                .gameid(session.getGameId());

        if (winnerId != null) {
            resultBuilder.winnerid(winnerId)
                    .gameEndReason(GameEndReason.timeout);
            var winner = "BOT".equals(winnerId) ? null : userService.findById(winnerId).orElse(null);
            if (winner != null) {
                resultBuilder.winnerName(winner.getFirstName() + " " + winner.getLastName())
                        .winner(session.getWhitePlayer().getUserId().equals(winnerId) ? PieceColor.white : PieceColor.black);
            } else if ("BOT".equals(winnerId)) {
                resultBuilder.winnerName("Chess Bot")
                        .winner(PieceColor.black);
            }
        } else if (isDraw) {
            resultBuilder.gameEndReason(GameEndReason.draw);
            if (tieOption != null && session.getGameMode() == GameMode.MULTIPLAYER_RPG) {
                List<String> playerIds = session.getPlayerIds();
                winnerId = playerIds.get(new Random().nextInt(playerIds.size()));
                resultBuilder.winnerid(winnerId)
                        .winner(session.getWhitePlayer().getUserId().equals(winnerId) ? PieceColor.white : PieceColor.black)
                        .winnerName(userService.findById(winnerId).map(u -> u.getFirstName() + " " + u.getLastName()).orElse("Unknown"))
                        .gameEndReason(GameEndReason.tie_resolved)
                        .tieResolutionOption(tieOption);
            }
        } else {
            resultBuilder.gameEndReason(GameEndReason.draw);
        }

        return resultBuilder.build();
    }

    private void updatePlayerStats(GameSession session, String winnerId, boolean isDraw, GameResult result) {
        List<String> playerIds = session.getPlayerIds();
        boolean isRPGMode = session.getGameMode() == GameMode.SINGLE_PLAYER_RPG ||
                session.getGameMode() == GameMode.MULTIPLAYER_RPG;

        for (String playerId : playerIds) {
            if ("BOT".equals(playerId)) continue;
            PlayerProfile profile = userService.getOrCreatePlayerProfile(playerId);
            PlayerGameStats stats = profile.getGameStats();
            if (stats == null) {
                stats = PlayerGameStats.builder()
                        .totalGamesPlayed(0)
                        .totalGamesWon(0)
                        .classicGamesPlayed(0)
                        .classicGamesWon(0)
                        .rpgGamesPlayed(0)
                        .rpgGamesWon(0)
                        .highestRPGRound(0)
                        .totalRPGScore(0)
                        .winRate(0.0)
                        .currentStreak(0)
                        .build();
                profile.setGameStats(stats);
            }

            final PlayerGameStats finalStats = stats;

            stats.setTotalGamesPlayed(stats.getTotalGamesPlayed() + 1);
            if (isRPGMode) {
                stats.setRpgGamesPlayed(stats.getRpgGamesPlayed() + 1);
            } else {
                stats.setClassicGamesPlayed(stats.getClassicGamesPlayed() + 1);
            }

            boolean isWinner = playerId.equals(winnerId);
            if (isWinner) {
                stats.setTotalGamesWon(stats.getTotalGamesWon() + 1);
                if (isRPGMode) {
                    stats.setRpgGamesWon(stats.getRpgGamesWon() + 1);
                } else {
                    stats.setClassicGamesWon(stats.getClassicGamesWon() + 1);
                }
                stats.setCurrentStreak(stats.getCurrentStreak() >= 0 ? stats.getCurrentStreak() + 1 : 1);
            } else if (!isDraw) {
                stats.setCurrentStreak(stats.getCurrentStreak() <= 0 ? stats.getCurrentStreak() - 1 : -1);
            } else {
                stats.setCurrentStreak(0);
            }

            if (isRPGMode) {
                int currentRound = 0;
                if (session.getRpgGameStateId() != null) {
                    RPGGameState rpgGameState = rpgGameStateRepository.findById(session.getRpgGameStateId())
                            .orElse(null);
                    if (rpgGameState != null) {
                        currentRound = rpgGameState.getCurrentRound();
                        stats.setTotalRPGScore(stats.getTotalRPGScore() + rpgGameState.getScore());
                    }
                }
                stats.setHighestRPGRound(Math.max(stats.getHighestRPGRound(), currentRound));
            }
            stats.setWinRate((double) stats.getTotalGamesWon() / stats.getTotalGamesPlayed() * 100);
            stats.setLastGamePlayed(LocalDateTime.now());


            profile.setGamesPlayed(stats.getTotalGamesPlayed());
            profile.setGamesWon(stats.getTotalGamesWon());
            profile.setLastUpdated(LocalDateTime.now());

            playerProfileRepository.save(profile);

            userService.findById(playerId).ifPresent(user -> {
                user.setGameStats(finalStats);
                userRepository.save(user);
            });
// Points will be calculated and updated by frontend based on streak
            logger.info("Processing player: {}, isWinner: {}, isDraw: {}, ranked: {} - Points will be calculated by frontend", playerId, isWinner, isDraw, session.isRankedMatch());
        }
    }

    @Override
    public List<GameSession> findActiveGamesForPlayer(String playerId) {
        return gameSessionRepository.findActiveGamesByPlayerId(playerId,
                Arrays.asList(GameStatus.WAITING_FOR_PLAYERS, GameStatus.ACTIVE, GameStatus.PAUSED));
    }

    @Override
    public List<GameSession> findAvailableGames() {
        return gameSessionRepository.findByStatus(GameStatus.WAITING_FOR_PLAYERS);
    }

    @Override
    @Transactional
    public GameSession updateGameStatus(String gameId, GameStatus status) {
        GameSession session = gameSessionRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game session not found with id: " + gameId));

        session.setStatus(status);
        session.setLastActivity(LocalDateTime.now());
        return gameSessionRepository.save(session);
    }

    @Override
    @Transactional
    public void updatePlayerLastSeen(String gameId, String playerId) {
        GameSession session = gameSessionRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game session not found with id: " + gameId));

        if (!session.getPlayerIds().contains(playerId)) {
            throw new RuntimeException("Player not part of this game session");
        }

        session.getPlayerLastSeen().put(playerId, LocalDateTime.now());
        session.setLastActivity(LocalDateTime.now());
        gameSessionRepository.save(session);
    }

    @Override
    @Transactional
    public GameSession reconnectPlayer(String gameId, String playerId) {
        GameSession session = gameSessionRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game session not found with id: " + gameId));

        if (session.getWhitePlayer() != null && session.getWhitePlayer().getUserId().equals(playerId)) {
            session.getWhitePlayer().setConnected(true);
            session.getWhitePlayer().setLastSeen(LocalDateTime.now());
        } else if (session.getBlackPlayer().stream().anyMatch(p -> p.getUserId().equals(playerId))) {
            session.getBlackPlayer().stream()
                    .filter(p -> p.getUserId().equals(playerId))
                    .findFirst()
                    .ifPresent(p -> {
                        p.setConnected(true);
                        p.setLastSeen(LocalDateTime.now());
                    });
        } else {
            throw new RuntimeException("Player not part of this game session");
        }

        updatePlayerLastSeen(gameId, playerId);
        return gameSessionRepository.save(session);
    }

    @Override
    public List<GameSession> findGamesByMode(GameMode gameMode) {
        return gameSessionRepository.findByGameMode(gameMode);
    }

    private void initializeGameState(GameSession session) {
        GameState gameState = GameState.builder()
                .gamestateId(UUID.randomUUID().toString())
                .gameSessionId(session.getGameId())
                .currentTurn(PieceColor.white)
                .moveCount(0)
                .isCheck(false)
                .isCheckmate(false)
                .canWhiteCastleKingSide(true)
                .canWhiteCastleQueenSide(true)
                .canBlackCastleKingSide(true)
                .canBlackCastleQueenSide(true)
                .build();

        GameTimers timers = GameTimers.builder()
                .defaultTime(session.getTimeControlMinutes() * 60)
                .white(PlayerTimer.builder()
                        .timeLeft(session.getTimeControlMinutes() * 60)
                        .active(true)
                        .build())
                .black(PlayerTimer.builder()
                        .timeLeft(session.getTimeControlMinutes() * 60)
                        .active(false)
                        .build())
                .build();

        session.setGameState(gameState);
        session.setTimers(timers);
        session.setBoard(initializeStandardChessBoard());
    }

    public Piece[][] initializeStandardChessBoard() {
        Piece[][] board = new Piece[8][8];
        board[0][0] = new Piece(PieceType.ROOK, PieceColor.black);
        board[0][1] = new Piece(PieceType.KNIGHT, PieceColor.black);
        board[0][2] = new Piece(PieceType.BISHOP, PieceColor.black);
        board[0][3] = new Piece(PieceType.QUEEN, PieceColor.black);
        board[0][4] = new Piece(PieceType.KING, PieceColor.black);
        board[0][5] = new Piece(PieceType.BISHOP, PieceColor.black);
        board[0][6] = new Piece(PieceType.KNIGHT, PieceColor.black);
        board[0][7] = new Piece(PieceType.ROOK, PieceColor.black);

        for (int col = 0; col < 8; col++) {
            board[1][col] = new Piece(PieceType.PAWN, PieceColor.black);
        }

        for (int row = 2; row < 6; row++) {
            for (int col = 0; col < 8; col++) {
                board[row][col] = null;
            }
        }

        for (int col = 0; col < 8; col++) {
            board[6][col] = new Piece(PieceType.PAWN, PieceColor.white);
        }

        board[7][0] = new Piece(PieceType.ROOK, PieceColor.white);
        board[7][1] = new Piece(PieceType.KNIGHT, PieceColor.white);
        board[7][2] = new Piece(PieceType.BISHOP, PieceColor.white);
        board[7][3] = new Piece(PieceType.QUEEN, PieceColor.white);
        board[7][4] = new Piece(PieceType.KING, PieceColor.white);
        board[7][5] = new Piece(PieceType.BISHOP, PieceColor.white);
        board[7][6] = new Piece(PieceType.KNIGHT, PieceColor.white);
        board[7][7] = new Piece(PieceType.ROOK, PieceColor.white);

        return board;
    }

    private String generateInviteCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    public GameSession getGameSessionByGameStateId(String gameStateId) {
        for (GameSession gameSession : gameSessionRepository.findAll()) {
            if (gameSession.getGameState() != null &&
                    gameSession.getGameState().getGamestateId() != null &&
                    gameSession.getGameState().getGamestateId().equals(gameStateId)) {
                return gameSession;
            }
        }
        return null;
    }

    @Override
    @Transactional
    public void saveSession(GameSession session) {
        gameSessionRepository.save(session);
    }


    @Transactional
    @Override
    public void isJoinedSpectating(String gameId, String playerId) {
        GameSession session = gameSessionRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game session not found with id: " + gameId));
        if (session.isAllowSpectators()){
            session.getSpectatorIds().add(playerId);
            gameSessionRepository.save(session);
        }
    }

    @Transactional
    @Override
    public void isLeftSpectating(String gameId, String playerId) {
        GameSession session = gameSessionRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game session not found with id: " + gameId));
        if (session.isAllowSpectators()){
            session.getSpectatorIds().remove(playerId);
        }
    }

    @Transactional
    @Override
    public List<String> getAllSpectators(String gameId){
        GameSession session = gameSessionRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game session not found with id: " + gameId));
        return session.getSpectatorIds();
    }

    @Transactional
    @Override
    public void offSpectatorMode(String gameId){
        GameSession session = gameSessionRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game session not found with id: " + gameId));
        session.setAllowSpectators(false);
        gameSessionRepository.save(session);
    }

    @Transactional
    @Override
    public void onSpectatorMode(String gameId){
        GameSession session = gameSessionRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game session not found with id: " + gameId));
        session.setAllowSpectators(true);
        gameSessionRepository.save(session);
    }

}
