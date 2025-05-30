package org.userservice.controller;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.userservice.dto.details.UserDetailsRequestDto;
import org.userservice.dto.details.UserDetailsResponseDto;
import org.userservice.service.details.UserDetailsService;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserDetailsController {

    private final UserDetailsService userService;

    public UserDetailsController(UserDetailsService userService) {
        this.userService = userService;
    }

    @PostMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserDetailsResponseDto> createProfile(
            @Valid @RequestPart("profile") UserDetailsRequestDto detailsRequest,
            @RequestPart("photo") MultipartFile photoFile,
            Principal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        UUID userId = userService.getCurrentUserId(principal.getName());
        return ResponseEntity.ok(userService.createProfile(userId, detailsRequest, photoFile));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDetailsResponseDto> getUserDetails(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserDetailsByUserId(id));
    }

    @PutMapping("/{id}/details")
    public ResponseEntity<UserDetailsResponseDto> updateDetails(@PathVariable UUID id,
                                                                @Valid @RequestBody UserDetailsRequestDto detailsForUpdate) {
        return ResponseEntity.ok(userService.updateUserDetails(id, detailsForUpdate));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserDetails(@PathVariable UUID id) {
        userService.deleteUserProfile(id);
        return ResponseEntity.noContent().build();
    }
}
