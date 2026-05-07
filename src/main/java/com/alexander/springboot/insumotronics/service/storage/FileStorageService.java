package com.alexander.springboot.insumotronics.service.storage;

import com.alexander.springboot.insumotronics.enums.FileType;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public interface FileStorageService {
    String saveFile(MultipartFile file) throws IOException;
    File getDownloadFile(String fileName) throws FileNotFoundException;
    boolean deleteFile(String fileName) throws IOException;
    FileType extractExtension(String filename);
    String extractOriginalFilename(String filename);
    String getPresignedUrl(String fileName, int expiryMinutes);
}
