package org.userservice.dto.details;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record UserDetailsRequestDto(
        @NotBlank(message = "First name is required")
        String firstName,
        @NotBlank(message = "Last name is required")
        String lastName,
        String middleName,
        @NotNull(message = "Birth date is required")
        LocalDate birthDate,
        @Email(message = "Invalid email format", regexp = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$|^(?!.*)$")
        String email,

        @Pattern(regexp = "^\\+?[0-9]{11}$|^$", message = "Phone number is invalid")
        String phone){
}
