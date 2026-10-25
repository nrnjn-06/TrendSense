package com.socialanalytics.dao;

import com.socialanalytics.model.Comment;
import java.util.List;

/**
 * DAO interface for managing normalized social media comments and sentiment annotations.
 */
public interface CommentDAO {
    void save(Comment comment);
    void saveAll(List<Comment> comments);
    List<Comment> findByPostId(String postId);
    List<Comment> findByUserId(Long userId);
    void updateSentiment(String commentId, Comment.SentimentType sentiment, double score);
}
