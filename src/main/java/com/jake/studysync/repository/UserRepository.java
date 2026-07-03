package com.jake.studysync.repository;

import com.jake.studysync.model.AuthProvider;
import com.jake.studysync.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByProviderAndProviderId( AuthProvider provider, String providerId);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}