package org.userservice.dto.photo;

import io.github.resilience4j.core.lang.Nullable;
import org.userservice.entity.UserPhoto;

public record PhotoUpdateResult(UserPhoto photo, @Nullable String oldPath) {}
