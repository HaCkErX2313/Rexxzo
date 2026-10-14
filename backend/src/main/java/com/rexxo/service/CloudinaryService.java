package com.rexxo.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.rexxo.exception.RexxoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class CloudinaryService {

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    public record UploadResult(String url, String publicId, String filename) {}

    public boolean isConfigured() {
        return cloudName != null && !cloudName.isBlank() && !"placeholder".equalsIgnoreCase(cloudName.trim())
            && apiKey != null && !apiKey.isBlank() && !"placeholder".equalsIgnoreCase(apiKey.trim())
            && apiSecret != null && !apiSecret.isBlank() && !"placeholder".equalsIgnoreCase(apiSecret.trim());
    }

    public UploadResult uploadImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw RexxoException.badRequest("File cannot be empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/"))) {
            throw RexxoException.badRequest("Only image files (JPEG, PNG, WEBP, GIF) are allowed");
        }

        if (isConfigured()) {
            try {
                Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName.trim(),
                    "api_key", apiKey.trim(),
                    "api_secret", apiSecret.trim(),
                    "secure", true
                ));

                String targetFolder = (folder != null && !folder.isBlank()) ? folder.trim() : "rexxzo/products";
                Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", targetFolder,
                    "resource_type", "image"
                ));

                String secureUrl = (String) uploadResult.get("secure_url");
                String publicId = (String) uploadResult.get("public_id");

                log.info("Durable image uploaded to Cloudinary: publicId={}, url={}", publicId, secureUrl);
                return new UploadResult(secureUrl, publicId, file.getOriginalFilename());
            } catch (Exception e) {
                log.error("Cloudinary upload failed: {}", e.getMessage(), e);
                throw RexxoException.internal("Failed to upload image to durable cloud storage: " + e.getMessage());
            }
        }

        // Fallback for offline development or when Cloudinary env vars are missing
        log.warn("Cloudinary is not configured. Falling back to local file storage (NOTE: container restarts may lose local files in production).");
        try {
            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "product.jpg";
            String extension = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf(".")) : ".jpg";
            String filename = "prod_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;

            Path uploadDir = Paths.get("frontend", "public", "images", "uploads");
            if (!Files.exists(uploadDir)) {
                Path projectRoot = Paths.get("..", "frontend", "public", "images", "uploads");
                if (Files.exists(Paths.get("..", "frontend"))) {
                    uploadDir = projectRoot;
                }
                Files.createDirectories(uploadDir);
            }

            Path targetPath = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String webUrl = "/images/uploads/" + filename;
            return new UploadResult(webUrl, null, filename);
        } catch (IOException e) {
            log.error("Fallback file saving failed: {}", e.getMessage());
            throw RexxoException.internal("Failed to save uploaded image: " + e.getMessage());
        }
    }

    public void deleteImage(String publicId) {
        if (!isConfigured() || publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName.trim(),
                "api_key", apiKey.trim(),
                "api_secret", apiSecret.trim(),
                "secure", true
            ));
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Deleted image from Cloudinary: {}", publicId);
        } catch (Exception e) {
            log.warn("Failed to delete image from Cloudinary (publicId: {}): {}", publicId, e.getMessage());
        }
    }
}
