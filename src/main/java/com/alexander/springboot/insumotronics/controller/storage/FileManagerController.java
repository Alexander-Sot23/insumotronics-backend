package com.alexander.springboot.insumotronics.controller.storage;

/*
 * Este controlador funciona tanto para los usuarios con rol STUDENT, TEACHER y ADMIN
 */

import com.alexander.springboot.insumotronics.service.storage.FileStorageService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/student/files")
public class FileManagerController {

    private static final Logger log = LoggerFactory.getLogger(FileManagerController.class);
    private final FileStorageService fileStorageService;
    private final OkHttpClient httpClient;

    public FileManagerController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
        this.httpClient = new OkHttpClient();
    }

    /**
     * Devuelve una URL firmada para ver el archivo (imágenes, PDFs, etc.)
     */
    @GetMapping("/view-file")
    public ResponseEntity<String> viewFile(@RequestParam("fileName") String fileName) {
        try {
            // URL válida por 60 minutos
            String presignedUrl = fileStorageService.getPresignedUrl(fileName, 60);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "text/plain")
                    .body(presignedUrl);

        } catch (Exception e) {
            log.error("Error al generar URL de visualización para {}: {}", fileName, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Actúa como un proxy para descargar el archivo desde Supabase.
     */
    @GetMapping("/download-file")
    public ResponseEntity<byte[]> downloadFile(@RequestParam("fileName") String fileName) {
        try {
            // 1. Obtener la URL firmada de Supabase
            String presignedUrl = fileStorageService.getPresignedUrl(fileName, 5);
            String originalName = fileStorageService.extractOriginalFilename(fileName);

            log.info("Iniciando proxy de descarga para: {} (Original: {})", fileName, originalName);

            // 2. Solicitar los bytes a Supabase
            Request request = new Request.Builder()
                    .url(presignedUrl)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log.error("Supabase respondió con error al intentar descargar {}: {}", fileName, response.code());
                    return ResponseEntity.status(response.code()).build();
                }

                byte[] fileBytes = response.body().bytes();
                String contentType = response.header("Content-Type", "application/octet-stream");

                // 3. Devolver los bytes directamente al cliente con los headers correctos
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalName + "\"")
                        .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                        .body(fileBytes);
            }

        } catch (IOException e) {
            log.error("Error de E/S al descargar archivo {}: {}", fileName, e.getMessage());
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            log.error("Error inesperado al descargar archivo {}: {}", fileName, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
