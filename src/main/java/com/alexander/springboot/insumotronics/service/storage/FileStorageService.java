package com.alexander.springboot.insumotronics.service.storage;

import com.alexander.springboot.insumotronics.enums.FileType;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Interfaz de servicio de almacenamiento de archivos.
 *
 * Con la migración a Supabase Storage, el flujo principal es:
 *  1. saveFile(file) → sube el archivo y devuelve la URL pública completa
 *  2. La URL se guarda directamente en la entidad (Product.imageUrls / documentUrls)
 *  3. El frontend accede directamente a las URLs sin pasar por el backend
 *
 * getPresignedUrl() sigue disponible para generar URLs firmadas
 * en caso de buckets privados.
 */
public interface FileStorageService {

    /**
     * Sube un archivo al storage y retorna su URL pública completa.
     * Esta URL se guarda directamente en la base de datos.
     *
     * @param file archivo a subir
     * @return URL pública accesible del archivo
     */
    String saveFile(MultipartFile file) throws IOException;

    /**
     * Genera una URL firmada (con expiración) para un archivo.
     * Útil para buckets privados o accesos temporales.
     *
     * @param fileUrl URL pública o path del archivo
     * @param expiryMinutes minutos de validez
     * @return URL firmada con expiración
     */
    String getPresignedUrl(String fileUrl, int expiryMinutes);

    /**
     * Elimina un archivo del storage dado su URL completa.
     *
     * @param fileUrl URL pública del archivo a eliminar
     * @return true si se eliminó correctamente
     */
    boolean deleteFile(String fileUrl) throws IOException;

    /**
     * Extrae el FileType (enum) a partir del nombre o URL del archivo.
     */
    FileType extractExtension(String filename);

    /**
     * Extrae el nombre original del archivo quitando prefijos de timestamp/UUID.
     */
    String extractOriginalFilename(String fileUrl);

    /**
     * NO SOPORTADO en implementaciones basadas en cloud storage.
     * Se conserva por compatibilidad con la interfaz original.
     * @deprecated Usar las URLs directas devueltas por saveFile().
     */
    @Deprecated
    File getDownloadFile(String fileName) throws FileNotFoundException;
}
