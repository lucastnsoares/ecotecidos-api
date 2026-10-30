package br.com.maisprati.ecotecidos.repository;

import br.com.maisprati.ecotecidos.model.entity.CollectionPointPendingUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CollectionPointPendingUpdateRepository extends JpaRepository<CollectionPointPendingUpdate, Long> {
    Optional<CollectionPointPendingUpdate> findByCollectionPointId(Long collectionPointId);
    void deleteByCollectionPointId(Long collectionPointId);
}