package org.example.chessmystic.Service.interfaces.GameRelated;

import org.example.chessmystic.Models.rpg.BoardEffect;
import org.example.chessmystic.Models.Interactions.CombatResult;
import org.example.chessmystic.Models.rpg.EnhancedRPGPiece;
import org.example.chessmystic.Models.Mechanics.RPGBoss;
import org.springframework.transaction.annotation.Transactional;

public interface IEnhancedRPGService {


    @Transactional
    CombatResult resolveCombat(EnhancedRPGPiece attacker, EnhancedRPGPiece defender, String gameId, String playerId);

    @Transactional
    void applyBoardEffect(String gameId, BoardEffect effect, String playerId);

    @Transactional
    void handleBossEncounter(String gameId, RPGBoss boss, String playerId);
}

