package com.alexander.springboot.insumotronics.service.storage;

import com.alexander.springboot.insumotronics.enums.FileType;
import okhttp3.*;
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

/**
 * Implementación de FileStorageService usando Supabase Storage.
 * La URL que se guarda en la entidad Product (imageUrls / documentUrls) ya es
 * una URL completa y accesible directamente, sin necesidad de llamadas adicionales
 * al backend.
 */
@Service
public class SupabaseFileStorageServiceImpl implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseFileStorageServiceImpl.class);

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd--HH-mm-ss-SSS");

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

    private final String supabaseUrl;
    private final String serviceRoleKey;
    private final String imagesBucket;
    private final String documentsBucket;
    private final List<String> allowedExtensions;
    private final List<String> allowedMimeTypes;
    private final OkHttpClient httpClient;

    public SupabaseFileStorageServiceImpl(
            @Value("${supabase.url}") String supabaseUrl,
            @Value("${supabase.service-role-key}") String serviceRoleKey,
            @Value("${supabase.bucket.images}") String imagesBucket,
            @Value("${supabase.bucket.documents}") String documentsBucket,
            @Value("${app.settings.allowed_extencions}") String extensions,
            @Value("${app.settings.allowed_mime_types}") String mimeTypes) {

        // Remover trailing slash si existe
        this.supabaseUrl = supabaseUrl.endsWith("/")
                ? supabaseUrl.substring(0, supabaseUrl.length() - 1)
                : supabaseUrl;
        this.serviceRoleKey = serviceRoleKey;
        this.imagesBucket = imagesBucket;
        this.documentsBucket = documentsBucket;
        this.allowedExtensions = Arrays.asList(extensions.toLowerCase().split(","));
        this.allowedMimeTypes = Arrays.asList(mimeTypes.toLowerCase().split(","));
        this.httpClient = new OkHttpClient();

        log.info("SupabaseFileStorageServiceImpl inicializado. URL: {} | Bucket imágenes: {} | Bucket documentos: {}",
                this.supabaseUrl, this.imagesBucket, this.documentsBucket);
    }

    // =========================================================================
    // MÉTODO PRINCIPAL: subir archivo
    // Retorna la URL pública/firmada completa lista para guardar en la entidad
    // =========================================================================

    /**
     * Sube un archivo a Supabase Storage y devuelve su URL pública.
     *
     * El bucket se selecciona automáticamente según el tipo de archivo:
     *   - Imágenes (PNG, JPG, JPEG, SVG) → bucket "images" (público)
     *   - Documentos (PDF)               → bucket "documents"
     *   - Video (MP4)                    → bucket "images" (públicos)
     *
     * @param file archivo a subir
     * @return URL pública completa del archivo en Supabase
     */
    @Override
    public String saveFile(MultipartFile file) throws IOException {
        // --- Validaciones ---
        validateFile(file);

        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        String safeFilename = sanitizeFilename(originalFilename);
        FileType fileType = extractExtension(safeFilename);

        // Seleccionar bucket según tipo
        String bucket = isImageOrVideo(fileType) ? imagesBucket : documentsBucket;

        // Construir nombre único para evitar colisiones
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String uniqueId = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String objectPath = timestamp + "_" + uniqueId + "_" + safeFilename;

        log.info("Subiendo archivo a Supabase Storage | bucket: {} | path: {} | tipo: {} | tamaño: {} bytes",
                bucket, objectPath, contentType, file.getSize());

        // Construir la petición HTTP a la API de Supabase Storage
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + objectPath;

        RequestBody body = RequestBody.create(
                file.getBytes(),
                MediaType.parse(contentType)
        );

        Request request = new Request.Builder()
                .url(uploadUrl)
                .addHeader("Authorization", "Bearer " + serviceRoleKey)
                .addHeader("apikey", serviceRoleKey)
                .addHeader("Content-Type", contentType)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "sin cuerpo";
                log.error("Error al subir archivo a Supabase. Código: {} | Respuesta: {}",
                        response.code(), errorBody);
                throw new IOException("Error al subir archivo a Supabase Storage. Código HTTP: "
                        + response.code() + " | " + errorBody);
            }
            log.info("Archivo subido exitosamente: {}/{}", bucket, objectPath);
        }

        // Devolver URL pública permanente (para bucket público)
        return getPublicUrl(bucket, objectPath);
    }

    /**
     * Genera una URL pública permanente para un archivo en un bucket público.
     * Formato: {supabaseUrl}/storage/v1/object/public/{bucket}/{objectPath}
     */
    public String getPublicUrl(String bucket, String objectPath) {
        return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + objectPath;
    }

    /**
     * Genera una URL firmada con expiración para un archivo.
     * Útil para buckets privados (ej: documentos confidenciales).
     *
     * @param fileUrl URL pública o path del archivo (extrae el path automáticamente)
     * @param expiryMinutes minutos de validez de la URL firmada
     * @return URL firmada con expiración
     */
    @Override
    public String getPresignedUrl(String fileUrl, int expiryMinutes) {
        // Extraer bucket y objectPath desde la URL pública si se pasa la URL completa
        String bucket;
        String objectPath;

        if (fileUrl.contains("/storage/v1/object/public/")) {
            String afterPublic = fileUrl.substring(fileUrl.indexOf("/storage/v1/object/public/")
                    + "/storage/v1/object/public/".length());
            int slashIdx = afterPublic.indexOf('/');
            if (slashIdx == -1) throw new IllegalArgumentException("URL de archivo inválida: " + fileUrl);
            bucket = afterPublic.substring(0, slashIdx);
            objectPath = afterPublic.substring(slashIdx + 1);
        } else {
            // Asumir que es el path relativo "bucket/objectPath" o solo el "objectPath"
            int slashIdx = fileUrl.indexOf('/');
            if (slashIdx == -1) {
                // Si no hay '/', asumimos que es solo el nombre del archivo y deducimos el bucket
                FileType fileType = extractExtension(fileUrl);
                bucket = isImageOrVideo(fileType) ? imagesBucket : documentsBucket;
                objectPath = fileUrl;
            } else {
                bucket = fileUrl.substring(0, slashIdx);
                objectPath = fileUrl.substring(slashIdx + 1);
            }
        }

        // Ahora tanto imagesBucket como documentsBucket son públicos,
        // por lo que podemos devolver la URL pública directamente para ambos.
        if (bucket.equals(imagesBucket) || bucket.equals(documentsBucket)) {
            return getPublicUrl(bucket, objectPath);
        }

        String signUrl = supabaseUrl + "/storage/v1/object/sign/" + bucket + "/" + objectPath;
        String jsonBody = "{\"expiresIn\": " + (expiryMinutes * 60) + "}";

        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(signUrl)
                .addHeader("Authorization", "Bearer " + serviceRoleKey)
                .addHeader("apikey", serviceRoleKey)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new RuntimeException("Error al generar URL firmada. Código: "
                        + response.code() + " | " + errorBody);
            }
            String responseBody = response.body() != null ? response.body().string() : "";
            // Extraer signedURL del JSON de respuesta (sin dependencia extra)
            return extractSignedUrl(responseBody);
        } catch (IOException e) {
            log.error("Error al generar URL firmada para: {}", fileUrl, e);
            throw new RuntimeException("Error al generar URL firmada: " + e.getMessage(), e);
        }
    }

    /**
     * Elimina un archivo de Supabase Storage dado su URL pública completa.
     */
    @Override
    public boolean deleteFile(String fileUrl) throws IOException {
        if (fileUrl == null || fileUrl.isBlank()) {
            log.warn("Se intentó eliminar un archivo con URL nula o vacía.");
            return false;
        }

        // Extraer bucket y objectPath desde la URL
        String bucket;
        String objectPath;

        if (fileUrl.contains("/storage/v1/object/public/")) {
            String afterPublic = fileUrl.substring(fileUrl.indexOf("/storage/v1/object/public/")
                    + "/storage/v1/object/public/".length());
            int slashIdx = afterPublic.indexOf('/');
            if (slashIdx == -1) {
                log.error("No se pudo extraer el path del archivo desde la URL: {}", fileUrl);
                return false;
            }
            bucket = afterPublic.substring(0, slashIdx);
            objectPath = afterPublic.substring(slashIdx + 1);
        } else {
            log.warn("URL de archivo no reconocida para eliminación: {}. Saltando.", fileUrl);
            return false;
        }

        String deleteUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + objectPath;
        Request request = new Request.Builder()
                .url(deleteUrl)
                .addHeader("Authorization", "Bearer " + serviceRoleKey)
                .addHeader("apikey", serviceRoleKey)
                .delete()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                log.info("Archivo eliminado de Supabase: {}/{}", bucket, objectPath);
                return true;
            } else {
                String errorBody = response.body() != null ? response.body().string() : "";
                log.error("Error al eliminar archivo de Supabase. Código: {} | {}", response.code(), errorBody);
                return false;
            }
        }
    }

    // =========================================================================
    // MÉTODO NO SOPORTADO (legacy de implementación local)
    // =========================================================================

    @Override
    public File getDownloadFile(String fileName) throws FileNotFoundException {
        throw new UnsupportedOperationException(
                "Supabase usa URLs directas. Use getPresignedUrl() o las URLs públicas guardadas en la entidad.");
    }

    @Override
    public FileType extractExtension(String filename) {
        if (filename == null || filename.isBlank()) return FileType.UNKNOWN;
        String extension = getFileExtension(filename).toLowerCase();
        try {
            return FileType.valueOf(extension.toUpperCase());
        } catch (IllegalArgumentException e) {
            return FileType.UNKNOWN;
        }
    }

    /**
     * Extrae el nombre original de un archivo quitando el prefijo de timestamp y UUID.
     * Formato esperado: yyyy-MM-dd--HH-mm-ss-SSS_UIDXXXXXXXX_nombre-original.ext
     */
    @Override
    public String extractOriginalFilename(String fileUrl) {
        if (fileUrl == null) return null;
        // Obtener solo el nombre de archivo desde la URL
        String filename = fileUrl.contains("/") ? fileUrl.substring(fileUrl.lastIndexOf('/') + 1) : fileUrl;
        // Quitar prefijo: timestamp_uid_
        String[] parts = filename.split("_", 3);
        return parts.length >= 3 ? parts[2] : filename;
    }

    private void validateFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo no puede ser nulo o vacío.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IOException("El archivo supera el tamaño máximo permitido de 10 MB. "
                    + "Tamaño recibido: " + (file.getSize() / (1024 * 1024)) + " MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !allowedMimeTypes.contains(contentType.toLowerCase())) {
            throw new IOException("Tipo de archivo no permitido: " + contentType
                    + ". Tipos permitidos: " + allowedMimeTypes);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del archivo no puede ser nulo o vacío.");
        }

        String extension = getFileExtension(sanitizeFilename(originalFilename)).toLowerCase();
        if (!allowedExtensions.contains(extension)) {
            throw new IOException("Extensión de archivo no permitida: ." + extension
                    + ". Extensiones permitidas: " + allowedExtensions);
        }
    }

    private boolean isImageOrVideo(FileType fileType) {
        return fileType == FileType.PNG || fileType == FileType.JPG
                || fileType == FileType.JPEG || fileType == FileType.SVG
                || fileType == FileType.MP4;
    }

    private String sanitizeFilename(String filename) {
        return filename
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", "_")
                .toLowerCase()
                .trim();
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot == -1 ? "" : filename.substring(lastDot + 1);
    }

    /**
     * Extrae el campo "signedURL" de la respuesta JSON de Supabase.
     * Ejemplo de respuesta: {"signedURL":"/storage/v1/object/sign/...?token=xxx"}
     * Retorna la URL completa con el host de Supabase.
     */
    private String extractSignedUrl(String json) {
        // Parsing manual simple — sin dependencia extra de Jackson
        // También puede venir como "signedUrl" en minúscula en algunas versiones
        String key = "\"signedURL\"";
        int keyIdx = json.indexOf(key);
        if (keyIdx == -1) {
            key = "\"signedUrl\"";
            keyIdx = json.indexOf(key);
            if (keyIdx == -1) {
                throw new RuntimeException("Respuesta de Supabase inesperada: " + json);
            }
        }
        int colonIdx = json.indexOf(':', keyIdx);
        int startQuote = json.indexOf('"', colonIdx + 1);
        int endQuote = json.indexOf('"', startQuote + 1);
        String signedPath = json.substring(startQuote + 1, endQuote);

        // Limpiar caracteres escapados de JSON (ej: \/ -> /)
        signedPath = signedPath.replace("\\/", "/");

        // Si el path ya es una URL completa, devolverla tal cual
        if (signedPath.startsWith("http")) return signedPath;
        // Si es un path relativo, agregar el host de Supabase
        return supabaseUrl + signedPath;
    }
}
