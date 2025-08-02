package org.example.chessmystic.Service.implementation.GameRelated;

import org.example.chessmystic.Models.Tracking.GameHistory;
import org.example.chessmystic.Service.implementation.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PointsAttributionService {
    private static final int BASE_POINTS = 13;
    private static final int MAX_POINTS = 20;
    private static final int MIN_POINTS = 13;

    @Autowired
    private GameHistoryService gameHistoryService;

    @Autowired
    private UserService userService;

    public PointsCalculation calculatePoints(String winnerId, String loserId, boolean isDraw) {
        if (isDraw) {
            return new PointsCalculation(MIN_POINTS, MIN_POINTS, 0, 0);
        }

        List<GameHistory> winnerHistory = gameHistoryService.findRankedGamesByUser(winnerId);
        List<GameHistory> loserHistory = gameHistoryService.findRankedGamesByUser(loserId);

        int winnerStreak = calculateWinStreak(winnerHistory, winnerId);
        int loserStreak = calculateLossStreak(loserHistory, loserId);

        int winnerPoints = calculateWinnerPoints(winnerStreak);
        int loserPoints = calculateLoserPoints(loserStreak);

        return new PointsCalculation(winnerPoints, -loserPoints, winnerStreak, loserStreak);
    }

    public void updatePoints(String winnerId, String loserId, boolean isDraw) {
        PointsCalculation calc = calculatePoints(winnerId, loserId, isDraw);
        if (!isDraw && winnerId != null && loserId != null) {
            userService.updateUserPoints(winnerId, calc.getWinnerPoints());
            userService.updateUserPoints(loserId, calc.getLoserPoints());
        } else if (isDraw && winnerId != null && loserId != null) {
            userService.updateUserPoints(winnerId, calc.getWinnerPoints());
            userService.updateUserPoints(loserId, calc.getLoserPoints());
        }
    }

    private int calculateWinStreak(List<GameHistory> history, String playerId) {
        int streak = 0;
        for (GameHistory game : history.stream()
                .filter(g -> g.getResult() != null && g.getEndTime() != null)
                .sorted((a, b) -> b.getEndTime().compareTo(a.getEndTime()))
                .toList()) {
            if (game.getResult().getWinnerid().equals(playerId) && !"draw".equals(game.getResult().getWinner())) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    private int calculateLossStreak(List<GameHistory> history, String playerId) {
        int streak = 0;
        for (GameHistory game : history.stream()
                .filter(g -> g.getResult() != null && g.getEndTime() != null)
                .sorted((a, b) -> b.getEndTime().compareTo(a.getEndTime()))
                .toList()) {
            if (!game.getResult().getWinnerid().equals(playerId) && !"draw".equals(game.getResult().getWinner())) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    private int calculateWinnerPoints(int winStreak) {
        int streakBonus = Math.min(winStreak, MAX_POINTS - BASE_POINTS);
        return Math.min(BASE_POINTS + streakBonus, MAX_POINTS);
    }

    private int calculateLoserPoints(int lossStreak) {
        int streakPenalty = Math.min(lossStreak, MAX_POINTS - BASE_POINTS);
        return Math.min(BASE_POINTS + streakPenalty, MAX_POINTS);
    }
}

class PointsCalculation {
    private final int winnerPoints;
    private final int loserPoints;
    private final int winnerStreak;
    private final int loserStreak;

    public PointsCalculation(int winnerPoints, int loserPoints, int winnerStreak, int loserStreak) {
        this.winnerPoints = winnerPoints;
        this.loserPoints = loserPoints;
        this.winnerStreak = winnerStreak;
        this.loserStreak = loserStreak;
    }

    public int getWinnerPoints() { return winnerPoints; }
    public int getLoserPoints() { return loserPoints; }
    public int getWinnerStreak() { return winnerStreak; }
    public int getLoserStreak() { return loserStreak; }
}