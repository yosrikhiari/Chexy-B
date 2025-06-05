package org.example.chessmystic.Service.interfaces.GameRelated;

import org.example.chessmystic.Models.rpg.BoardEffect;
import org.example.chessmystic.Models.Interactions.CombatResult;
import org.example.chessmystic.Models.rpg.EnhancedRPGPiece;
import org.example.chessmystic.Models.Mechanics.RPGBoss;
import org.springframework.transaction.annotation.Transactional;

public interface IEnhancedRPGService {

    @Transactional
    CombatResult resolveCombat(EnhancedRPGPiece attacker, EnhancedRPGPiece defender, String gameId);

    void applyBoardEffect(String gameId, BoardEffect effect);
    void handleBossEncounter(String gameId, RPGBoss boss);
}

