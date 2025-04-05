package com.gamelib.gamelib.mapper;

import com.gamelib.gamelib.dto.GameDto;
import com.gamelib.gamelib.dto.ReviewDto;
import com.gamelib.gamelib.model.Company;
import com.gamelib.gamelib.model.Game;
import com.gamelib.gamelib.model.Review;
import com.gamelib.gamelib.service.CompanyService;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Component;

@Component
public class GameMapper {

    private final ReviewMapper reviewMapper;
    private final CompanyService companyService;

    public GameMapper(ReviewMapper reviewMapper, CompanyService companyService) {
        this.reviewMapper = reviewMapper;
        this.companyService = companyService;
    }

    public GameDto toDto(Game game) {
        GameDto dto = new GameDto();
        dto.setId(game.getId());
        dto.setTitle(game.getTitle());
        dto.setDescription(game.getDescription());
        dto.setReleaseDate(game.getReleaseDate());
        dto.setGenre(game.getGenre());

        if (game.getCompanies() != null && Hibernate.isInitialized(game.getCompanies())) {
            dto.setCompanyIds(game.getCompanies().stream()
                    .map(Company::getId)
                    .collect(Collectors.toSet()));
        }

        if (game.getReviews() != null && Hibernate.isInitialized(game.getReviews())) {
            dto.setReviews(game.getReviews().stream()
                    .map(reviewMapper::toDto)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    public Game toEntity(GameDto dto) {
        Game game = new Game();
        game.setId(dto.getId());
        game.setTitle(dto.getTitle());
        game.setDescription(dto.getDescription());
        game.setReleaseDate(dto.getReleaseDate());
        game.setGenre(dto.getGenre());

        if (dto.getCompanyIds() != null) {
            Set<Company> companies = dto.getCompanyIds().stream()
                    .map(companyService::getCompanyByIdOrThrow)
                    .collect(Collectors.toSet());
            game.setCompanies(companies);
        }

        // Преобразуем ReviewDto → Review и устанавливаем game
        if (dto.getReviews() != null) {
            List<Review> reviews = dto.getReviews().stream()
                    .map(reviewDto -> {
                        Review review = reviewMapper.toEntity(reviewDto);
                        review.setGame(game);
                        return review;
                    })
                    .collect(Collectors.toList());
            game.setReviews(reviews);
        }

        return game;
    }

    public List<GameDto> toDtoList(List<Game> games) {
        return games.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}
