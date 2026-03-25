package com.authservice.repositories;

import com.authservice.models.UserCredentials;
import io.netty.util.AsyncMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserCredentials, UUID> {
    Optional<UserCredentials> findByEmail(String username);

}
