package com.gamelib.gamelib.repository;

import com.gamelib.gamelib.model.Game;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface GameRepository extends JpaRepository<Game, Long>, JpaSpecificationExecutor<Game> {
    @EntityGraph(value = "game-with-companies", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT g FROM Game g WHERE g.id = :id")
    Optional<Game> findByIdWithCompaniesGraph(@Param("id") Long id);

    @EntityGraph(value = "game-with-companies", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT g FROM Game g WHERE LOWER(g.title) LIKE LOWER(CONCAT('%', :title, '%'))")
    List<Game> findByTitleContainingIgnoreCase(@Param("title") String title);

    @EntityGraph(value = "game-with-reviews", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT g FROM Game g") // Явно указываем JPQL-запрос
    List<Game> findAllWithReviews();

    boolean existsByTitle(String title);
}