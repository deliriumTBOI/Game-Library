package com.gamelib.gamelib.service.impl;

import com.gamelib.gamelib.cache.LruCache;
import com.gamelib.gamelib.exception.ResourceAlreadyExistsException;
import com.gamelib.gamelib.exception.ResourceNotFoundException;
import com.gamelib.gamelib.model.Company;
import com.gamelib.gamelib.model.Game;
import com.gamelib.gamelib.repository.CompanyRepository;
import com.gamelib.gamelib.repository.GameRepository;
import com.gamelib.gamelib.service.CompanyService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyServiceImpl implements CompanyService {
    private static final String COMPANY_NOT_FOUND_WITH_ID = "Company not found with id: ";
    private static final String GAME_NOT_FOUND_WITH_ID = "Game not found with id: ";
    private static final String CACHE_COMPANY_PREFIX = "company:id:";

    private final CompanyRepository companyRepository;
    private final GameRepository gameRepository;
    private final LruCache<String, Company> companyCache = new LruCache<>(5,
            100, "CompanyCache");

    public CompanyServiceImpl(CompanyRepository companyRepository, GameRepository gameRepository) {
        this.companyRepository = companyRepository;
        this.gameRepository = gameRepository;
    }

    @Override
    @Transactional
    public Company createCompany(Company company) {
        if (companyRepository.existsByName(company.getName())) {
            throw new ResourceAlreadyExistsException("Company is already exist");
        }
        return companyRepository.save(company);
    }

    @Override
    @Transactional
    public List<Company> createCompanies(List<Company> companies) {
        Set<String> incomingNames = companies.stream()
                .map(Company::getName)
                .collect(Collectors.toSet());

        List<String> existingNames = companyRepository.findByNameIn(incomingNames)
                .stream()
                .map(Company::getName)
                .toList();

        if (!existingNames.isEmpty()) {
            throw new ResourceAlreadyExistsException(
                    "Companies with names: " + String.join(", ", existingNames) + " already exist"
            );
        }

        return companyRepository.saveAll(companies);
    }


    @Override
    public Optional<Company> getCompanyById(Long id) {
        String cacheKey = CACHE_COMPANY_PREFIX + id;

        // Проверяем наличие в кэше
        if (companyCache.containsKey(cacheKey)) {
            Company cachedCompany = companyCache.get(cacheKey);
            return Optional.ofNullable(cachedCompany);
        }

        // Если нет в кэше, получаем из репозитория
        Optional<Company> companyOpt = companyRepository.findById(id);

        // Если компания найдена, кешируем её
        companyOpt.ifPresent(company -> companyCache.put(cacheKey, company));

        return companyOpt;
    }

    @Override
    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    @Override
    public List<Company> getCompaniesByName(String name) {
        return companyRepository.findByNameContainingIgnoreCase(name);
    }

    @Override
    @Transactional
    public Company updateCompany(Long id, Company updatedCompany) {
        Company existingCompany = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND_WITH_ID + id));

        if (!existingCompany.getName().equals(updatedCompany.getName())
                && companyRepository.existsByName(updatedCompany.getName())) {
            throw new ResourceAlreadyExistsException("Company with name "
                    + updatedCompany.getName() + " already exists");
        }

        existingCompany.setName(updatedCompany.getName());
        existingCompany.setDescription(updatedCompany.getDescription());
        existingCompany.setFoundedYear(updatedCompany.getFoundedYear());
        existingCompany.setWebsite(updatedCompany.getWebsite());

        Company result = companyRepository.save(existingCompany);

        // Обновляем кэш
        String cacheKey = CACHE_COMPANY_PREFIX + id;
        companyCache.put(cacheKey, result);

        return result;
    }

    @Override
    @Transactional
    public boolean deleteCompany(Long id) {
        if (companyRepository.existsById(id)) {
            companyRepository.deleteById(id);

            // Удаляем из кэша
            String cacheKey = CACHE_COMPANY_PREFIX + id;
            companyCache.remove(cacheKey);

            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public Company addGameToCompany(Long companyId, Long gameId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND_WITH_ID
                        + companyId));

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException(GAME_NOT_FOUND_WITH_ID + gameId));

        company.getGames().add(game);

        Company result = companyRepository.save(company);

        // Обновляем кэш
        String cacheKey = CACHE_COMPANY_PREFIX + companyId;
        companyCache.put(cacheKey, result);

        return result;
    }

    @Override
    public Company getCompanyByNameOrThrow(String name) {
        return companyRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with name: "
                        + name));
    }

    @Override
    @Transactional
    public boolean removeGameFromCompany(Long companyId, Long gameId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND_WITH_ID
                        + companyId));

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException(GAME_NOT_FOUND_WITH_ID + gameId));

        boolean removed = company.getGames().remove(game);
        if (removed) {
            Company result = companyRepository.save(company);

            // Обновляем кэш
            String cacheKey = CACHE_COMPANY_PREFIX + companyId;
            companyCache.put(cacheKey, result);
        }

        return removed;
    }
}