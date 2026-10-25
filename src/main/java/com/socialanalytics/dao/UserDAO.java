package com.socialanalytics.dao;

import com.socialanalytics.model.User;
import java.util.Optional;

/**
 * Data Access Object interface for User entities.
 */
public interface UserDAO {
    User create(User user);
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    void updatePassword(Long userId, String passwordHash, String salt);
}
