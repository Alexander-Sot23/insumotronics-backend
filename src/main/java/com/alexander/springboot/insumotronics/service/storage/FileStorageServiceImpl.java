package com.alexander.springboot.insumotronics.service.storage;

import com.alexander.springboot.insumotronics.enums.FileType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService{

    private final String storageDirectory;

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd--HH-mm-ss-SS");

    private final List<String> allowedExtensions;

    private final List<String> allowedMimeTypes;

    // Constructor que recibe las propiedades
    public FileStorageServiceImpl(
            @Value("${app.settings.storage_directory}") String storageDirectory,
            @Value("${app.settings.allowed_extencions}") String extensions,
            @Value("${app.settings.allowed_mime_types}") String mimeTypes){

        // Normalizamos la ruta para Windows/Linux
        this.storageDirectory = normalizePath(storageDirectory);
        this.allowedExtensions = Arrays.asList(extensions.toLowerCase().split(","));
        this.allowedMimeTypes = Arrays.asList(mimeTypes.toLowerCase().split(","));

        // Crear directorio si no existe
        createStorageDirectory();
    }

    private String normalizePath(String path) {
        // Reemplazar barras invertidas por barras normales
        String normalized = path.replace("\\", "/");

        // Si es Windows, asegurar formato correcto
        if (normalized.contains(":")) {
            // Windows: C:/Spring-r-chan
            return normalized.replace("//", "/");
        } else {
            // Linux/Unix
            return normalized;
        }
    }

    private void createStorageDirectory() {
        try {
            Path path = Paths.get(storageDirectory);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                System.out.println("Directorio creado: " + storageDirectory);
            }
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear el directorio: " + storageDirectory, e);
        }
    }

    private void validateFileType(File file) {
        try {
            String fileType = Files.probeContentType(file.toPath());
            if (!isAllowedMimeType(fileType)) {
                throw new SecurityException("File type not supported: " + fileType);
            }
        } catch (IOException e) {
            throw new SecurityException("Cannot detect file type: " + file.getName(), e);
        }
    }

    private String sanitizeFilename(String filename) {

        return filename
                .replaceAll("[\\\\/:*?\"<>|]", "_")  //Caracteres inválidos en Windows
                .replaceAll("\\s+", "_")             //Espacios por guiones bajos
                .trim();
    }

    private void validatePathSafety(Path path) throws SecurityException {
        try {
            Path normalizedPath = path.toAbsolutePath().normalize();
            Path storagePath = Paths.get(storageDirectory).toAbsolutePath().normalize();

            // Verificar que el archivo esté dentro del directorio de almacenamiento
            if (!normalizedPath.startsWith(storagePath)) {
                throw new SecurityException("Unsupported file path: attempted path traversal");
            }
        } catch (Exception e) {
            throw new SecurityException("Invalid file path", e);
        }
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
        if (lastDot == -1) {
            return "";
        }
        return filename.substring(lastDot + 1);
    }

    @Override
    public String saveFile(MultipartFile fileToSave) throws IOException {
        if (fileToSave == null || fileToSave.isEmpty()) {
            throw new IllegalArgumentException("File to save is null or empty.");
        }

        // Validar tipo MIME
        String contentType = fileToSave.getContentType();
        if (!isAllowedMimeType(contentType)) {
            throw new IOException("File type not supported: " + contentType);
        }

        // Validar nombre de archivo
        String originalFilename = fileToSave.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid filename");
        }

        // Limpiar nombre de archivo
        originalFilename = sanitizeFilename(originalFilename);

        //Validar extensión
        if (!hasAllowedExtension(originalFilename)) {
            throw new IOException("File extension not allowed: " + originalFilename);
        }

        // Generar nombre único
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String safeFilename = timestamp + "_" + uniqueId + "_" + originalFilename;

        // Crear ruta completa
        Path targetPath = Paths.get(storageDirectory, safeFilename);

        // Validar que no haya path traversal
        validatePathSafety(targetPath);

        // Guardar archivo
        Files.copy(fileToSave.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        return safeFilename;
    }

    @Override
    public File getDownloadFile(String fileName) throws FileNotFoundException {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("File name is null or empty");
        }

        fileName = sanitizeFilename(fileName);

        Path filePath = Paths.get(storageDirectory, fileName);
        File fileToDownload = filePath.toFile();

        if (!fileToDownload.exists()) {
            throw new FileNotFoundException("No file named: " + fileName);
        }

        // Validar seguridad de ruta
        validatePathSafety(filePath);

        // Validar tipo de archivo
        validateFileType(fileToDownload);

        return fileToDownload;
    }

    @Override
    public boolean deleteFile(String fileName) throws IOException {
        if (fileName == null || fileName.trim().isEmpty()) {
            return false;
        }

        fileName = sanitizeFilename(fileName);
        Path filePath = Paths.get(storageDirectory, fileName);

        // Validar seguridad
        validatePathSafety(filePath);

        if (Files.exists(filePath)) {
            Files.delete(filePath);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public FileType extractExtension(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        try {
            return FileType.valueOf(extension.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Si no existe en el enum, devolver un valor por defecto
            return FileType.UNKNOWN;
        }
    }

    @Override
    public String extractOriginalFilename(String filename) {
        if (filename == null) {
            return null;
        }

        int lastUnderscore = filename.lastIndexOf('_');
        if (lastUnderscore == -1) {
            return filename;
        }

        String withoutTimestamp = filename.substring(filename.indexOf('_') + 1);
        int secondUnderscore = withoutTimestamp.indexOf('_');

        if (secondUnderscore == -1) {
            return filename;
        }

        return withoutTimestamp.substring(secondUnderscore + 1);
    }
}
