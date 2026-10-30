package br.com.maisprati.ecotecidos.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

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
    private static final long MAX_AVATAR_SIZE_BYTES = 5 * 1024 * 1024;
    private static final int MAX_AVATAR_DIMENSION = 8_000;
    private static final int AVATAR_SIZE = 512;

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

    public String saveUserAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        if (file.getSize() > MAX_AVATAR_SIZE_BYTES) {
            throw new IllegalArgumentException("O avatar deve ter no máximo 5 MB.");
        }

        validateImageFile(file);

        try {
            BufferedImage original;
            try (InputStream inputStream = file.getInputStream()) {
                original = ImageIO.read(inputStream);
            }

            if (original == null) {
                throw new IllegalArgumentException("O arquivo enviado não contém uma imagem válida.");
            }
            if (original.getWidth() > MAX_AVATAR_DIMENSION
                    || original.getHeight() > MAX_AVATAR_DIMENSION) {
                throw new IllegalArgumentException("As dimensões do avatar não podem exceder 8000x8000 pixels.");
            }

            Path baseDir = baseDirectory();
            Path targetDir = resolveEntityDirectory("users", userId);
            Files.createDirectories(targetDir);

            Path temporaryPath = targetDir.resolve("avatar.tmp").normalize();
            Path targetPath = targetDir.resolve("avatar.jpg").normalize();
            if (!temporaryPath.startsWith(targetDir) || !targetPath.startsWith(targetDir)) {
                throw new IllegalArgumentException("Caminho de armazenamento inválido.");
            }

            writeJpeg(createAvatarImage(original), temporaryPath);

            try {
                Files.move(temporaryPath, targetPath,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(temporaryPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            return baseDir.relativize(targetPath).toString().replace('\\', '/');
        } catch (IOException e) {
            throw new RuntimeException("Falha ao processar o avatar.", e);
        }
    }

    private void writeJpeg(BufferedImage image, Path targetPath) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IOException("Nenhum codificador JPEG disponível.");
        }

        ImageWriter writer = writers.next();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(targetPath.toFile())) {
            writer.setOutput(output);
            ImageWriteParam parameters = writer.getDefaultWriteParam();
            parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parameters.setCompressionQuality(0.85f);
            writer.write(null, new javax.imageio.IIOImage(image, null, null), parameters);
        } finally {
            writer.dispose();
        }
    }

    private BufferedImage createAvatarImage(BufferedImage original) {
        int cropSize = Math.min(original.getWidth(), original.getHeight());
        int x = (original.getWidth() - cropSize) / 2;
        int y = (original.getHeight() - cropSize) / 2;

        BufferedImage avatar = new BufferedImage(AVATAR_SIZE, AVATAR_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = avatar.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(original, 0, 0, AVATAR_SIZE, AVATAR_SIZE,
                    x, y, x + cropSize, y + cropSize, null);
        } finally {
            graphics.dispose();
        }
        return avatar;
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
        deleteImage(fileName);
    }

    public void deleteImage(String fileName) {
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
            throw new RuntimeException("Falha ao deletar a imagem.", e);
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
