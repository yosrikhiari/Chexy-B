package org.example.chessmystic.Service.interfaces.GameRelated;

import org.example.chessmystic.Models.Mechanics.RPGGameState;
import org.example.chessmystic.Models.rpg.BoardEffect;
import org.example.chessmystic.Models.Transactions.RPGModifier;
import org.example.chessmystic.Models.Transactions.EquipmentItem;
import org.example.chessmystic.Models.rpg.RPGPiece;
import org.example.chessmystic.Models.rpg.SpecializationType;
import org.example.chessmystic.Models.rpg.AbilityId;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface IRPGGameService {

	@Transactional
	RPGGameState createRPGGame(String userId, String gameSessionId, boolean isMultiplayer);

	Optional<RPGGameState> findById(String gameId);

	RPGGameState findByGameSessionId(String gameSessionId);

	@Transactional
	RPGGameState progressToNextRound(String gameId);

	@Transactional
	RPGGameState addPieceToArmy(String gameId, RPGPiece piece, String playerId);

	@Transactional
	RPGGameState addModifier(String gameId, RPGModifier modifier, String playerId);

	@Transactional
	RPGGameState addBoardEffect(String gameId, BoardEffect effect, String playerId);

	@Transactional
	RPGGameState updateScore(String gameId, int scoreToAdd, String playerId);

	@Transactional
	RPGGameState updateCoins(String gameId, int coinsToAdd, String playerId);

	@Transactional
	RPGGameState endGame(String gameId, boolean victory);

	List<RPGGameState> findActiveGamesByUser(String userId);

	//    List<RPGGameState> findGamesByUser(String userId);

	@Transactional
	RPGGameState purchaseShopItem(String gameId, String shopItemId, String playerId);

	// NEW: MVP RPG operations
	@Transactional
	RPGGameState chooseSpecialization(String gameId, String pieceId, SpecializationType specialization, String playerId);

	@Transactional
	RPGGameState equipItem(String gameId, String pieceId, EquipmentItem item, String playerId);

	@Transactional
	RPGGameState resolveTie(String gameId, String playerId, String choice);

	@Transactional
	RPGGameState spawnQuests(String gameId, String playerId);

    // Ability activation
    @Transactional
    RPGGameState activateAbility(String gameId, String pieceId, AbilityId abilityId, String targetPieceId, String playerId);

	@Transactional
	RPGGameState acceptQuest(String gameId, String questId, String playerId);

	@Transactional
	RPGGameState completeQuest(String gameId, String questId, String playerId);

	@Transactional
	RPGGameState awardXp(String gameId, String pieceId, int xp, String playerId);

	// NEW: Special mechanics
	@Transactional
	RPGGameState converseWithDreamer(String gameId, String pieceId, String prompt, String playerId);

	@Transactional
	RPGGameState preacherControl(String gameId, String preacherPieceId, String targetEnemyPieceId, String playerId);

	@Transactional
	RPGGameState triggerStatueEvent(String gameId, String playerId);

	@Transactional
	RPGGameState setMusicCue(String gameId, String cueId, String playerId);

	@Transactional
	RPGGameState updateWeaknesses(String gameId, String pieceId, java.util.Set<org.example.chessmystic.Models.rpg.WeaknessType> weaknesses, String playerId);
}