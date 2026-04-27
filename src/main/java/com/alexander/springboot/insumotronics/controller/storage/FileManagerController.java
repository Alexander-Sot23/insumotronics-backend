package com.alexander.springboot.insumotronics.controller.storage;

/*
 * Este controlador funciona tanto para los usuarios con rol STUDENT, TEACHER y ADMIN
 */

import com.alexander.springboot.insumotronics.service.storage.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileNotFoundException;

@RestController
@RequestMapping("/api/student/files")
public class FileManagerController {

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping("/view-file")
    public ResponseEntity<Resource> viewFile(@RequestParam("fileName") String fileName){
        try {
            var fileToDownload = fileStorageService.getDownloadFile(fileName);
            String originalFileName = fileStorageService.extractOriginalFilename(fileName);

            MediaType mediaType = determineMediaType(fileName);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + originalFileName + "\"")
                    .contentLength(fileToDownload.length())
                    .contentType(mediaType)
                    .body(new FileSystemResource(fileToDownload));

        } catch (FileNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private MediaType determineMediaType(String fileName){
        if (fileName.toLowerCase().endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        } else if (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        } else if (fileName.toLowerCase().endsWith(".pdf")) {
            return MediaType.APPLICATION_PDF;
        } else {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}

