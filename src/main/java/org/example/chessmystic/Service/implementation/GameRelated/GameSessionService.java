package org.example.chessmystic.Service.implementation.GameRelated;

import org.example.chessmystic.Models.GameStateandFlow.GameState;
import org.example.chessmystic.Models.GameStateandFlow.GameTimers;
import org.example.chessmystic.Models.chess.Piece;
import org.example.chessmystic.Models.GameStateandFlow.PlayerTimer;
import org.example.chessmystic.Models.GameStateandFlow.GameMode;
import org.example.chessmystic.Models.GameStateandFlow.GameStatus;
import org.example.chessmystic.Models.chess.PieceColor;
import org.example.chessmystic.Models.chess.PieceType;
import org.example.chessmystic.Models.Tracking.GameSession;
import org.example.chessmystic.Models.Tracking.PlayerSessionInfo;
import org.example.chessmystic.Repository.GameSessionRepository;
import org.example.chessmystic.Service.implementation.UserService;
import org.example.chessmystic.Service.interfaces.GameRelated.IGameSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class GameSessionService implements IGameSessionService {

    private static final Logger logger = LoggerFactory.getLogger(GameSessionService.class);
    private final GameSessionRepository gameSessionRepository;
    private final UserService userService;

    @Autowired
    public GameSessionService(GameSessionRepository gameSessionRepository, UserService userService) {
        this.gameSessionRepository = gameSessionRepository;
        this.userService = userService;
    }

    @Override
    @Transactional
    public GameSession createGameSession(String playerId, GameMode gameMode, boolean isPrivate, String inviteCode) {
        logger.info("Creating game session for player: {}, mode: {}", playerId, gameMode);
        var user = userService.findById(playerId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + playerId));

        PlayerSessionInfo playerInfo = PlayerSessionInfo.builder()
                .userId(playerId)
                .keycloakId(user.getKeycloakId())
                .username(user.getUsername())
                .displayName(user.getFirstName() + " " + user.getLastName())
                .isConnected(true)
                .lastSeen(LocalDateTime.now())
                .build();

        GameSession session = GameSession.builder()
                .whitePlayer(playerInfo)
                .gameMode(gameMode)
                .isPrivate(isPrivate)
                .inviteCode(isPrivate ? (inviteCode != null ? inviteCode : generateInviteCode()) : null)
                .status(GameStatus.WAITING_FOR_PLAYERS)
                .createdAt(LocalDateTime.now())
                .lastActivity(LocalDateTime.now())
                .isActive(true)
                .playerIds(new ArrayList<>(List.of(playerId)))
                .playerLastSeen(new HashMap<>(Map.of(playerId, LocalDateTime.now())))
                .timeControlMinutes(10)
                .incrementSeconds(0)
                .allowSpectators(true)
                .spectatorIds(new ArrayList<>())
                .build();

        GameSession savedSession = gameSessionRepository.save(session);
        logger.info("Game session created: {}", savedSession.getGameId());
        return savedSession;
    }

    @Override
    public Optional<GameSession> findById(String gameId) {
        return gameSessionRepository.findById(gameId);
    }

    @Override
    public Optional<GameSession> findByInviteCode(String inviteCode) {
        return gameSessionRepository.findByInviteCode(inviteCode);
    }

    @Override
    @Transactional
    public GameSession joinGame(String gameId, String playerId, String inviteCode) {
        logger.info("Player {} joining game: {}", playerId, gameId);
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

        if (session.getBlackPlayer() != null) {
            throw new RuntimeException("Game is already full");
        }

        var user = userService.findById(playerId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + playerId));

        PlayerSessionInfo playerInfo = PlayerSessionInfo.builder()
                .userId(playerId)
                .keycloakId(user.getKeycloakId())
                .username(user.getUsername())
                .displayName(user.getFirstName() + " " + user.getLastName())
                .isConnected(true)
                .lastSeen(LocalDateTime.now())
                .build();

        session.setBlackPlayer(playerInfo);
        session.setStatus(GameStatus.ACTIVE);
        session.setStartedAt(LocalDateTime.now());
        session.setLastActivity(LocalDateTime.now());

        List<String> playerIds = new ArrayList<>(session.getPlayerIds());
        playerIds.add(playerId);
        session.setPlayerIds(playerIds);

        Map<String, LocalDateTime> lastSeen = new HashMap<>(session.getPlayerLastSeen());
        lastSeen.put(playerId, LocalDateTime.now());
        session.setPlayerLastSeen(lastSeen);

        initializeGameState(session);

        GameSession updatedSession = gameSessionRepository.save(session);
        logger.info("Player {} joined game: {}", playerId, gameId);
        return updatedSession;
    }

    @Override
    @Transactional
    public GameSession startGame(String gameId) {
        GameSession session = gameSessionRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game session not found with id: " + gameId));

        if (session.getWhitePlayer() == null || session.getBlackPlayer() == null) {
            throw new RuntimeException("Cannot start game without two players");
        }

        if (session.getStatus() != GameStatus.WAITING_FOR_PLAYERS) {
            throw new RuntimeException("Game is not in WAITING_FOR_PLAYERS state");
        }

        session.setStatus(GameStatus.ACTIVE);
        session.setStartedAt(LocalDateTime.now());
        session.setLastActivity(LocalDateTime.now());

        initializeGameState(session);

        GameSession updatedSession = gameSessionRepository.save(session);
        logger.info("Game started: {}", gameId);
        return updatedSession;
    }

    @Override
    @Transactional
    public GameSession endGame(String gameId, String winnerId) {
        GameSession session = gameSessionRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game session not found with id: " + gameId));

        if (winnerId != null && !session.getPlayerIds().contains(winnerId)) {
            throw new RuntimeException("Winner ID is not part of the game");
        }

        session.setStatus(GameStatus.COMPLETED);
        session.setLastActivity(LocalDateTime.now());
        session.setActive(false);

        GameSession updatedSession = gameSessionRepository.save(session);
        logger.info("Game ended: {}, winner: {}", gameId, winnerId != null ? winnerId : "none");
        return updatedSession;
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

        GameSession updatedSession = gameSessionRepository.save(session);
        logger.info("Game status updated: {} to {}", gameId, status);
        return updatedSession;
    }

    @Override
    @Transactional
    public void updatePlayerLastSeen(String gameId, String playerId) {
        GameSession session = gameSessionRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game session not found with id: " + gameId));

        if (!session.getPlayerIds().contains(playerId)) {
            throw new RuntimeException("Player not part of this game session");
        }

        Map<String, LocalDateTime> lastSeen = new HashMap<>(session.getPlayerLastSeen());
        lastSeen.put(playerId, LocalDateTime.now());
        session.setPlayerLastSeen(lastSeen);
        session.setLastActivity(LocalDateTime.now());

        gameSessionRepository.save(session);
        logger.info("Updated last seen for player {} in game: {}", playerId, gameId);
    }

    @Override
    @Transactional
    public GameSession reconnectPlayer(String gameId, String playerId) {
        GameSession session = gameSessionRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game session not found with id: " + gameId));

        if (session.getWhitePlayer() != null && session.getWhitePlayer().getUserId().equals(playerId)) {
            session.getWhitePlayer().setConnected(true);
            session.getWhitePlayer().setLastSeen(LocalDateTime.now());
        } else if (session.getBlackPlayer() != null && session.getBlackPlayer().getUserId().equals(playerId)) {
            session.getBlackPlayer().setConnected(true);
            session.getBlackPlayer().setLastSeen(LocalDateTime.now());
        } else {
            throw new RuntimeException("Player not part of this game session");
        }

        updatePlayerLastSeen(gameId, playerId);

        GameSession updatedSession = gameSessionRepository.save(session);
        logger.info("Player {} reconnected to game: {}", playerId, gameId);
        return updatedSession;
    }

    @Override
    public List<GameSession> findGamesByMode(GameMode gameMode) {
        return gameSessionRepository.findByGameMode(gameMode);
    }

    private void initializeGameState(GameSession session) {
        GameState gameState = GameState.builder()
                .gameSessionId(session.getGameId())
                .userId1(session.getWhitePlayer().getUserId())
                .userId2(session.getBlackPlayer() != null ? session.getBlackPlayer().getUserId() : null)
                .currentTurn(PieceColor.WHITE)
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

        Piece[][] board = new Piece[8][8];
        initializeStandardChessBoard(board);

        session.setGameState(gameState);
        session.setTimers(timers);
        session.setBoard(board);
    }

    private void initializeStandardChessBoard(Piece[][] board) {
        for (int i = 0; i < 8; i++) {
            board[1][i] = Piece.builder().type(PieceType.PAWN).color(PieceColor.WHITE).build();
            board[6][i] = Piece.builder().type(PieceType.PAWN).color(PieceColor.BLACK).build();
        }
        board[0][0] = board[0][7] = Piece.builder().type(PieceType.ROOK).color(PieceColor.WHITE).build();
        board[0][1] = board[0][6] = Piece.builder().type(PieceType.KNIGHT).color(PieceColor.WHITE).build();
        board[0][2] = board[0][5] = Piece.builder().type(PieceType.BISHOP).color(PieceColor.WHITE).build();
        board[0][3] = Piece.builder().type(PieceType.QUEEN).color(PieceColor.WHITE).build();
        board[0][4] = Piece.builder().type(PieceType.KING).color(PieceColor.WHITE).build();
        board[7][0] = board[7][7] = Piece.builder().type(PieceType.ROOK).color(PieceColor.BLACK).build();
        board[7][1] = board[7][6] = Piece.builder().type(PieceType.KNIGHT).color(PieceColor.BLACK).build();
        board[7][2] = board[7][5] = Piece.builder().type(PieceType.BISHOP).color(PieceColor.BLACK).build();
        board[7][3] = Piece.builder().type(PieceType.QUEEN).color(PieceColor.BLACK).build();
        board[7][4] = Piece.builder().type(PieceType.KING).color(PieceColor.BLACK).build();
    }

    private String generateInviteCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }



}