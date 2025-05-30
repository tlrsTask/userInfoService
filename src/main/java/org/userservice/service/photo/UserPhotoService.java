package org.userservice.service.photo;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.userservice.entity.UserDetails;
import org.userservice.entity.UserPhoto;
import org.userservice.exception.PhotoServiceException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;


/**
 * Сервис для управления фотографиями пользователей.
 * Отвечает за загрузку, удаление, хранение и получение фотографий,
 * взаимодействуя с MinIO и базой данных через вспомогательные сервисы.
 *
 * <p>Основные функции:
 * <ul>
 *     <li>Загрузка новой фотографии с валидацией и заменой предыдущей</li>
 *     <li>Получение фотографии по ID пользователя</li>
 *     <li>Создание сущности {@link UserPhoto}</li>
 *     <li>Удаление фотографии из хранилища и БД</li>
 *     <li>Генерация pre-signed URL для доступа к фотографии</li>
 * </ul>
 *
 * <p>Класс является транзакционным и логирует ключевые действия.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class UserPhotoService {
    private final MinioFileStorageService fileStorageService;
    private final FileValidationService fileValidationService;
    private final UserPhotoCrudService userPhotoCrudService;

    /**
     * Загружает фотографию пользователя, валидирует файл, сохраняет его в хранилище и обновляет запись в БД.
     * При наличии предыдущей фотографии — она удаляется из хранилища.
     * @param userId ID пользователя
     * @param file   файл изображения
     * @return сохранённая сущность {@link UserPhoto}
     * @throws PhotoServiceException если произошла ошибка чтения или загрузки файла
     */
    public UserPhoto uploadUserPhoto(UUID userId, MultipartFile file) {
        log.info("Uploading photo for user {}", userId);
        fileValidationService.validateImageFile(file);
        log.info("File validation passed");
        String filePath = generateFileName(userId, file);
        log.info("Generated file path: {}", filePath);
        try (InputStream inputStream = file.getInputStream()) {
            fileStorageService.uploadFile(filePath, inputStream, file.getSize(), file.getContentType());
            log.info("File successfully uploaded to MinIO");
            UserPhoto existingPhoto = null;
            try {
                existingPhoto = userPhotoCrudService.getPhotoForUser(userId);
            } catch (EntityNotFoundException ignored) {
            }
            UserPhoto saved = userPhotoCrudService.updateOrCreateUserPhoto(userId, filePath);
            log.info("Photo record saved in DB: {}", saved.getFilePath());
            if (existingPhoto != null && !existingPhoto.getFilePath().equals(filePath)) {
                try {
                    fileStorageService.deleteFile(existingPhoto.getFilePath());
                } catch (Exception e) {
                    log.warn("Failed to delete old photo file: {}", existingPhoto.getFilePath(), e);
                }
            }
            return saved;
        } catch (IOException e) {
            log.error("IOException during upload", e);
            throw new PhotoServiceException("Error reading uploaded file", e);
        } catch (Exception e) {
            log.error("Unexpected error during photo upload", e);
            throw new PhotoServiceException("Error uploading photo", e);
        }
    }

    /**
     * Возвращает байтовый массив фотографии по ID пользователя.
     * @param userId ID пользователя
     * @return содержимое файла в виде byte[]
     * @throws EntityNotFoundException если фото не найдено
     */
    public byte[] findPhotoByUserId(UUID userId) {
        UserPhoto userPhoto = userPhotoCrudService.getPhotoForUser(userId);
        return fileStorageService.getFile(userPhoto.getFilePath());
    }

    /**
     * Загружает фото в MinIO без создания записи в БД. Используется как вспомогательный метод.
     * @param filePath путь к файлу (ключ в хранилище)
     * @param file     файл изображения
     * @throws PhotoServiceException если произошла ошибка при загрузке
     */
    public void uploadPhotoToStorage(String filePath, MultipartFile file) {
        fileValidationService.validateImageFile(file);
        try (InputStream inputStream = file.getInputStream()) {
            fileStorageService.uploadFile(filePath, inputStream, file.getSize(), file.getContentType());
        } catch (IOException e) {
            throw new PhotoServiceException("Error uploading photo", e);
        }
    }

    /**
     * Создаёт сущность {@link UserPhoto} в базе данных без загрузки файла в хранилище.
     *
     * @param userDetails сущность {@link UserDetails}
     * @param filePath    путь к файлу
     * @return созданная сущность {@link UserPhoto}
     */
    public UserPhoto createPhotoEntity(UserDetails userDetails, String filePath) {
        return userPhotoCrudService.createPhotoEntity(userDetails, filePath);
    }

    /**
     * Удаляет фотографию пользователя из хранилища и БД.
     * @param userId ID пользователя
     * @throws EntityNotFoundException если фотография не найдена
     */
    public void deleteUserPhoto(UUID userId) {
        UserPhoto userPhoto = userPhotoCrudService.getPhotoForUser(userId);
        fileStorageService.deleteFile(userPhoto.getFilePath());
        userPhotoCrudService.deleteUserPhoto(userPhoto);
    }

    /**
     * Генерирует pre-signed URL для временного доступа к фотографии.
     * @param filePath путь к файлу в хранилище
     * @return URL с ограниченным сроком действия
     */
    public String getPresignedPhotoUrl(String filePath) {
        return fileStorageService.generatePresignedUrl(filePath);
    }

    /**
     * Генерирует уникальное имя файла с учётом ID пользователя и расширения файла.
     * @param userId ID пользователя
     * @param file   загружаемый файл
     * @return уникальный путь к файлу
     * @throws PhotoServiceException если имя файла отсутствует
     */
    public String generateFileName(UUID userId, MultipartFile file) {
        String originalFilename = Optional.ofNullable(file.getOriginalFilename())
                .orElseThrow(() -> new PhotoServiceException("Filename is missing"));
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        return userId + "/" + UUID.randomUUID() + extension;
    }
}