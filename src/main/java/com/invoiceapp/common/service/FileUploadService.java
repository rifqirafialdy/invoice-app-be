package com.invoiceapp.common.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface FileUploadService {
    /**
     * @param file File yang diunggah.
     * @param allowedTypes Array string tipe MIME yang diizinkan (misalnya, "image/jpeg").
     * @param maxSizeKB Ukuran maksimum dalam Kilobyte (KB).
     * @return URL publik dari file yang diunggah.
     * @throws IOException Jika terjadi kesalahan I/O saat upload.
     */
    String uploadFile(MultipartFile file, String[] allowedTypes, long maxSizeKB) throws IOException;
}