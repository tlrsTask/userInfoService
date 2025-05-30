package org.userservice.service.photo;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.userservice.exception.InvalidFileException;

import java.util.List;

@Service
public class FileValidationService {
    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "image/gif");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    public void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidFileException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidFileException("File size exceeds maximum limit of 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new InvalidFileException("Only JPG, PNG and GIF images are allowed");
        }
    }
}
