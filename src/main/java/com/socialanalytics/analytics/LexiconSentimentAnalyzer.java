package com.socialanalytics.analytics;

import com.socialanalytics.model.Comment;
import com.socialanalytics.model.Post;
import com.socialanalytics.model.SentimentResult;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Lightweight, lexicon/rule-based sentiment analyzer implemented in pure Java.
 * Detects word polarities, negation flips ("not good"), and calculates percentage breakdowns.
 */
public class LexiconSentimentAnalyzer implements SentimentAnalyzer {

    private static final Map<String, Double> POSITIVE_LEXICON = new HashMap<>();
    private static final Map<String, Double> NEGATIVE_LEXICON = new HashMap<>();
    private static final Set<String> NEGATORS = new HashSet<>(Arrays.asList(
            "not", "no", "never", "hardly", "barely", "scarcely", "cannot", "can't", "won't", "don't", "doesn't", "isn't", "aren't"
    ));

    static {
        // Positive words and intensities
        POSITIVE_LEXICON.put("amazing", 2.0);
        POSITIVE_LEXICON.put("excellent", 2.0);
        POSITIVE_LEXICON.put("outstanding", 2.0);
        POSITIVE_LEXICON.put("brilliant", 2.0);
        POSITIVE_LEXICON.put("love", 1.8);
        POSITIVE_LEXICON.put("great", 1.5);
        POSITIVE_LEXICON.put("awesome", 1.8);
        POSITIVE_LEXICON.put("fantastic", 1.8);
        POSITIVE_LEXICON.put("useful", 1.4);
        POSITIVE_LEXICON.put("helpful", 1.4);
        POSITIVE_LEXICON.put("good", 1.0);
        POSITIVE_LEXICON.put("best", 1.8);
        POSITIVE_LEXICON.put("clean", 1.0);
        POSITIVE_LEXICON.put("perfect", 2.0);
        POSITIVE_LEXICON.put("innovative", 1.5);
        POSITIVE_LEXICON.put("impressive", 1.6);
        POSITIVE_LEXICON.put("valuable", 1.4);
        POSITIVE_LEXICON.put("smart", 1.2);
        POSITIVE_LEXICON.put("fast", 1.1);
        POSITIVE_LEXICON.put("efficient", 1.3);
        POSITIVE_LEXICON.put("smooth", 1.1);
        POSITIVE_LEXICON.put("insightful", 1.5);
        POSITIVE_LEXICON.put("excited", 1.4);
        POSITIVE_LEXICON.put("appreciate", 1.3);
        POSITIVE_LEXICON.put("super", 1.4);
        POSITIVE_LEXICON.put("cool", 1.0);

        // Negative words and intensities
        NEGATIVE_LEXICON.put("terrible", -2.0);
        NEGATIVE_LEXICON.put("horrible", -2.0);
        NEGATIVE_LEXICON.put("awful", -2.0);
        NEGATIVE_LEXICON.put("hate", -2.0);
        NEGATIVE_LEXICON.put("broken", -1.8);
        NEGATIVE_LEXICON.put("useless", -1.8);
        NEGATIVE_LEXICON.put("worst", -2.0);
        NEGATIVE_LEXICON.put("bad", -1.5);
        NEGATIVE_LEXICON.put("poor", -1.5);
        NEGATIVE_LEXICON.put("garbage", -1.8);
        NEGATIVE_LEXICON.put("trash", -1.8);
        NEGATIVE_LEXICON.put("slow", -1.1);
        NEGATIVE_LEXICON.put("bug", -1.0);
        NEGATIVE_LEXICON.put("buggy", -1.4);
        NEGATIVE_LEXICON.put("crash", -1.6);
        NEGATIVE_LEXICON.put("crashes", -1.6);
        NEGATIVE_LEXICON.put("error", -1.0);
        NEGATIVE_LEXICON.put("fail", -1.4);
        NEGATIVE_LEXICON.put("failed", -1.4);
        NEGATIVE_LEXICON.put("failure", -1.5);
        NEGATIVE_LEXICON.put("disappoint", -1.5);
        NEGATIVE_LEXICON.put("disappointing", -1.6);
        NEGATIVE_LEXICON.put("waste", -1.5);
        NEGATIVE_LEXICON.put("annoying", -1.3);
        NEGATIVE_LEXICON.put("frustrating", -1.5);
        NEGATIVE_LEXICON.put("flawed", -1.2);
    }

    @Override
    public double analyzeSentiment(String text) {
        if (text == null || text.isBlank()) {
            return 0.0;
        }

        String cleaned = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9'\\s]", " ");
        String[] words = cleaned.split("\\s+");

        double totalScore = 0.0;
        int sentimentWordsFound = 0;
        boolean negated = false;

        for (int i = 0; i < words.length; i++) {
            String word = words[i].trim();
            if (word.isEmpty()) continue;

            if (NEGATORS.contains(word)) {
                negated = true;
                continue;
            }

            double wordScore = 0.0;
            if (POSITIVE_LEXICON.containsKey(word)) {
                wordScore = POSITIVE_LEXICON.get(word);
            } else if (NEGATIVE_LEXICON.containsKey(word)) {
                wordScore = NEGATIVE_LEXICON.get(word);
            }

            if (wordScore != 0.0) {
                if (negated) {
                    wordScore = -wordScore * 0.8; // negate and slightly dampen
                    negated = false;
                }
                totalScore += wordScore;
                sentimentWordsFound++;
            } else {
                // If more than 2 words passed after negator, reset negation
                negated = false;
            }
        }

        if (sentimentWordsFound == 0) {
            return 0.0;
        }

        // Normalize between -1.0 and 1.0 using tanh-like scaling
        double avg = totalScore / sentimentWordsFound;
        return Math.max(-1.0, Math.min(1.0, avg / 2.0));
    }

    @Override
    public Comment.SentimentType classify(String text) {
        double score = analyzeSentiment(text);
        if (score >= 0.15) {
            return Comment.SentimentType.POSITIVE;
        } else if (score <= -0.15) {
            return Comment.SentimentType.NEGATIVE;
        } else {
            return Comment.SentimentType.NEUTRAL;
        }
    }

    @Override
    public SentimentResult analyzeBatch(List<Post> posts, List<Comment> comments) {
        SentimentResult result = new SentimentResult();
        long pos = 0, neu = 0, neg = 0;
        double sumScore = 0.0;
        long total = 0;

        Map<String, Integer> posFreq = new HashMap<>();
        Map<String, Integer> negFreq = new HashMap<>();

        // Analyze comments
        if (comments != null) {
            for (Comment c : comments) {
                double score = analyzeSentiment(c.getText());
                Comment.SentimentType type = classify(c.getText());
                c.setSentiment(type);
                c.setSentimentScore(score);

                total++;
                sumScore += score;
                if (type == Comment.SentimentType.POSITIVE) pos++;
                else if (type == Comment.SentimentType.NEGATIVE) neg++;
                else neu++;

                extractLexiconKeywords(c.getText(), posFreq, negFreq);
            }
        }

        // Also analyze post captions/content if no comments or to complement
        if (posts != null) {
            for (Post p : posts) {
                double score = analyzeSentiment(p.getContent());
                Comment.SentimentType type = classify(p.getContent());

                total++;
                sumScore += score;
                if (type == Comment.SentimentType.POSITIVE) pos++;
                else if (type == Comment.SentimentType.NEGATIVE) neg++;
                else neu++;

                extractLexiconKeywords(p.getContent(), posFreq, negFreq);
            }
        }

        int pCount = (posts != null) ? posts.size() : 0;
        int cCount = (comments != null) ? comments.size() : 0;
        result.setPostsAnalyzed(pCount);
        result.setCommentsAnalyzed(cCount);
        if (cCount > 0 && pCount > 0) {
            result.setCorpusDescription(String.format("Analyzed %d audience comments and %d post captions", cCount, pCount));
        } else if (cCount > 0) {
            result.setCorpusDescription(String.format("Analyzed %d audience comments", cCount));
        } else if (pCount > 0) {
            result.setCorpusDescription(String.format("Analyzed %d post captions (Audience comments restricted via API scope)", pCount));
        } else {
            result.setCorpusDescription("No text corpus available for analysis");
        }

        result.setTotalAnalyzed(total);
        result.setPositiveCount(pos);
        result.setNeutralCount(neu);
        result.setNegativeCount(neg);
        result.setAverageScore(total > 0 ? (sumScore / total) : 0.0);
        result.calculatePercentages();
        result.setTopPositiveKeywords(posFreq);
        result.setTopNegativeKeywords(negFreq);

        return result;
    }

    private void extractLexiconKeywords(String text, Map<String, Integer> posFreq, Map<String, Integer> negFreq) {
        if (text == null) return;
        String[] words = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ").split("\\s+");
        for (String w : words) {
            w = w.trim();
            if (POSITIVE_LEXICON.containsKey(w)) {
                posFreq.put(w, posFreq.getOrDefault(w, 0) + 1);
            } else if (NEGATIVE_LEXICON.containsKey(w)) {
                negFreq.put(w, negFreq.getOrDefault(w, 0) + 1);
            }
        }
    }
}
