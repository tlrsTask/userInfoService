package org.userservice.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.userservice.dto.photo.UserPhotoDto;
import org.userservice.entity.UserPhoto;
import org.userservice.service.photo.UserPhotoService;
import org.userservice.utils.UserPhotoMapper;

import java.util.UUID;

@RestController
@RequestMapping("/api/photos")
public class PhotoController {
    private final UserPhotoService photoService;
    private final UserPhotoMapper photoMapper;

    public PhotoController(UserPhotoService photoService, UserPhotoMapper photoMapper) {
        this.photoService = photoService;
        this.photoMapper = photoMapper;
    }

    @PostMapping("/{userId}")
    public ResponseEntity<UserPhotoDto> uploadPhoto(
            @PathVariable UUID userId,
            @RequestParam("file") MultipartFile file
    ) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }
        UserPhoto photo = photoService.uploadUserPhoto(userId, file);
        return ResponseEntity.ok(photoMapper.toDto(photo));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<byte[]> getPhoto(@PathVariable UUID userId) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(photoService.findPhotoByUserId(userId));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deletePhoto(@PathVariable UUID userId) {
        photoService.deleteUserPhoto(userId);
        return ResponseEntity.noContent().build();
    }
}
