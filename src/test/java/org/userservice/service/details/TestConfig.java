package org.userservice.service.details;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.userservice.service.photo.FileValidationService;
import org.userservice.service.photo.MinioFileStorageService;
import org.userservice.service.photo.UserPhotoCrudService;
import org.userservice.service.photo.UserPhotoService;
import org.userservice.utils.UserDetailsMapper;
import org.userservice.utils.UserPhotoMapper;

import static org.mockito.Mockito.mock;

@Configuration
public class TestConfig {

    @Bean
    @Primary
    public UserCrudService userCrudService() {
        return mock(UserCrudService.class);
    }

    @Bean
    @Primary
    public UserDetailsMapper userDetailsMapper() {
        return mock(UserDetailsMapper.class);
    }

    @Bean
    @Primary
    public UserPhotoService userPhotoService() {
        return mock(UserPhotoService.class);
    }

    @Bean
    @Primary
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("userDetails");
    }

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
