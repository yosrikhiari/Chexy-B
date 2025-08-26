package org.example.chessmystic.Config.websocket;

public class RabbitMQConfig {

    public static final String CHESS_EXCHANGE = "chess.exchange";
    public static final String CHESS_DEAD_LETTER_EXCHANGE = "chess.dlx";


    public static final String GAME_UPDATE_QUEUE = "game.update";
    public static final String MATCHMAKING_QUEUE = "matchmaking.queue";
    public static final String PLAYER_MOVES = "player.moves";
    public static final String TIMER_UPDATE = "timer.update";

    public static final String GAME_UPDATE_RK = "game.update.rk"; // RK stands for routing key
    public static final String MATCHMAKING_RK = "matchmaking.rk";
    public static final String PLAYER_MOVES_RK = "player.moves.rk";
    public static final String TIMER_UPDATE_RK = "timer.update.rk";

    
}
