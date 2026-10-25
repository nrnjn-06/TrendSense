package com.socialanalytics.analytics;

import com.socialanalytics.model.Comment;
import com.socialanalytics.model.Post;
import com.socialanalytics.model.SentimentResult;
import java.util.List;

/**
 * Pluggable interface for sentiment analysis.
 * Allows seamless replacement with an ML model or cloud NLP service
 * in the future without modifying downstream analytics code.
 */
public interface SentimentAnalyzer {

    /**
     * Analyzes sentiment for a single text snippet.
     *
     * @param text input text
     * @return polarity score between -1.0 (most negative) and +1.0 (most positive)
     */
    double analyzeSentiment(String text);

    /**
     * Classifies a comment and annotates its sentiment type and score.
     */
    Comment.SentimentType classify(String text);

    /**
     * Analyzes a collection of posts and comments, aggregating counts,
     * percentages, and frequent emotional keywords.
     */
    SentimentResult analyzeBatch(List<Post> posts, List<Comment> comments);
}
