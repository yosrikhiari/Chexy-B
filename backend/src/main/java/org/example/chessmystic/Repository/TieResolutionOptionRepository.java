package org.example.chessmystic.Repository;

import org.example.chessmystic.Models.UIUX.TieResolutionOption;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TieResolutionOptionRepository extends MongoRepository<TieResolutionOption, String> {
}