package br.com.maisprati.ecotecidos.dto.response;

import br.com.maisprati.ecotecidos.model.entity.Category;

public record CategoryResponseDTO(
        Long id,
        String name
) {
    public CategoryResponseDTO(Category category) {
        this(
                category.getId(),
                category.getName()
        );
    }
}
