package org.example.chessmystic.Repository;

import org.example.chessmystic.Models.rpg.BoardEffect;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardEffectRepository extends MongoRepository<BoardEffect, String> {
}

