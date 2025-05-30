package org.userservice.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.userservice.dto.details.UserDetailsResponseDto;
import org.userservice.entity.UserDetails;
import org.userservice.service.photo.UserPhotoService;

@Component
public class UserDetailsMapper {

    private final UserPhotoService userPhotoService;

    public UserDetailsMapper(@Autowired UserPhotoService userPhotoService) {
        this.userPhotoService = userPhotoService;
    }

    public UserDetailsResponseDto toUserDetailsDto(UserDetails userDetails) {
        String photoUrl = null;
        if (userDetails != null && userDetails.getPhoto() != null) {
            photoUrl = userPhotoService.getPresignedPhotoUrl(userDetails.getPhoto().getFilePath());
        }

        return new UserDetailsResponseDto(
                userDetails.getId(),
                userDetails.getFirstName(),
                userDetails.getLastName(),
                userDetails.getMiddleName(),
                userDetails.getBirthDate(),
                userDetails.getEmail(),
                userDetails.getPhone(),
                photoUrl
        );
    }
}
