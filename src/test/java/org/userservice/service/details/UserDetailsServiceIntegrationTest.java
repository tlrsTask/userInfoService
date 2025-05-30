package org.userservice.service.details;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;
import org.userservice.dto.details.UserDetailsRequestDto;
import org.userservice.dto.details.UserDetailsResponseDto;
import org.userservice.entity.User;
import org.userservice.entity.UserDetails;
import org.userservice.entity.UserPhoto;

import org.userservice.exception.PhotoServiceException;
import org.userservice.service.photo.UserPhotoService;
import org.userservice.utils.UserDetailsMapper;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@SpringBootTest(classes = {TestConfig.class, UserDetailsService.class})
@EnableCaching
@EnableRetry
class UserDetailsServiceIntegrationTest {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private UserCrudService userCrudService;

    @Autowired
    private UserPhotoService userPhotoService;

    @Autowired
    private UserDetailsMapper userDetailsMapper;

    @Autowired
    private CacheManager cacheManager;

    private final UUID userId = UUID.randomUUID();
    private final String username = "testuser";
    private UserDetails userDetails;
    private UserDetailsResponseDto responseDto;
    private UserDetailsRequestDto requestDto;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(userId);
        user.setUserName(username);
        user.setPasswordHash("test-password-hash");

        UserPhoto userPhoto = new UserPhoto();
        userPhoto.setId(UUID.randomUUID());
        userPhoto.setUserDetails(userDetails);

        userDetails = new UserDetails();
        userDetails.setUser(user);
        userDetails.setPhoto(userPhoto);
        userPhoto.setUserDetails(userDetails);
        responseDto = new UserDetailsResponseDto(
                userId, "John", "Doe", null,
                LocalDate.of(1990, 1, 1),
                "john@example.com", "+123456789", "photo-url"
        );
        requestDto = new UserDetailsRequestDto(
                "John", "Doe", null,
                LocalDate.of(1990, 1, 1),
                "john@example.com", "+123456789"
        );
        cacheManager.getCache("userDetails").clear();
        when(userCrudService.getUserDetailsWithPhoto(any(UUID.class))).thenReturn(userDetails);
        when(userDetailsMapper.toUserDetailsDto(any(UserDetails.class))).thenReturn(responseDto);
        when(userCrudService.saveUserDetails(any(UserDetails.class))).thenReturn(userDetails);
    }

    @Test
    void getUserDetailsByUserId_shouldReturnCachedData() {
        userDetails.setFirstName("John");
        userDetails.setLastName("Doe");
        userDetails.setBirthDate(LocalDate.of(1990, 1, 1));
        userDetails.setEmail("john@example.com");
        userDetails.setPhone("+123456789");

        when(userCrudService.getUserDetailsWithPhoto(userId)).thenReturn(userDetails);

        UserDetailsResponseDto result1 = userDetailsService.getUserDetailsByUserId(userId);
        UserDetailsResponseDto result2 = userDetailsService.getUserDetailsByUserId(userId);

        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals("John", result1.firstName());
        assertEquals("Doe", result1.lastName());
        verify(userCrudService, times(1)).getUserDetailsWithPhoto(userId);
        assertNotNull(cacheManager.getCache("userDetails").get(userId));
    }

    @Test
    void updateUserDetails_shouldEvictCache() {
        when(userCrudService.getUserDetailsForUpdate(userId)).thenReturn(Optional.of(userDetails));
        when(userCrudService.saveUserDetails(userDetails)).thenReturn(userDetails);

        UserDetailsResponseDto result = userDetailsService.updateUserDetails(userId, requestDto);
        assertNotNull(result);
        assertNull(cacheManager.getCache("userDetails").get(userId));
        verify(userCrudService).getUserDetailsForUpdate(userId);
        verify(userCrudService).saveUserDetails(userDetails);
    }

    @Test
    void getCurrentUserId_shouldReturnUserId() {
        User user = new User();
        user.setId(userId);
        when(userCrudService.getUserByUsername(username)).thenReturn(Optional.of(user));
        UUID result = userDetailsService.getCurrentUserId(username);
        assertEquals(userId, result);
    }

    @Test
    void getCurrentUserId_shouldThrowWhenUserNotFound() {
        when(userCrudService.getUserByUsername(username)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> {
            userDetailsService.getCurrentUserId(username);
        });
    }

    @Test
    void createProfile_shouldCreateProfileThenAddPhotoAfterRetries() {
        MultipartFile photoFile = mock(MultipartFile.class);
        when(photoFile.isEmpty()).thenReturn(false);
        User user = new User();
        user.setId(userId);
        when(userCrudService.getUserById(userId)).thenReturn(user);
        when(userCrudService.getUserDetailsForUpdate(userId)).thenReturn(Optional.empty());
        String filePath = "user_photos/" + userId + "_profile.jpg";
        when(userPhotoService.generateFileName(eq(userId), any(MultipartFile.class)))
                .thenReturn(filePath);
        doThrow(new PhotoServiceException("MinIO error"))
                .doNothing()
                .when(userPhotoService).uploadPhotoToStorage(eq(filePath), any(MultipartFile.class));
        UserPhoto photo = new UserPhoto();
        when(userPhotoService.createPhotoEntity(any(UserDetails.class), eq(filePath)))
                .thenReturn(photo);
        UserDetailsResponseDto result = userDetailsService.createProfile(userId, requestDto, photoFile);
        assertNotNull(result);
        verify(userPhotoService, times(2)).uploadPhotoToStorage(anyString(), any(MultipartFile.class));
    }

}