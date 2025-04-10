package com.gamelib.gamelib.controller;

import com.gamelib.gamelib.dto.GameDto;
import com.gamelib.gamelib.mapper.GameMapper;
import com.gamelib.gamelib.model.Game;
import com.gamelib.gamelib.service.GameService;
import java.util.List;
import org.hibernate.Hibernate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/games")
public class GameController {
    private final GameService gameService;
    private final GameMapper gameMapper;

    public GameController(GameService gameService, GameMapper gameMapper) {
        this.gameService = gameService;
        this.gameMapper = gameMapper;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<GameDto>> getGames(@RequestParam(required = false) String title) {
        List<Game> games;

        if (title != null && !title.isEmpty()) {
            games = gameService.getGamesByTitle(title);
        } else {
            games = gameService.getAllGames();
        }

        games.forEach(game -> {
            if (game.getCompanies() != null) {
                Hibernate.initialize(game.getCompanies());
            }
            if (game.getReviews() != null) {
                Hibernate.initialize(game.getReviews());
            }
        });
        return ResponseEntity.ok(gameMapper.toDtoList(games));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<GameDto> getGameById(@PathVariable Long id) {
        Game game = gameService.getGameById(id);
        if (game != null) {

            if (game.getCompanies() != null) {
                Hibernate.initialize(game.getCompanies());
            }
            if (game.getReviews() != null) {
                Hibernate.initialize(game.getReviews());
            }
            return ResponseEntity.ok(gameMapper.toDto(game));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<GameDto> createGame(@RequestBody GameDto gameDto) {
        try {
            Game game = gameMapper.toEntity(gameDto);
            Game createdGame = gameService.createGame(game);
            return new ResponseEntity<>(gameMapper.toDto(createdGame), HttpStatus.CREATED);
        } catch (RuntimeException e) {
            if ("Game is already exist".equals(e.getMessage())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
            } else {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Server Error: " + e.getMessage());
            }
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<GameDto> updateGame(@PathVariable Long id, @RequestBody GameDto gameDto) {
        Game game = gameMapper.toEntity(gameDto);
        Game updatedGame = gameService.updateGame(id, game);
        return updatedGame != null ? ResponseEntity.ok(gameMapper.toDto(updatedGame)) :
                ResponseEntity.notFound().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<GameDto> patchGame(@PathVariable Long id, @RequestBody GameDto gameDto) {
        Game game = gameMapper.toEntity(gameDto);
        Game updatedGame = gameService.patchGame(id, game);
        if (updatedGame != null) {
            return new ResponseEntity<>(gameMapper.toDto(updatedGame), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGame(@PathVariable Long id) {
        boolean isDeleted = gameService.deleteGame(id);
        return isDeleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/{gameId}/companies/{companyId}")
    public ResponseEntity<GameDto> addCompanyToGame(@PathVariable Long gameId,
                                                    @PathVariable Long companyId) {
        Game updatedGame = gameService.addCompanyToGame(gameId, companyId);
        if (updatedGame != null) {
            return ResponseEntity.ok(gameMapper.toDto(updatedGame));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/by-rating")
    @Transactional(readOnly = true)
    public ResponseEntity<List<GameDto>> getGamesByRating(
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) Integer maxRating) {

        List<Game> games;

        // Если указаны оба параметра - используем диапазон
        if (minRating != null && maxRating != null) {
            games = gameService.getGamesByRatingRange(minRating, maxRating);
        } else if (minRating != null) {
            games = gameService.getGamesByMinimumRating(minRating);
        } else {
            games = gameService.getAllGames();
        }

        // Инициализируем связанные сущности для корректной сериализации
        games.forEach(game -> {
            if (game.getCompanies() != null) {
                Hibernate.initialize(game.getCompanies());
            }
            if (game.getReviews() != null) {
                Hibernate.initialize(game.getReviews());
            }
        });

        return ResponseEntity.ok(gameMapper.toDtoList(games));
    }

    // Дополнительный метод для очистки кэша (опционально)
    @DeleteMapping("/cache")
    public ResponseEntity<Void> clearCache() {
        gameService.clearCache();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{gameId}/companies/{companyId}")
    public ResponseEntity<Void> removeCompanyFromGame(@PathVariable Long gameId,
                                                      @PathVariable Long companyId) {
        boolean removed = gameService.removeCompanyFromGame(gameId, companyId);
        return removed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}