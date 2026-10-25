package com.socialanalytics.dao;

import com.socialanalytics.model.Platform;
import com.socialanalytics.model.SocialAccount;
import java.util.List;
import java.util.Optional;

/**
 * DAO interface for connected social media accounts.
 */
public interface SocialAccountDAO {
    SocialAccount save(SocialAccount account);
    void update(SocialAccount account);
    Optional<SocialAccount> findById(Long id);
    List<SocialAccount> findByUserId(Long userId);
    Optional<SocialAccount> findByUserAndPlatform(Long userId, Platform platform);
    void delete(Long id);
    void disconnectAll(Long userId);
}
