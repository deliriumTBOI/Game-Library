package com.gamelib.gamelib.service.impl;

import com.gamelib.gamelib.cache.LruCache;
import com.gamelib.gamelib.exception.ResourceAlreadyExistsException;
import com.gamelib.gamelib.exception.ResourceNotFoundException;
import com.gamelib.gamelib.model.Company;
import com.gamelib.gamelib.model.Game;
import com.gamelib.gamelib.model.Review;
import com.gamelib.gamelib.repository.CompanyRepository;
import com.gamelib.gamelib.repository.GameRepository;
import com.gamelib.gamelib.service.GameService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class GameServiceImpl implements GameService {
    private static final String GAME_NOT_FOUND_WITH_ID = "Game not found with id: ";
    private static final String CACHE_MIN_RATING_PREFIX = "games:min_rating:";
    private static final String CACHE_RATING_RANGE_PREFIX = "games:rating_range:";

    private final GameRepository gameRepository;
    private final CompanyRepository companyRepository;
    private final LruCache<String, List<Game>> gameCache = new LruCache<>(50000,
            100, "GameCache");

    public GameServiceImpl(GameRepository gameRepository, CompanyRepository companyRepository) {
        this.gameRepository = gameRepository;
        this.companyRepository = companyRepository;
    }

    @Override
    @Transactional
    public Game createGame(Game game) {
        if (gameRepository.existsByTitle(game.getTitle())) {
            throw new ResourceAlreadyExistsException("Game is already exist");
        }
        Game createdGame = gameRepository.save(game);
        clearQueryCaches();
        return createdGame;
    }

    @Override
    public List<Game> getAllGames() {
        return gameRepository.findAll();
    }

    @Override
    public List<Game> getGamesByTitle(String title) {
        return gameRepository.findByTitleContainingIgnoreCase(title);
    }

    @Override
    public Game getGameById(Long id) {
        return gameRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(GAME_NOT_FOUND_WITH_ID + id));
    }

    @Override
    @Transactional
    public Game updateGame(Long id, Game updatedGame) {
        Game existingGame = gameRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(GAME_NOT_FOUND_WITH_ID + id));

        if (!existingGame.getTitle().equals(updatedGame.getTitle())
                && gameRepository.existsByTitle(updatedGame.getTitle())) {
            throw new ResourceAlreadyExistsException("Game with title "
                    + updatedGame.getTitle() + " already exists");
        }

        existingGame.setTitle(updatedGame.getTitle());
        existingGame.setDescription(updatedGame.getDescription());
        existingGame.setReleaseDate(updatedGame.getReleaseDate());
        existingGame.setGenre(updatedGame.getGenre());

        if (updatedGame.getCompanies() != null) {
            existingGame.getCompanies().clear();
            existingGame.getCompanies().addAll(updatedGame.getCompanies());
        }

        if (updatedGame.getReviews() != null) {
            existingGame.getReviews().clear();
            for (Review review : updatedGame.getReviews()) {
                review.setGame(existingGame);
                existingGame.getReviews().add(review);
            }
        }

        Game result = gameRepository.save(existingGame);
        clearQueryCaches();
        return result;
    }

    @Override
    @Transactional
    public Game patchGame(Long id, Game partialGame) {
        Game existingGame = gameRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(GAME_NOT_FOUND_WITH_ID + id));

        if (StringUtils.hasText(partialGame.getTitle())) {
            if (!existingGame.getTitle().equals(partialGame.getTitle())
                    && gameRepository.existsByTitle(partialGame.getTitle())) {
                throw new ResourceAlreadyExistsException("Game with title "
                        + partialGame.getTitle() + " already exists");
            }
            existingGame.setTitle(partialGame.getTitle());
        }

        if (StringUtils.hasText(partialGame.getDescription())) {
            existingGame.setDescription(partialGame.getDescription());
        }

        if (partialGame.getReleaseDate() != null) {
            existingGame.setReleaseDate(partialGame.getReleaseDate());
        }

        if (StringUtils.hasText(partialGame.getGenre())) {
            existingGame.setGenre(partialGame.getGenre());
        }

        if (partialGame.getCompanies() != null) {
            existingGame.getCompanies().clear();
            existingGame.getCompanies().addAll(partialGame.getCompanies());
        }

        if (partialGame.getReviews() != null) {
            existingGame.getReviews().clear();
            for (Review review : partialGame.getReviews()) {
                review.setGame(existingGame);
                existingGame.getReviews().add(review);
            }
        }

        Game result = gameRepository.save(existingGame);
        clearQueryCaches();
        return result;
    }

    @Override
    @Transactional
    public boolean deleteGame(Long id) {
        if (gameRepository.existsById(id)) {
            gameRepository.deleteById(id);
            clearQueryCaches();
            return true;
        }
        return false;
    }


    @Override
    @Transactional
    public Game addCompanyToGame(Long gameId, Long companyId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException(GAME_NOT_FOUND_WITH_ID
                        + gameId));

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: "
                        + companyId));

        game.getCompanies().add(company);
        return gameRepository.save(game);
    }

    @Override
    @Transactional
    public boolean removeCompanyFromGame(Long gameId, Long companyId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException(GAME_NOT_FOUND_WITH_ID
                        + gameId));

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: "
                        + companyId));

        boolean removed = game.getCompanies().remove(company);
        if (removed) {
            gameRepository.save(game);
        }

        return removed;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Game> getGamesByMinimumRating(Integer minRating) {
        String cacheKey = CACHE_MIN_RATING_PREFIX + minRating;

        // Проверяем наличие результатов в кэше
        if (gameCache.containsKey(cacheKey)) {
            return gameCache.get(cacheKey);
        }

        // Выполняем запрос, если данных нет в кэше
        List<Game> games = gameRepository.findGamesByMinimumRating(minRating);

        // Сохраняем результат в кэш
        gameCache.put(cacheKey, games);

        return games;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Game> getGamesByRatingRange(Integer minRating, Integer maxRating) {
        String cacheKey = CACHE_RATING_RANGE_PREFIX + minRating + ":" + maxRating;

        // Проверяем наличие результатов в кэше
        if (gameCache.containsKey(cacheKey)) {
            return gameCache.get(cacheKey);
        }

        // Выполняем запрос, если данных нет в кэше
        List<Game> games = gameRepository.findGamesByRatingRange(minRating, maxRating);

        // Сохраняем результат в кэш
        gameCache.put(cacheKey, games);

        return games;
    }

    @Override
    public void clearCache() {
        gameCache.clear();
    }

    /**
     * Очищает только кеши для запросов, связанных с рейтингами
     */
    private void clearQueryCaches() {
        // Более селективный подход: чистим только кеши запросов,
        // так как изменения в играх могут повлиять на результаты запросов по рейтингам
        gameCache.clear();
    }
}