package org.example.chessmystic.Repository;

import org.example.chessmystic.Models.AISystem.AIStrategy;
import org.example.chessmystic.Models.AISystem.EnemyArmyConfig;
import org.example.chessmystic.Models.rpg.ArmyCapacity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArmyCapacityRepository extends MongoRepository<ArmyCapacity, String> {
}