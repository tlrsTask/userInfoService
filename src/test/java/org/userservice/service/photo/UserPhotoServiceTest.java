package org.userservice.service.photo;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.multipart.MultipartFile;
import org.userservice.entity.UserPhoto;
import org.userservice.exception.PhotoServiceException;
import org.userservice.utils.UserPhotoMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = { UserPhotoServiceIntegrationTest.TestConfigForUserPhotoService.class, UserPhotoService.class })
class UserPhotoServiceIntegrationTest {

    @Autowired
    private UserPhotoService userPhotoService;

    @Autowired
    private MinioFileStorageService minioFileStorageService;

    @Autowired
    private FileValidationService fileValidationService;

    @Autowired
    private UserPhotoCrudService userPhotoCrudService;

    private final UUID userId = UUID.randomUUID();

    private MultipartFile mockFile;

    @BeforeEach
    void setUp() {
        mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        try {
            when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("dummy".getBytes()));
        } catch (Exception e) {
            fail("Unexpected exception during setup: " + e.getMessage());
        }
    }

    @Test
    void uploadUserPhoto_shouldUploadSuccessfully() {
        doNothing().when(fileValidationService).validateImageFile(mockFile);
        String expectedPattern = userId + "/.*\\.jpg";
        when(userPhotoCrudService.getPhotoForUser(userId))
                .thenThrow(new EntityNotFoundException("Photo not found"));
        when(userPhotoCrudService.updateOrCreateUserPhoto(eq(userId), matches(expectedPattern)))
                .thenAnswer(invocation -> {
                    UserPhoto photo = new UserPhoto();
                    photo.setId(UUID.randomUUID());
                    photo.setFilePath(invocation.getArgument(1));
                    return photo;
                });
        UserPhoto result = userPhotoService.uploadUserPhoto(userId, mockFile);
        assertNotNull(result);
        assertTrue(result.getFilePath().matches(expectedPattern));
        verify(fileValidationService).validateImageFile(mockFile);
        verify(minioFileStorageService).uploadFile(
                matches(expectedPattern),
                any(InputStream.class),
                eq(1024L),
                eq("image/jpeg")
        );
        verify(userPhotoCrudService).updateOrCreateUserPhoto(eq(userId), matches(expectedPattern));
    }

    @Test
    void uploadUserPhoto_shouldDeleteOldPhotoIfExists() {
        String oldFilePath = userId + "/old.jpg";
        UserPhoto oldPhoto = new UserPhoto();
        oldPhoto.setId(UUID.randomUUID());
        oldPhoto.setFilePath(oldFilePath);
        doNothing().when(fileValidationService).validateImageFile(mockFile);
        when(userPhotoCrudService.getPhotoForUser(userId)).thenReturn(oldPhoto);
        String newFilePattern = userId + "/.*\\.jpg";
        when(userPhotoCrudService.updateOrCreateUserPhoto(eq(userId), matches(newFilePattern)))
                .thenAnswer(invocation -> {
                    UserPhoto photo = new UserPhoto();
                    photo.setId(UUID.randomUUID());
                    photo.setFilePath(invocation.getArgument(1));
                    return photo;
                });
        userPhotoService.uploadUserPhoto(userId, mockFile);
        verify(minioFileStorageService).deleteFile(oldFilePath);
    }

    @Test
    void findPhotoByUserId_shouldReturnPhotoBytes() {
        byte[] expectedBytes = "photo-content".getBytes();
        String filePath = "user_photos/" + userId + ".jpg";
        UserPhoto userPhoto = new UserPhoto();
        userPhoto.setId(UUID.randomUUID());
        userPhoto.setFilePath(filePath);
        when(userPhotoCrudService.getPhotoForUser(userId)).thenReturn(userPhoto);
        when(minioFileStorageService.getFile(filePath)).thenReturn(expectedBytes);
        byte[] result = userPhotoService.findPhotoByUserId(userId);
        assertArrayEquals(expectedBytes, result);
    }

    @Test
    void deleteUserPhoto_shouldDeleteFromStorageAndDb() {
        String filePath = "user_photos/" + userId + ".jpg";
        UserPhoto userPhoto = new UserPhoto();
        userPhoto.setId(UUID.randomUUID());
        userPhoto.setFilePath(filePath);
        when(userPhotoCrudService.getPhotoForUser(userId)).thenReturn(userPhoto);
        userPhotoService.deleteUserPhoto(userId);
        verify(minioFileStorageService).deleteFile(filePath);
        verify(userPhotoCrudService).deleteUserPhoto(userPhoto);
    }

    @Test
    void uploadUserPhoto_whenIOException_throwsPhotoServiceException() throws IOException {
        when(mockFile.getInputStream()).thenThrow(new IOException("Disk full"));
        PhotoServiceException exception = assertThrows(PhotoServiceException.class, () -> {
            userPhotoService.uploadUserPhoto(userId, mockFile);
        });
        assertTrue(exception.getCause() instanceof IOException);
        assertEquals("Error reading uploaded file", exception.getMessage());
    }

    @Configuration
    static class TestConfigForUserPhotoService {

        @Bean
        @Primary
        public MinioFileStorageService minioFileStorageService() {
            return mock(MinioFileStorageService.class);
        }

        @Bean
        @Primary
        public FileValidationService fileValidationService() {
            return mock(FileValidationService.class);
        }

        @Bean
        @Primary
        public UserPhotoCrudService userPhotoCrudService() {
            return mock(UserPhotoCrudService.class);
        }

        @Bean
        @Primary
        public UserPhotoMapper userPhotoMapper() {
            return mock(UserPhotoMapper.class);
        }
    }
}