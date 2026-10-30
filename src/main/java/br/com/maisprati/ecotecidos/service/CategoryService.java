package br.com.maisprati.ecotecidos.service;

import jakarta.persistence.EntityNotFoundException;
import br.com.maisprati.ecotecidos.dto.request.CategoryRequestDTO;
import br.com.maisprati.ecotecidos.dto.response.CategoryResponseDTO;
import br.com.maisprati.ecotecidos.model.entity.Category;
import br.com.maisprati.ecotecidos.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final PostService postService;


    public CategoryResponseDTO createCategory(CategoryRequestDTO dto) {
        if (categoryRepository.existsByName(dto.name())) {
            throw new IllegalArgumentException("Categoria já existente");
        }
        Category category = new Category();
        category.setName(dto.name());
        categoryRepository.save(category);
        return new CategoryResponseDTO(category);
    }

    public Page<CategoryResponseDTO> findAll(Pageable pageable) {
        return categoryRepository.findAll(pageable)
                .map(CategoryResponseDTO::new);
    }

    public CategoryResponseDTO findCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria inexistente"));
        return new CategoryResponseDTO(category);
    }

    public CategoryResponseDTO updateCategory(Long id, CategoryRequestDTO request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria inexistente"));
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(request.name().trim(), id)) {
            throw new IllegalArgumentException("Categoria já existente");
        }
        category.setName(request.name().trim());
        categoryRepository.save(category);
        return new CategoryResponseDTO(category);
    }

    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria inexistente"));

        if (postService.existsByCategory(category)) {
            throw new IllegalArgumentException("Não é possível excluir a categoria, pois existem posts associados a ela");
        }
        categoryRepository.delete(category);
    }
}
