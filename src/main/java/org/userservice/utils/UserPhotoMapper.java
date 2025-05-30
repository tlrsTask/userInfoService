package org.userservice.utils;

import org.springframework.stereotype.Component;
import org.userservice.dto.photo.UserPhotoDto;
import org.userservice.entity.UserPhoto;
import org.userservice.service.photo.MinioFileStorageService;

@Component
public class UserPhotoMapper {
    private final MinioFileStorageService fileStorageService;

    public UserPhotoMapper(MinioFileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    public UserPhotoDto toDto(UserPhoto photo) {
        return new UserPhotoDto(
                photo.getId(),
                photo.getUserDetails().getUser().getId(),
                photo.getFilePath(),
                fileStorageService.generatePresignedUrl(photo.getFilePath())
        );
    }
}