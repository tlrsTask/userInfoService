package org.userservice.service.details;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.userservice.entity.User;
import org.userservice.entity.UserDetails;
import org.userservice.exception.CustomServiceUnavailableException;
import org.userservice.repo.UserDetailsRepository;
import org.userservice.repo.UserPhotoRepository;
import org.userservice.repo.UserRepository;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserCrudService {
    private final UserRepository userRepository;
    private final UserDetailsRepository userDetailsRepository;
    private final UserPhotoRepository userPhotoRepository;

    @Transactional(rollbackFor = {DataAccessException.class, CustomServiceUnavailableException.class})
    public UserDetails saveUserDetails(UserDetails details) {
        try {
            return userDetailsRepository.save(details);
        } catch (DataAccessException e) {
            log.error("Database error saving user details: {}", e.getLocalizedMessage());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Transactional(readOnly = true)
    @EntityGraph(attributePaths = {"photo"})
    public UserDetails getUserDetailsWithPhoto(UUID userId) {
        log.debug("Acquiring lock for user details update: {}", userId);
        return userDetailsRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Details not found for user: " + userId));
    }

    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    public Optional<UserDetails> getUserDetailsForUpdate(UUID userId) {
        return userDetailsRepository.findByUserId(userId);
    }

    @Transactional(rollbackFor = DataAccessException.class)
    public void deleteProfile(UUID userId) {
        User user = getUserById(userId);
        userDetailsRepository.findByUser(user).ifPresent(userDetailsRepository::delete);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUserName(username);
    }


}
