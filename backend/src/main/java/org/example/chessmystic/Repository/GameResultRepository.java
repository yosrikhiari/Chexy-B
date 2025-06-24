package org.example.chessmystic.Repository;

import org.example.chessmystic.Models.Tracking.GameResult;
import org.example.chessmystic.Models.UserManagement.Friendship;
import org.example.chessmystic.Models.UserManagement.FriendshipStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameResultRepository extends MongoRepository<GameResult, String> {

}
