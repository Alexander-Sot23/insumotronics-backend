package com.alexander.springboot.insumotronics.service.storage;

/**
 * CLASE DESACTIVADA - Migrado a Supabase Storage.
 * Se conserva como referencia histórica. Ver: SupabaseFileStorageServiceImpl.java
 *
 * Para reactivar:
 *  1. Quitar los comentarios de bloque (/* ... ) de abajo
 *  2. Restaurar la dependencia de MinIO en pom.xml
 *  3. Restaurar las variables de MinIO en application.properties
 *  4. Comentar o borrar SupabaseFileStorageServiceImpl.java
 */

/*

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

// @Service  -- DESACTIVADO: usar SupabaseFileStorageServiceImpl
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
                    BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al crear/verificar bucket en MinIO", e);
        }
    }

    @Override
    public String saveFile(MultipartFile fileToSave) throws IOException {
        if (fileToSave == null || fileToSave.isEmpty())
            throw new IllegalArgumentException("File to save is null or empty.");
        String contentType = fileToSave.getContentType();
        if (contentType == null || !isAllowedMimeType(contentType))
            throw new IOException("File type not supported: " + contentType);
        String originalFilename = fileToSave.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty())
            throw new IllegalArgumentException("Original filename is null or empty");
        String safeFilename = sanitizeFilename(originalFilename);
        if (!hasAllowedExtension(safeFilename))
            throw new IOException("File extension not allowed: " + safeFilename);
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String objectName = timestamp + "_" + uniqueId + "_" + safeFilename;
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName).object(objectName)
                    .stream(fileToSave.getInputStream(), fileToSave.getSize(), -1L)
                    .contentType(contentType).build());
            log.info("Archivo subido a MinIO: {} | Tipo: {} | Tamano: {} bytes",
                    fileToSave.getOriginalFilename(), contentType, fileToSave.getSize());
            return objectName;
        } catch (Exception e) {
            log.error("Error al subir {} a MinIO", fileToSave.getOriginalFilename(), e);
            throw new IOException("Error al subir archivo a MinIO: " + e.getMessage(), e);
        }
    }

    @Override
    public File getDownloadFile(String fileName) throws FileNotFoundException {
        throw new UnsupportedOperationException("Con MinIO use getPresignedUrl()");
    }

    @Override
    public String getPresignedUrl(String objectName, int expiryMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucketName).object(objectName)
                    .method(Method.GET).expiry(expiryMinutes * 60).build());
        } catch (Exception e) {
            throw new RuntimeException("Error al generar URL firmada para: " + objectName, e);
        }
    }

    @Override
    public boolean deleteFile(String fileName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName).object(fileName).build());
            log.info("Archivo {} eliminado de MinIO", fileName);
            return true;
        } catch (Exception e) {
            log.error("Error al eliminar {} en MinIO", fileName, e);
            return false;
        }
    }

    @Override
    public FileType extractExtension(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        try { return FileType.valueOf(extension.toUpperCase()); }
        catch (IllegalArgumentException e) { return FileType.UNKNOWN; }
    }

    @Override
    public String extractOriginalFilename(String filename) {
        if (filename == null) return null;
        String withoutTimestamp = filename.substring(filename.indexOf('_') + 1);
        int secondUnderscore = withoutTimestamp.indexOf('_');
        if (secondUnderscore == -1) return filename;
        return withoutTimestamp.substring(secondUnderscore + 1);
    }

    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s+", "_").trim();
    }
    private boolean isAllowedMimeType(String mimeType) {
        return mimeType != null && allowedMimeTypes.contains(mimeType.toLowerCase());
    }
    private boolean hasAllowedExtension(String filename) {
        return allowedExtensions.contains(getFileExtension(filename).toLowerCase());
    }
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot == -1 ? "" : filename.substring(lastDot + 1);
    }
}

*/