package com.socialanalytics;

import com.socialanalytics.analytics.LexiconSentimentAnalyzer;
import com.socialanalytics.model.Comment;
import com.socialanalytics.model.SentimentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SentimentAnalyzerTest {
    private LexiconSentimentAnalyzer analyzer;

    @BeforeEach
    public void setup() {
        analyzer = new LexiconSentimentAnalyzer();
    }

    @Test
    public void testPositiveClassification() {
        String text = "This is an amazing, excellent and brilliant feature! Really love it.";
        assertEquals(Comment.SentimentType.POSITIVE, analyzer.classify(text));
        assertTrue(analyzer.analyzeSentiment(text) > 0.3);
    }

    @Test
    public void testNegativeClassification() {
        String text = "Terrible experience, horrible bug and poor customer service. Truly awful.";
        assertEquals(Comment.SentimentType.NEGATIVE, analyzer.classify(text));
        assertTrue(analyzer.analyzeSentiment(text) < -0.3);
    }

    @Test
    public void testNeutralClassification() {
        String text = "The meeting is scheduled at 4 PM in room 3B.";
        assertEquals(Comment.SentimentType.NEUTRAL, analyzer.classify(text));
        assertEquals(0.0, analyzer.analyzeSentiment(text), 0.05);
    }

    @Test
    public void testNegationInversion() {
        String positive = "This software is good and helpful.";
        String negated = "This software is not good and not helpful.";

        assertTrue(analyzer.analyzeSentiment(positive) > 0);
        assertTrue(analyzer.analyzeSentiment(negated) < 0, "Negation 'not good' should flip polarity to negative");
    }

    @Test
    public void testBatchSentimentDistribution() {
        List<Comment> comments = new ArrayList<>();

        Comment c1 = new Comment("1", "p1", null, "User1", "Awesome and wonderful update!", null);
        Comment c2 = new Comment("2", "p1", null, "User2", "Great and useful tutorial.", null);
        Comment c3 = new Comment("3", "p1", null, "User3", "This is an informative post.", null);
        Comment c4 = new Comment("4", "p1", null, "User4", "Terrible waste of time and broken.", null);

        comments.add(c1);
        comments.add(c2);
        comments.add(c3);
        comments.add(c4);

        SentimentResult result = analyzer.analyzeBatch(null, comments);

        assertEquals(4, result.getTotalAnalyzed());
        assertEquals(2, result.getPositiveCount()); // c1, c2
        assertEquals(1, result.getNeutralCount());  // c3
        assertEquals(1, result.getNegativeCount()); // c4

        assertEquals(50.0, result.getPositivePercent(), 0.01);
        assertEquals(25.0, result.getNeutralPercent(), 0.01);
        assertEquals(25.0, result.getNegativePercent(), 0.01);
        assertEquals("Positive", result.getDominantSentiment());
    }
}
