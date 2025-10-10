package com.invoiceapp.common.service.implement;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.invoiceapp.common.exception.BadRequestException;
import com.invoiceapp.common.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryUploadService implements FileUploadService {

    private final Cloudinary cloudinary;

    @Override
    public String uploadFile(MultipartFile file, String[] allowedTypes, long maxSizeKB) throws IOException {

        if (file.isEmpty()) {
            throw new BadRequestException("File cannot be empty.");
        }

        long maxSizeBytes = maxSizeKB * 1024;
        if (file.getSize() > maxSizeBytes) {
            throw new BadRequestException("File size exceeds the limit of " + maxSizeKB + " KB (Max: " + (maxSizeKB / 1024) + " MB).");
        }

        String contentType = file.getContentType();
        if (contentType == null || Arrays.stream(allowedTypes).noneMatch(contentType::equalsIgnoreCase)) {
            throw new BadRequestException("Invalid file type. Only JPG, JPEG, PNG, and WebP images are allowed.");
        }

        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
            return (String) uploadResult.get("url");
        } catch (IOException e) {
            throw new IOException("Failed to upload file to external service: " + e.getMessage(), e);
        }
    }
}