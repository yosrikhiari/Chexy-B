package org.example.chessmystic.Service.implementation.GameRelated;

import org.example.chessmystic.Models.UIUX.TieResolutionOption;
import org.example.chessmystic.Models.rpg.Rarity;
import org.example.chessmystic.Repository.TieResolutionOptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TieResolutionOptionService implements CommandLineRunner {

    @Autowired
    private TieResolutionOptionRepository repository;

    @Override
    public void run(String... args) throws Exception {
        if (repository.count() == 0) {
            repository.save(TieResolutionOption.builder()
                    .id("gambit")
                    .name("Gambit of the Phoenix")
                    .flavor("The ancient Phoenix offers rebirth—at a cost.")
                    .description("Sacrifice a piece to turn the tie into victory. The piece returns weaker next round.")
                    .weight(4)
                    .range("1-4")
                    .icon("flame")
                    .rarity(Rarity.COMMON)
                    .build());

            repository.save(TieResolutionOption.builder()
                    .id("puzzle")
                    .name("Puzzle Gauntlet")
                    .flavor("The Lich King freezes time—solve his riddle to proceed!")
                    .description("Solve a chess puzzle within 15 seconds to claim victory.")
                    .weight(2)
                    .range("5-6")
                    .icon("skull")
                    .rarity(Rarity.LEGENDARY)
                    .build());

            repository.save(TieResolutionOption.builder()
                    .id("showdown")
                    .name("Army Showdown")
                    .flavor("The armies clash—only the strongest prevail!")
                    .description("Compare total piece values. Highest army strength wins.")
                    .weight(6)
                    .range("7-12")
                    .icon("swords")
                    .rarity(Rarity.COMMON)
                    .build());

            repository.save(TieResolutionOption.builder()
                    .id("oracle")
                    .name("Oracle's Bargain")
                    .flavor("The Oracle demands a tribute for her prophecy...")
                    .description("Draft upgrades and sacrifice one to convert the tie into victory.")
                    .weight(3)
                    .range("13-15")
                    .icon("eye")
                    .rarity(Rarity.RARE)
                    .build());

            repository.save(TieResolutionOption.builder()
                    .id("blood")
                    .name("Blood Chess")
                    .flavor("The board thirsts—feed it or perish!")
                    .description("Lose random pieces until one side cannot move.")
                    .weight(5)
                    .range("16-20")
                    .icon("crown")
                    .rarity(Rarity.EPIC)
                    .build());
        }
    }

    public List<TieResolutionOption> getAllOptions() {
        return repository.findAll();
    }



}