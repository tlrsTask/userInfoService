package org.userservice.service.details;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.userservice.dto.details.UserDetailsRequestDto;
import org.userservice.dto.details.UserDetailsResponseDto;
import org.userservice.entity.User;
import org.userservice.entity.UserDetails;
import org.userservice.entity.UserPhoto;
import org.userservice.exception.PhotoServiceException;
import org.userservice.service.photo.UserPhotoService;
import java.util.UUID;

/**
 * Сервис для управления профилем пользователя, включающим основные данные и фото.
 * <p>
 * Поддерживает создание, получение, обновление и удаление профиля пользователя.
 * Использует кеширование для ускорения получения данных и механизм повторных попыток (retry) при работе с внешним хранилищем фотографий.
 * </p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserDetailsService {
    private final UserCrudService userCrudService;
    private final UserPhotoService userPhotoService;

    /**
     * Создаёт или обновляет профиль пользователя с указанным ID, включая загрузку фото.
     * <p>
     * В случае ошибок при работе с сервисом фотографий выполняется повторная попытка до 3 раз с экспоненциальной задержкой.
     * Если профиль для пользователя отсутствует, создаётся новый.
     * </p>
     *
     * @param userId        ID пользователя
     * @param detailsRequest DTO с данными профиля для сохранения
     * @param photoFile     файл фотографии пользователя, может быть null или пустым
     * @return DTO с сохранёнными данными профиля, включая URL фото, если оно загружено
     */
    @Retryable(
            value = {PhotoServiceException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public UserDetailsResponseDto createProfile(UUID userId, UserDetailsRequestDto detailsRequest, MultipartFile photoFile) {
        log.info("Creating profile for user {}", userId);
        User user = userCrudService.getUserById(userId);
        UserDetails userDetails = userCrudService.getUserDetailsForUpdate(userId)
                .orElseGet(() -> {
                    log.info("No existing user details. Creating new profile for user {}", userId);
                    UserDetails newUserDetails = new UserDetails();
                    newUserDetails.setUser(user);
                    return newUserDetails;
                });
        userDetails.setFirstName(detailsRequest.firstName());
        userDetails.setLastName(detailsRequest.lastName());
        userDetails.setMiddleName(detailsRequest.middleName());
        userDetails.setBirthDate(detailsRequest.birthDate());
        userDetails.setEmail(detailsRequest.email());
        userDetails.setPhone(detailsRequest.phone());
        log.info("Mapping UserDetails without photo");
        UserDetails savedDetails = userCrudService.saveUserDetails(userDetails);
        log.info("User details saved with ID: {}", savedDetails.getId());
        if (photoFile != null && !photoFile.isEmpty()) {
            String filePath = userPhotoService.generateFileName(userId, photoFile);
            userPhotoService.uploadPhotoToStorage(filePath, photoFile);
            UserPhoto photo = userPhotoService.createPhotoEntity(savedDetails, filePath);
            savedDetails.setPhoto(photo);
        }
        return toUserDetailsDto(userCrudService.saveUserDetails(savedDetails));
    }

    /**
     * Получает профиль пользователя по ID с кешированием результата.
     * @param id ID пользователя
     * @return DTO с данными профиля и URL фотографии (если есть)
     * @throws EntityNotFoundException если профиль пользователя не найден
     */
    @Cacheable(value = "userDetails", key = "#id")
    public UserDetailsResponseDto getUserDetailsByUserId(UUID id) {
        UserDetails userDetails = userCrudService.getUserDetailsWithPhoto(id);
        return toUserDetailsDto(userDetails);
    }

    /**
     * Обновляет данные профиля пользователя с указанным ID.
     * <p>
     * При возникновении ошибок оптимистической блокировки выполняет до 3 повторных попыток.
     * Очищает кеш по ключу пользователя после успешного обновления.
     * </p>
     * @param id               ID пользователя
     * @param detailsForUpdate DTO с новыми данными для обновления
     * @return DTO с обновлёнными данными профиля
     * @throws EntityNotFoundException если профиль не найден для обновления
     * @throws OptimisticLockingFailureException при конфликте обновления (будет выполнено автоматическое повторение вызова)
     */
    @Retryable(value = {OptimisticLockingFailureException.class}, maxAttempts = 3)
    @CacheEvict(value = "userDetails", key = "#id")
    public UserDetailsResponseDto updateUserDetails(UUID id, UserDetailsRequestDto detailsForUpdate) {
        UserDetails userDetails = userCrudService.getUserDetailsForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Details not found for update: " + id));
        updateDetailsFromDto(userDetails, detailsForUpdate);
        return toUserDetailsDto(userCrudService.saveUserDetails(userDetails));
    }

    /**
     * Удаляет профиль пользователя и связанное фото по ID.
     * Очищает кеш по ключу пользователя.
     * @param id ID пользователя
     */
    @CacheEvict(value = "userDetails", key = "#id")
    public void deleteUserProfile(UUID id) {
        userPhotoService.deleteUserPhoto(id);
        userCrudService.deleteProfile(id);
    }

    /**
     * Получает ID пользователя по его username.
     * @param username имя пользователя (уникальное)
     * @return UUID пользователя
     * @throws EntityNotFoundException если пользователь не найден
     */
    public UUID getCurrentUserId(String username) {
        return userCrudService.getUserByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"))
                .getId();
    }

    private void updateDetailsFromDto(UserDetails userDetails, UserDetailsRequestDto dto) {
        userDetails.setFirstName(dto.firstName());
        userDetails.setLastName(dto.lastName());
        userDetails.setMiddleName(dto.middleName());
        userDetails.setBirthDate(dto.birthDate());
        userDetails.setEmail(dto.email());
        userDetails.setPhone(dto.phone());
    }

    private UserDetailsResponseDto toUserDetailsDto(UserDetails userDetails) {
        if (userDetails == null) return null;
        String photoUrl = null;
        if (userDetails.getPhoto() != null) {
            photoUrl = userDetails.getPhoto().getFilePath();
        }

        return new UserDetailsResponseDto(
                userDetails.getId(),
                userDetails.getFirstName(),
                userDetails.getLastName(),
                userDetails.getMiddleName(),
                userDetails.getBirthDate(),
                userDetails.getPhone(),
                userDetails.getEmail(),
                photoUrl
        );
    }
}
