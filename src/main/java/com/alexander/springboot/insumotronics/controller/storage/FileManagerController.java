package com.alexander.springboot.insumotronics.controller.storage;

/*
 * Este controlador funciona tanto para los usuarios con rol STUDENT, TEACHER y ADMIN
 */

import com.alexander.springboot.insumotronics.service.storage.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/files")
public class FileManagerController {

    private final FileStorageService fileStorageService;

    public FileManagerController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * Devuelve una URL firmada para ver el archivo (imágenes, PDFs, etc.)
     */
    @GetMapping("/view-file")
    public ResponseEntity<String> viewFile(@RequestParam("fileName") String fileName) {
        try {
            // URL válida por 60 minutos (ajusta según necesites)
            String presignedUrl = fileStorageService.getPresignedUrl(fileName, 60);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "text/plain")
                    .body(presignedUrl);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/download-file")
    public ResponseEntity<Resource> downloadFile(@RequestParam("fileName") String fileName) {
        try {
            String presignedUrl = fileStorageService.getPresignedUrl(fileName, 10); // 10 minutos es suficiente para descarga

            // Redirigimos al cliente a la URL de MinIO
            return ResponseEntity.ok()
                    .header(HttpHeaders.LOCATION, presignedUrl)
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Método auxiliar para determinar el tipo de archivo
     */
    private MediaType determineMediaType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (lower.endsWith(".pdf")) return MediaType.APPLICATION_PDF;
        if (lower.endsWith(".mp4")) return MediaType.parseMediaType("video/mp4");
        if (lower.endsWith(".svg")) return MediaType.parseMediaType("image/svg+xml");
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}