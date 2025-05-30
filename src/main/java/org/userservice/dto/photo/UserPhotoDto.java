package org.userservice.dto.photo;

import java.util.UUID;

public record UserPhotoDto(UUID id,
                           UUID userId,
                           String filePath,
                           String url) {
}
