package org.userservice.security.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.userservice.dto.security.RegisterRequest;
import org.userservice.entity.User;
import org.userservice.exception.UserAlreadyExistsException;
import org.userservice.repo.UserRepository;
import org.userservice.security.interfaces.IUserOperations;
import org.userservice.security.jwt.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.userservice.utils.UserMapper;
import org.userservice.dto.UserDto;

@Service
@RequiredArgsConstructor
public class UserAccountManager implements IUserOperations {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    @Autowired(required = false)
    private TokenBlacklistService tokenBlacklistService;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserDto register(RegisterRequest req) {
        if (userRepository.findByUserName(req.username()).isPresent()) {
            throw new UserAlreadyExistsException("Username already exists");
        }
        User user = new User();
        user.setUserName(req.username());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        userRepository.save(user);
        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public void changePassword(String username, String newPassword) {
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        if (tokenBlacklistService != null) {
            tokenBlacklistService.revokeAll(user);
        }
    }
}
