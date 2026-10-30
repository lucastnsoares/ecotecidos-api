package br.com.maisprati.ecotecidos.service;

import br.com.maisprati.ecotecidos.dto.request.PostCreateRequestDTO;
import br.com.maisprati.ecotecidos.dto.request.PostDeleteRequestDTO;
import br.com.maisprati.ecotecidos.dto.request.PostEditRequestDTO;
import br.com.maisprati.ecotecidos.dto.response.AdminPostResponseDTO;
import br.com.maisprati.ecotecidos.dto.response.AdminPostSummaryResponseDTO;
import br.com.maisprati.ecotecidos.dto.response.PostResponseDTO;
import br.com.maisprati.ecotecidos.dto.response.PostSummaryResponseDTO;
import br.com.maisprati.ecotecidos.model.entity.Category;
import br.com.maisprati.ecotecidos.model.entity.ClothType;
import br.com.maisprati.ecotecidos.model.entity.Post;
import br.com.maisprati.ecotecidos.model.entity.User;
import br.com.maisprati.ecotecidos.repository.CategoryRepository;
import br.com.maisprati.ecotecidos.repository.ClothTypeRepository;
import br.com.maisprati.ecotecidos.repository.PostRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final FileStorageService fileStorageService;
    private final CategoryRepository categoryRepository;
    private final ClothTypeRepository clothTypeRepository;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    @Transactional
    public PostResponseDTO createPost(PostCreateRequestDTO dto, MultipartFile image, User loggedInUser) {
        Category category = categoryRepository.findById(dto.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Categoria informada não foi encontrada."));

        validateAccessibilityAndCoverImage(image, dto.coverImageAlt());

        String slug = generateUniqueSlug(dto.title());

        Set<ClothType> clothTypes = resolveClothTypes(dto.clothTypeIds());

        Set<String> sanitizedTags = sanitizeTags(dto.tags());

        Post post = new Post();
        post.setTitle(dto.title().trim());
        post.setSlug(slug);
        post.setSummary(dto.summary().trim());
        post.setBody(dto.body().trim());
        post.setCategory(category);
        post.setCreatedBy(loggedInUser);
        post.setCoverImageUrl(null);
        post.setCoverImageAlt(dto.coverImageAlt() != null ? dto.coverImageAlt().trim() : null);
        post.setTags(sanitizedTags);
        post.setClothTypes(clothTypes);
        post.setViewCount(0L);
        post.setPublished(dto.isPublished() == null || dto.isPublished());

        Post savedPost = postRepository.save(post);

        if(image != null && !image.isEmpty()) {
            String fileName = fileStorageService.savePostCoverImage(post.getId(), image);
            post.setCoverImageUrl(fileName); 
            savedPost = postRepository.save(post);
        }

        return new PostResponseDTO(savedPost);
    }

    @Transactional(readOnly = true)
    public Page<PostSummaryResponseDTO> findAllByPublishedTrue(Long categoryId, String searchTerm, Set<String> tags, Pageable pageable) {
        String termPattern = (searchTerm != null && !searchTerm.isBlank()) 
            ? "%" + searchTerm.toLowerCase() + "%" 
            : null;

        boolean hasTags = (tags != null && !tags.isEmpty());

        Set<String> tagsParam = hasTags ? tags : Collections.emptySet();
        return postRepository.findAllPublishedWithFilters(categoryId, termPattern, hasTags, tagsParam, pageable)
                .map(PostSummaryResponseDTO::new);
    }

    @Transactional(readOnly = true)
    public PostResponseDTO findPostByIdAndIsPublishedTrue(Long id) {
        Post post = postRepository.findByIdAndIsPublishedTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("Post inexistente ou não disponível."));
        return new PostResponseDTO(post);
    }

    // Apenas ADMIN
    @Transactional(readOnly = true)
    public Page<AdminPostSummaryResponseDTO> findAllPosts(Long categoryId, String searchTerm, Set<String> tags, Pageable pageable) {
        String termPattern = (searchTerm != null && !searchTerm.isBlank()) 
            ? "%" + searchTerm.toLowerCase() + "%" 
            : null;

        boolean hasTags = (tags != null && !tags.isEmpty());

        Set<String> tagsParam = hasTags ? tags : Collections.emptySet();
        return postRepository.findAllWithCategoryFilter(categoryId, termPattern, hasTags, tagsParam, pageable)
                .map(AdminPostSummaryResponseDTO::new);
    }

    // Apenas ADMIN
    @Transactional
    public PostResponseDTO updatePost(Long id, PostEditRequestDTO dto, MultipartFile image) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post inexistente."));

        if (post.getDeletedAt() != null) {
            throw new IllegalArgumentException("Não é possível atualizar um post que foi excluído.");
        }

        if (image != null && !image.isEmpty()) {
            String oldImageFileName = post.getCoverImageUrl();
            if (oldImageFileName != null && !oldImageFileName.isBlank()) {
                fileStorageService.deletePostCoverImage(oldImageFileName);
            }
            String newFileName = fileStorageService.savePostCoverImage(post.getId(), image);
            String targetImageAlt = dto.coverImageAlt() != null ? dto.coverImageAlt().trim() : post.getCoverImageAlt();
            validateAccessibilityAndCoverImage(image, targetImageAlt);
            post.setCoverImageUrl(newFileName);
        }
        
        if (dto.title() != null && !dto.title().isBlank()) {
            post.setTitle(dto.title().trim());
            String newSlug = generateUniqueSlug(dto.title());
            post.setSlug(newSlug);
        }
        if (dto.summary() != null && !dto.summary().isBlank()) {
            post.setSummary(dto.summary().trim());
        }
        if (dto.body() != null && !dto.body().isBlank()) {
            post.setBody(dto.body().trim());
        }

        if (dto.coverImageAlt() != null && !dto.coverImageAlt().isBlank()) {
            post.setCoverImageAlt(dto.coverImageAlt().trim());
        }
        if (dto.categoryId() != null) {
            Category category = categoryRepository.findById(dto.categoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Categoria informada não foi encontrada."));
            post.setCategory(category);
        }
        if (dto.clothTypeIds() != null) {
            Set<ClothType> clothTypes = resolveClothTypes(dto.clothTypeIds());
            post.setClothTypes(clothTypes);
        }
        if (dto.tags() != null) {
            Set<String> sanitizedTags = sanitizeTags(dto.tags());
            post.setTags(sanitizedTags);
        }
        if (dto.isPublished() != null) {
            post.setPublished(dto.isPublished());
        }
        Post updatedPost = postRepository.save(post);
        return new PostResponseDTO(updatedPost);
    }

    // apenas ADMIN
    @Transactional(readOnly = true)
    public AdminPostResponseDTO findPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post inexistente ou não disponível"));
        return new AdminPostResponseDTO(post);
    }

    // apenas ADMIN
    @Transactional
    public void deletePost(Long id, PostDeleteRequestDTO dto, User loggedInUser) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post inexistente."));
        if (post.getDeletedAt() != null) {
            throw new IllegalArgumentException("Post já deletado anteriormente");
        }
        if (dto.justification() != null && !dto.justification().isBlank()) {
            post.setDeletionJustification(dto.justification().trim());
        } else {
            throw new IllegalArgumentException("A justificativa é obrigatória para a exclusão do post.");
        }
        post.setDeletedBy(loggedInUser);
        post.setDeletedAt(Instant.now());
        post.setPublished(false);
        postRepository.save(post);
    }

    private void validateAccessibilityAndCoverImage(MultipartFile coverImage, String coverImageAlt) {
        boolean hasImageUrl = coverImage != null && !coverImage.isEmpty();
        boolean hasImageAlt = coverImageAlt != null && !coverImageAlt.isBlank();

        if (hasImageUrl && !hasImageAlt) {
            throw new IllegalArgumentException(
                    "O texto alternativo (coverImageAlt) é obrigatório quando uma imagem de capa é fornecida.");
        }
    }

    @Transactional(readOnly = true)
    protected Set<ClothType> resolveClothTypes(Set<Long> clothTypeIds) {
        if (clothTypeIds == null || clothTypeIds.isEmpty()) {
            return new HashSet<>();
        }

        List<ClothType> foundTypes = clothTypeRepository.findAllById(clothTypeIds);
        if (foundTypes.size() != clothTypeIds.size()) {
            Set<Long> foundIds = foundTypes.stream()
                    .map(ClothType::getId)
                    .collect(Collectors.toSet());

            Set<Long> missingIds = clothTypeIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toSet());

            throw new IllegalArgumentException(
                    "Os seguintes IDs de tipos de tecido não foram encontrados: " + missingIds);
        }

        return new HashSet<>(foundTypes);
    }

    private Set<String> sanitizeTags(Set<String> rawTags) {
        if (rawTags == null || rawTags.isEmpty()) {
            return new HashSet<>();
        }

        return rawTags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .map(tag -> tag.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private String generateUniqueSlug(String title) {
        String baseSlug = toSlug(title);
        String slug = baseSlug;
        int sequence = 1;

        while (postRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + sequence;
            sequence++;
        }

        return slug;
    }

    private String toSlug(String input) {
        if (input == null) {
            return "";
        }
        String nowhitespace = WHITESPACE.matcher(input.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH).replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
    }

    @Transactional(readOnly = true)
    public Boolean existsByCategory(Category category) {
        return postRepository.existsByCategory(category);
    }

    @Transactional (readOnly = true)
    public String findPostImageById(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post inexistente."));
        return post.getCoverImageUrl();
    }

    @Transactional(readOnly = true)
    public Resource findImageByPostIdAndIsPublishedTrue(Long id) {
        Post post = postRepository.findByIdAndIsPublishedTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("Post inexistente ou não publicado."));
        String imageUrl = post.getCoverImageUrl();
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("Post não possui imagem de capa.");
        }
        Resource imageResource = fileStorageService.loadPostCoverImage(imageUrl);
        if (imageResource == null) {
            throw new IllegalArgumentException("Imagem não encontrada no servidor.");
        }
        return imageResource;
    }

    @Transactional(readOnly = true)
    public Resource findImageByPostId(Long id) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new IllegalArgumentException("Post inexistente ou não disponível."));

        String fileName = post.getCoverImageUrl();
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("O post informado não possui imagem.");
        }

        Resource resource = fileStorageService.loadPostCoverImage(fileName);
        if (resource == null) {
            throw new IllegalArgumentException("Arquivo de imagem não encontrado no servidor.");
        }

        return resource;
    }

}
