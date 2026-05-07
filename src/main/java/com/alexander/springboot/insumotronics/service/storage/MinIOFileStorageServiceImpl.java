package com.alexander.springboot.insumotronics.service.storage;

import com.alexander.springboot.insumotronics.enums.FileType;
import io.minio.*;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class MinIOFileStorageServiceImpl implements FileStorageService {

    private final MinioClient minioClient;
    private final String bucketName;

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd--HH-mm-ss-SS");

    private final List<String> allowedExtensions;
    private final List<String> allowedMimeTypes;
    private final Logger log = LoggerFactory.getLogger(MinIOFileStorageServiceImpl.class);

    public MinIOFileStorageServiceImpl(
            @Value("${app.minio.endpoint}") String endpoint,
            @Value("${app.minio.access-key}") String accessKey,
            @Value("${app.minio.secret-key}") String secretKey,
            @Value("${app.minio.bucket}") String bucketName,
            @Value("${app.settings.allowed_extencions}") String extensions,
            @Value("${app.settings.allowed_mime_types}") String mimeTypes) {

        this.bucketName = bucketName;

        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();

        this.allowedExtensions = Arrays.asList(extensions.toLowerCase().split(","));
        this.allowedMimeTypes = Arrays.asList(mimeTypes.toLowerCase().split(","));

        createBucketIfNotExists();
    }

    private void createBucketIfNotExists() {
        try {
            boolean found = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                System.out.println("Bucket MinIO creado: " + bucketName);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al crear/verificar bucket en MinIO", e);
        }
    }

    @Override
    public String saveFile(MultipartFile fileToSave) throws IOException {
        if (fileToSave == null || fileToSave.isEmpty()) {
            throw new IllegalArgumentException("File to save is null or empty.");
        }

        String contentType = fileToSave.getContentType();
        if (contentType == null || !isAllowedMimeType(contentType)) {
            throw new IOException("File type not supported: " + contentType);
        }

        String originalFilename = fileToSave.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("Original filename is null or empty");
        }

        String safeFilename = sanitizeFilename(originalFilename);
        if (!hasAllowedExtension(safeFilename)) {
            throw new IOException("File extension not allowed: " + safeFilename);
        }

        // Generar nombre único
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String objectName = timestamp + "_" + uniqueId + "_" + safeFilename;

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(fileToSave.getInputStream(), fileToSave.getSize(), -1L)
                            .contentType(contentType)
                            .build()
            );

            log.info("Subiendo archivo a MinIO: {}| Tipo: {} | Tamaño: {} bytes",
                    fileToSave.getOriginalFilename(), contentType, fileToSave.getSize());
            return objectName;

        } catch (Exception e) {
            log.error("Error al subir el archivo {} a MinIO",
                    fileToSave.getOriginalFilename(), e);
            throw new IOException("Error al subir archivo a MinIO: " + e.getMessage(), e);
        }
    }

    @Override
    public File getDownloadFile(String fileName) throws FileNotFoundException {
        throw new UnsupportedOperationException("Con MinIO use getPresignedUrl() en lugar de getDownloadFile()");
    }

    @Override
    public String getPresignedUrl(String objectName, int expiryMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .method(Method.GET)
                            .expiry(expiryMinutes * 60)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Error al generar URL firmada para: " + objectName, e);
        }
    }

    @Override
    public boolean deleteFile(String fileName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );
            log.info("Archivo {} eliminado correctamente de MinIO",
                    fileName);
            return true;
        } catch (Exception e) {
            log.error("Error al eliminar el archivo {} en MinIO", fileName, e);
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public FileType extractExtension(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        try {
            return FileType.valueOf(extension.toUpperCase());
        } catch (IllegalArgumentException e) {
            return FileType.UNKNOWN;
        }
    }

    @Override
    public String extractOriginalFilename(String filename) {
        if (filename == null) return null;

        int lastUnderscore = filename.lastIndexOf('_');
        if (lastUnderscore == -1) return filename;

        String withoutTimestamp = filename.substring(filename.indexOf('_') + 1);
        int secondUnderscore = withoutTimestamp.indexOf('_');
        if (secondUnderscore == -1) return filename;

        return withoutTimestamp.substring(secondUnderscore + 1);
    }

    private String sanitizeFilename(String filename) {
        return filename
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", "_")
                .trim();
    }

    private boolean isAllowedMimeType(String mimeType) {
        return mimeType != null && allowedMimeTypes.contains(mimeType.toLowerCase());
    }

    private boolean hasAllowedExtension(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        return allowedExtensions.contains(extension);
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) return "";
        return filename.substring(lastDot + 1);
    }
}