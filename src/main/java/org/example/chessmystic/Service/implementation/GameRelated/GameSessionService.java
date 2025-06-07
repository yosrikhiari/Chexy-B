package org.example.chessmystic.Service.implementation.GameRelated;

import org.example.chessmystic.Models.GameStateandFlow.*;
import org.example.chessmystic.Models.UIUX.TieResolutionOption;
import org.example.chessmystic.Models.chess.Piece;
import org.example.chessmystic.Models.chess.PieceColor;
import org.example.chessmystic.Models.chess.PieceType;
import org.example.chessmystic.Models.Tracking.GameSession;
import org.example.chessmystic.Models.Tracking.GameHistory;
import org.example.chessmystic.Models.Tracking.GameResult;
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
    private final GameHistoryService gameHistoryService;
    private final ChessGameService chessGameService;

    @Autowired
    public GameSessionService(GameSessionRepository gameSessionRepository,
                              UserService userService,
                              GameHistoryService gameHistoryService,
                              ChessGameService chessGameService) {
        this.gameSessionRepository = gameSessionRepository;
        this.userService = userService;
        this.gameHistoryService = gameHistoryService;
        this.chessGameService = chessGameService;
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
                .isRankedMatch(gameMode == GameMode.TOURNAMENT)
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
                .build();

        return gameSessionRepository.save(session);
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

        // Allow multiple black players only for MULTIPLAYER_RPG
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
        session.getPlayerIds().add(playerId);
        session.getPlayerLastSeen().put(playerId, LocalDateTime.now());

        return gameSessionRepository.save(session);
    }

    @Override
    @Transactional
    public GameSession startGame(String gameId) {
        GameSession session = gameSessionRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game session not found with id: " + gameId));

        if (session.getWhitePlayer() == null || session.getBlackPlayer().isEmpty()) {
            throw new RuntimeException("Cannot start game without at least two players");
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

        GameResult.GameResultBuilder resultBuilder = GameResult.builder()
                .gameresultId(UUID.randomUUID().toString())
                .gameid(gameId);

        if (winnerId != null) {
            resultBuilder.winnerid(winnerId)
                    .gameEndReason(GameEndReason.CHECKMATE);
            var winner = userService.findById(winnerId).orElse(null);
            if (winner != null) {
                resultBuilder.winnerName(winner.getFirstName() + " " + winner.getLastName());
                resultBuilder.winner(session.getWhitePlayer().getUserId().equals(winnerId) ? PieceColor.WHITE : PieceColor.BLACK);
            }
        } else {
            // Check for draw
            boolean isDraw = chessGameService.isDraw(gameId, session.getGameState().getCurrentTurn());
            if (isDraw) {
                TieResolutionOption tieOption = chessGameService.selectTieResolutionOption(session.getGameMode());
                if (tieOption != null) {
                    // For simplicity, assume tie resolution picks a winner randomly for MULTIPLAYER_RPG
                    if (session.getGameMode() == GameMode.MULTIPLAYER_RPG) {
                        List<String> playerIds = session.getPlayerIds();
                        winnerId = playerIds.get(new Random().nextInt(playerIds.size()));
                        resultBuilder.winnerid(winnerId)
                                .winner(session.getWhitePlayer().getUserId().equals(winnerId) ? PieceColor.WHITE : PieceColor.BLACK)
                                .winnerName(userService.findById(winnerId)
                                        .map(u -> u.getFirstName() + " " + u.getLastName())
                                        .orElse("Unknown"))
                                .gameEndReason(GameEndReason.TIE_RESOLVED)
                                .tieResolutionOption(tieOption);
                    } else {
                        // For single-player RPG or Enhanced RPG, no winner
                        resultBuilder.gameEndReason(GameEndReason.DRAW)
                                .tieResolutionOption(tieOption);
                    }
                } else {
                    resultBuilder.gameEndReason(GameEndReason.DRAW);
                }
            } else {
                resultBuilder.gameEndReason(GameEndReason.DRAW);
            }
        }

        GameResult result = resultBuilder.build();
        GameSession updatedSession = gameSessionRepository.save(session);
        gameHistoryService.updateGameHistory(session.getGameHistoryId(), result, LocalDateTime.now());

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