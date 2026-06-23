package com.logmonitor.repository;

import com.logmonitor.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link User} persistence operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by unique username.
     *
     * @param username the username to look up
     * @return optional containing the user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Finds a user by unique email address.
     *
     * @param email the email to look up
     * @return optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks whether a username is already registered.
     *
     * @param username the username to check
     * @return {@code true} if the username exists
     */
    boolean existsByUsername(String username);

    /**
     * Checks whether an email is already registered.
     *
     * @param email the email to check
     * @return {@code true} if the email exists
     */
    boolean existsByEmail(String email);
}
