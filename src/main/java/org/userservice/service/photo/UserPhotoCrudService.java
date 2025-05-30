package org.userservice.service.photo;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.userservice.entity.User;
import org.userservice.entity.UserDetails;
import org.userservice.entity.UserPhoto;
import org.userservice.repo.UserDetailsRepository;
import org.userservice.repo.UserPhotoRepository;
import org.userservice.repo.UserRepository;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserPhotoCrudService {
    private final UserRepository userRepository;
    private final UserDetailsRepository userDetailsRepository;
    private final UserPhotoRepository userPhotoRepository;

    @Transactional
    public UserPhoto updateOrCreateUserPhoto(UUID userId, String filePath) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        UserDetails details = userDetailsRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("UserDetails not found for user " + userId));
        return userPhotoRepository.findByUserDetailsId(details.getId())
                .map(existing -> {
                    existing.setFilePath(filePath);
                    return userPhotoRepository.save(existing);
                })
                .orElseGet(() -> {
                    UserPhoto newPhoto = new UserPhoto();
                    newPhoto.setUserDetails(details);
                    newPhoto.setFilePath(filePath);
                    return userPhotoRepository.save(newPhoto);
                });
    }
    @Transactional(readOnly = true)
    public UserPhoto getPhotoForUser(UUID userId) {
        UserDetails details = userDetailsRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("UserDetails not found for user " + userId));
        return userPhotoRepository.findByUserDetailsId(details.getId())
                .orElseThrow(() -> new EntityNotFoundException("Photo not found"));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public UserPhoto createPhotoEntity(UserDetails userDetails, String filePath) {
        UserPhoto photo = new UserPhoto();
        photo.setFilePath(filePath);
        photo.setUserDetails(userDetails);
        return userPhotoRepository.save(photo);
    }

    @Transactional
    public void deleteUserPhoto(UserPhoto photo) {
        userPhotoRepository.delete(photo);
    }
}
