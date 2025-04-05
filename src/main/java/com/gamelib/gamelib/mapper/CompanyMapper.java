package com.gamelib.gamelib.mapper;

import com.gamelib.gamelib.dto.CompanyDto;
import com.gamelib.gamelib.model.Company;
import java.util.List;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Component;

@Component
public class CompanyMapper {

    private final GameMapper gameMapper;

    public CompanyMapper(GameMapper gameMapper) {
        this.gameMapper = gameMapper;
    }

    public CompanyDto toDto(Company company) {
        CompanyDto dto = new CompanyDto();
        dto.setId(company.getId());
        dto.setName(company.getName());
        dto.setDescription(company.getDescription());
        dto.setFoundedYear(company.getFoundedYear());
        dto.setWebsite(company.getWebsite());

        // Обработка игр - проверяем, инициализирована ли коллекция
        if (company.getGames() != null && Hibernate.isInitialized(company.getGames())) {
            dto.setGames(company.getGames().stream()
                    .map(gameMapper::toDto)
                    .toList());
        }

        return dto;
    }

    public Company toEntity(CompanyDto dto) {
        Company company = new Company();
        company.setId(dto.getId());
        company.setName(dto.getName());
        company.setDescription(dto.getDescription());
        company.setFoundedYear(dto.getFoundedYear());
        company.setWebsite(dto.getWebsite());

        // Связи с играми устанавливаются через сервис

        return company;
    }

    public List<CompanyDto> toDtoList(List<Company> companies) {
        return companies.stream()
                .map(this::toDto)
                .toList();
    }
}