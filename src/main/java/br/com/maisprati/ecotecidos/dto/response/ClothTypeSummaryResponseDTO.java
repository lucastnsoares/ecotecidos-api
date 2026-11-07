package br.com.maisprati.ecotecidos.dto.response;

import br.com.maisprati.ecotecidos.model.entity.ClothType;

public record ClothTypeSummaryResponseDTO(
        Long id,
        String name
) {
    public ClothTypeSummaryResponseDTO(ClothType clothType) {
        this(clothType.getId(), clothType.getName());
    }
}