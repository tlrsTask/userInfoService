package org.userservice.service.photo;

import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.userservice.exception.FileStorageException;
import org.userservice.exception.PhotoServiceException;
import io.minio.SetBucketPolicyArgs;
import io.minio.MinioClient;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.annotation.Timed;

/**
 * Сервис для работы с файловым хранилищем MinIO.
 * <p>
 * Обеспечивает создание бакета, загрузку, получение и удаление файлов (например, фото пользователей),
 * а также генерацию временных подписанных URL для доступа к файлам.
 * <p>
 * Включена поддержка повторных попыток (retry) с экспоненциальной задержкой и circuit breaker для устойчивости.
 * Метрики операций собираются с помощью Micrometer.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MinioFileStorageService {
    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucketName;

    /**
     * Инициализация сервиса.
     * Проверяет наличие бакета, создает его и устанавливает политику доступа при необходимости.
     */
    @PostConstruct
    public void init() {
        createBucketIfNotExists();
    }

    /**
     * Проверяет существование бакета в MinIO и создает его при отсутствии.
     * Устанавливает пользовательскую политику доступа.
     */
    private void createBucketIfNotExists() {
        log.info("Checking if bucket {} exists...", bucketName);
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                log.info("Bucket does not exist. Creating bucket {}", bucketName);
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                setCustomPolicy(bucketName);
            }
        } catch (Exception e) {
            log.error("Error creating bucket", e);
            throw new FileStorageException("Error creating bucket", e);
        }
    }

    /**
     * Устанавливает кастомную политику доступа из JSON-файла.
     *
     * @param bucketName имя бакета
     * @throws Exception если не удалось применить политику
     */
    private void setCustomPolicy(String bucketName) throws Exception {
        try {
            String policyJson = new String(
                    getClass().getResourceAsStream("/readwrite-policy.json").readAllBytes()
            );
            minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder()
                            .bucket(bucketName)
                            .config(policyJson)
                            .build()
            );
            log.info("Bucket policy set from custom JSON file");
        } catch (Exception e) {
            log.warn("Failed to apply bucket policy", e);
        }
    }

    /**
     * Загружает файл в MinIO.
     * Операция повторяется при ошибках с экспоненциальной задержкой.
     *
     * @param objectName  имя объекта в бакете
     * @param inputStream поток файла
     * @param size        размер файла в байтах
     * @param contentType MIME-тип файла
     * @return имя загруженного объекта
     * @throws FileStorageException при ошибках загрузки
     */
    @Retryable(
            value = {PhotoServiceException.class, FileStorageException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000)
    )
    @CircuitBreaker(name = "fileStorage", fallbackMethod = "uploadFallback")
    @Timed("user.photo.upload")
    public String uploadFile(String objectName, InputStream inputStream, long size, String contentType) {
        log.info("Uploading file: {}", objectName);
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );
            return objectName;
        } catch (Exception e) {
            throw new FileStorageException("Error uploading file", e);
        }
    }

    /**
     * Получает файл из MinIO.
     * Операция повторяется при ошибках с экспоненциальной задержкой.
     *
     * @param objectName имя объекта
     * @return массив байтов файла
     * @throws FileStorageException если файл не найден или произошла ошибка
     */
    @Retryable(
            value = {PhotoServiceException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000)
    )
    @CircuitBreaker(name = "fileStorage", fallbackMethod = "getPhotoFallback")
    @Timed("user.photo.get")
    public byte[] getFile(String objectName) {
        try (InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build())) {
            return inputStream.readAllBytes();
        } catch (Exception e) {
            throw new FileStorageException("The User has no photo ", e);
        }
    }


    /**
     * Удаляет файл из MinIO.
     * Операция повторяется при ошибках с экспоненциальной задержкой.
     *
     * @param objectName имя объекта
     * @throws FileStorageException при ошибках удаления
     */
    @Retryable(
            value = {PhotoServiceException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000)
    )
    @CircuitBreaker(name = "fileStorage", fallbackMethod = "deletePhotoFallback")
    @Timed("user.photo.delete")
    public void deleteFile(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
        } catch (Exception e) {
            throw new FileStorageException("Error deleting file", e);
        }
    }

    /**
     * Генерирует временную подписанную ссылку для доступа к файлу.
     *
     * @param objectName имя объекта
     * @return URL с ограниченным временем доступа (3 часа)
     * @throws FileStorageException при ошибках генерации URL
     */
    public String generatePresignedUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(3, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception e) {
            throw new FileStorageException("Error generating presigned URL", e);
        }
    }

    /**
     * Фоллбэк метод при ошибках загрузки файла.
     *
     * @param t          причина ошибки
     * @param objectName имя объекта
     * @return никогда не возвращает, всегда бросает исключение
     */
    @Timed("user.photo.upload.fallback")
    public String uploadFallback(Throwable t, String objectName) {
        log.error("Fallback triggered for uploadFile({}), reason: {}", objectName, t.getMessage(), t);
        throw new FileStorageException("MinIO service unavailable during upload", t);
    }

    /**
     * Фоллбэк метод при ошибках получения файла.
     *
     * @param e          причина ошибки
     * @param objectName имя объекта
     * @return никогда не возвращает, всегда бросает исключение
     */
    public byte[] getPhotoFallback(Throwable e, String objectName) {
        log.error("FALLBACK: MinIO get failed for object: {}", objectName, e);
        throw new FileStorageException("MinIO service unavailable during get", e);
    }


    /**
     * Фоллбэк метод при ошибках удаления файла.
     *
     * @param e          причина ошибки
     * @param objectName имя объекта
     */
    public void deletePhotoFallback(Throwable e, String objectName) {
        log.error("FALLBACK: MinIO delete failed for object: {}", objectName, e);
        throw new FileStorageException("MinIO service unavailable during delete", e);
    }
}
