package br.com.maisprati.projeto.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of("image/jpeg", "image/png", "image/webp");

    public void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }

        // 1. Valida o MIME type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Formato de arquivo inválido. Formatos suportados: JPEG, PNG e WebP.");
        }

        // 2. Valida a extensão do nome original
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.matches("(?i).*\\.(jpg|jpeg|png|webp)$")) {
            throw new IllegalArgumentException("Extensão de arquivo inválida.");
        }
    }

    public String saveImage(MultipartFile file, String prefix) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        validateImageFile(file);

        try {
            Path targetDir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(targetDir);

            // Extrai a extensão do arquivo (.jpg, .png, etc.)
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            }

            // Nomeia o arquivo com o id do post e o timestamp para evitar conflitos
            String fileName = prefix + "_" + System.currentTimeMillis() + extension;
            Path filePath = targetDir.resolve(fileName);

            // Salva o arquivo na pasta de uploads
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return fileName;
        } catch (IOException e) {
            throw new RuntimeException("Falha ao gravar a imagem no servidor.", e);
        }
    }

    public String savePostCoverImage(Long postId, MultipartFile file) {
        return saveEntityImage(file, "posts", postId, "cover");
    }

    public String saveCollectionPointImage(Long collectionPointId, MultipartFile file) {
        return saveEntityImage(file, "collection-points", collectionPointId, "image");
    }

    private String saveEntityImage(
            MultipartFile file,
            String resourceType,
            Long resourceId,
            String filePrefix) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        validateImageFile(file);

        try {
            Path baseDir = baseDirectory();
            Path targetDir = resolveEntityDirectory(resourceType, resourceId);
            Files.createDirectories(targetDir);

            String extension = extractExtension(file.getOriginalFilename());
            String fileName = filePrefix + "_" + System.currentTimeMillis() + extension;
            Path filePath = targetDir.resolve(fileName).normalize();

            if (!filePath.startsWith(targetDir)) {
                throw new IllegalArgumentException("Caminho de arquivo inválido.");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            return baseDir.relativize(filePath).toString().replace('\\', '/');
        } catch (IOException e) {
            throw new RuntimeException("Falha ao gravar a imagem no servidor.", e);
        }
    }

    public Resource loadPostCoverImage(String fileName) {
        return loadImage(fileName);
    }

    public Resource loadImage(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }

        try {
            Path baseDir = baseDirectory();
            Path filePath = baseDir.resolve(fileName).normalize();
            if (!filePath.startsWith(baseDir)) {
                throw new IllegalArgumentException("Caminho de arquivo inválido.");
            }
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            return null;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Caminho do arquivo inválido.", e);
        }
    }

    public void deletePostCoverImage(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return;
        }
        try {
            Path baseDir = baseDirectory();
            Path filePath = baseDir.resolve(fileName).normalize();
            if (!filePath.startsWith(baseDir)) {
                throw new IllegalArgumentException("Caminho de arquivo inválido.");
            }
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Falha ao deletar a imagem do post.", e);
        }
    }

    private Path baseDirectory() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    private Path resolveEntityDirectory(String resourceType, Long resourceId) {
        if (resourceId == null || resourceId < 1) {
            throw new IllegalArgumentException("Identificador de recurso inválido.");
        }

        Path baseDir = baseDirectory();
        Path entityDir = baseDir.resolve(resourceType).resolve(String.valueOf(resourceId)).normalize();
        if (!entityDir.startsWith(baseDir)) {
            throw new IllegalArgumentException("Caminho de armazenamento inválido.");
        }
        return entityDir;
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
    }
}
