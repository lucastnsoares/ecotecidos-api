package br.com.maisprati.ecotecidos.dto.response;

import br.com.maisprati.ecotecidos.model.entity.CollectionPoint;
import br.com.maisprati.ecotecidos.model.enums.CollectionPointStatus;

public record AssociationResponseDTO(
    Long id,
    String name,
    CollectionPointStatus status
) {

    public AssociationResponseDTO(CollectionPoint entity) {
        this(
            entity.getId(),
            entity.getName(),
            entity.getStatus()
        );
    }
}
