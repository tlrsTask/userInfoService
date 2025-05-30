package org.userservice.dto.details;

import java.time.LocalDate;
import java.util.UUID;

public record UserDetailsResponseDto(
        UUID id,
        String firstName,
        String lastName,
        String middleName,
        LocalDate birthDate,
        String email,
        String phone,
        String photoUrl
) {
}
