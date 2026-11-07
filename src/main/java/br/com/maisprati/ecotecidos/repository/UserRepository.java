package br.com.maisprati.ecotecidos.repository;

import br.com.maisprati.ecotecidos.model.entity.CollectionPoint;
import br.com.maisprati.ecotecidos.model.entity.Post;
import br.com.maisprati.ecotecidos.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @EntityGraph(attributePaths = "role")
    Optional<User> findByEmail(String email);
    Optional<User> findByDocument(String document);
    boolean existsByEmail(String email);

    boolean existsByDocument(String document);

    @EntityGraph(attributePaths = "role")
    Optional<User> findById(Long id);

    @EntityGraph(attributePaths = "role")
    Page<User> findAll(Pageable pageable);

    @Query(value = "SELECT p FROM User u JOIN u.favoritePosts p WHERE u.email = :email",
            countQuery = "SELECT COUNT(p) FROM User u JOIN u.favoritePosts p WHERE u.email = :email")
    Page<Post> findFavoritePostsByUserEmail(@Param("email") String email, Pageable pageable);

    @Query("SELECT COUNT(p) FROM User u JOIN u.favoritePosts p WHERE u.email = :email")
    Long countFavoritePosts(String email);

    @Query("SELECT c FROM User u JOIN u.favoriteCollectionPoints c WHERE u.email = :email")
    Page<CollectionPoint> findFavoriteCollectionPointsByEmail(String email, Pageable pageable);

    @Query("SELECT COUNT(c) FROM User u JOIN u.favoriteCollectionPoints c WHERE u.email = :email")
    Long countFavoriteCollectionPoints(String email);
}
