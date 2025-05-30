package org.userservice.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.userservice.entity.User;
import org.userservice.entity.UserDetails;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.Optional;
import java.util.UUID;

public interface UserDetailsRepository extends JpaRepository<UserDetails, UUID> {
    @EntityGraph(attributePaths = {"photo"})
    Optional<UserDetails> findByUserId(UUID userId);

    Optional<UserDetails> findByUser(User user);
}