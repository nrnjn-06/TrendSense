package com.socialanalytics.dao;

import com.socialanalytics.model.Platform;
import com.socialanalytics.model.Post;
import java.util.List;
import java.util.Optional;

/**
 * DAO interface for managing normalized social media posts and hashtags.
 */
public interface PostDAO {
    void save(Post post);
    void saveAll(List<Post> posts);
    Optional<Post> findById(String id);
    List<Post> findByAccountId(Long accountId);
    List<Post> findByUserId(Long userId);
    List<Post> findByPlatform(Long userId, Platform platform);
    void deleteByAccountId(Long accountId);
    long countByUserId(Long userId);
}
