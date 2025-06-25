package org.example.chessmystic.Repository;

import org.example.chessmystic.Models.Transactions.PlayerPurchaseHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerPurchaseHistoryRepository extends MongoRepository<PlayerPurchaseHistory, String> {
    List<PlayerPurchaseHistory> findByUserId(String userId);
    List<PlayerPurchaseHistory> findByGameSessionId(String gameSessionId);
}