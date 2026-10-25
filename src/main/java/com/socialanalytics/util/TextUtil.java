package com.socialanalytics.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Text processing utility for hashtag extraction, tokenization, and stop-word filtering.
 */
public final class TextUtil {
    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#([A-Za-z0-9_]{2,50})");

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "a", "about", "above", "after", "again", "against", "all", "am", "an", "and",
            "any", "are", "aren't", "as", "at", "be", "because", "been", "before", "being",
            "below", "between", "both", "but", "by", "can't", "cannot", "could", "couldn't",
            "did", "didn't", "do", "does", "doesn't", "doing", "don't", "down", "during",
            "each", "few", "for", "from", "further", "had", "hadn't", "has", "hasn't",
            "have", "haven't", "having", "he", "he'd", "he'll", "he's", "her", "here",
            "here's", "hers", "herself", "him", "himself", "his", "how", "how's", "i",
            "i'd", "i'll", "i'm", "i've", "if", "in", "into", "is", "isn't", "it", "it's",
            "its", "itself", "let's", "me", "more", "most", "mustn't", "my", "myself",
            "no", "nor", "not", "of", "off", "on", "once", "only", "or", "other", "ought",
            "our", "ours", "ourselves", "out", "over", "own", "same", "shan't", "she",
            "she'd", "she'll", "she's", "should", "shouldn't", "so", "some", "such",
            "than", "that", "that's", "the", "their", "theirs", "them", "themselves",
            "then", "there", "there's", "these", "they", "they'd", "they'll", "they're",
            "they've", "this", "those", "through", "to", "too", "under", "until", "up",
            "very", "was", "wasn't", "we", "we'd", "we'll", "we're", "we've", "were",
            "weren't", "what", "what's", "when", "when's", "where", "where's", "which",
            "while", "who", "who's", "whom", "why", "why's", "with", "won't", "would",
            "wouldn't", "you", "you'd", "you'll", "you're", "you've", "your", "yours",
            "yourself", "yourselves", "http", "https", "co", "rt", "via", "amp",
            // Movie metadata & common social media boilerplate
            "sung", "directed", "written", "produced", "singer", "composed", "starring", "featuring",
            "director", "producer", "music", "lyrics", "song", "movie", "film", "cinema", "video",
            "reel", "post", "official", "release", "released", "coming", "soon", "link", "bio",
            "watch", "full", "share", "subscribe", "follow", "check", "youtube", "instagram",
            "facebook", "twitter", "page", "reels", "cinemas", "theatres", "part", "chapter",
            "credits", "special", "thanks", "title", "track", "audio", "album", "poster",
            "teaser", "trailer", "update", "updates", "releasing", "theatrical", "shot",
            "scene", "dialogue", "story", "screenplay", "review", "actor", "actress", "star"
    ));

    private TextUtil() {
    }

    /**
     * Extracts hashtags from text without the leading '#' symbol.
     */
    public static List<String> extractHashtags(String text) {
        List<String> tags = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return tags;
        }
        Matcher matcher = HASHTAG_PATTERN.matcher(text);
        while (matcher.find()) {
            String tag = matcher.group(1).toLowerCase();
            if (!tags.contains(tag)) {
                tags.add(tag);
            }
        }
        return tags;
    }

    /**
     * Extracts meaningful keyword tokens, excluding stop-words and short tokens.
     */
    public static List<String> extractKeywords(String text) {
        List<String> keywords = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return keywords;
        }
        // Remove URLs and non-alphanumeric chars (keep spaces and letters)
        String cleaned = text.replaceAll("https?://\\S+\\s?", " ")
                .replaceAll("[^a-zA-Z0-9\\s#]", " ")
                .toLowerCase();

        String[] tokens = cleaned.split("\\s+");
        for (String token : tokens) {
            token = token.trim();
            boolean isHashtag = token.startsWith("#");
            if (isHashtag) {
                token = token.substring(1);
            }
            int minLen = isHashtag ? 3 : 4;
            if (token.length() >= minLen && !STOP_WORDS.contains(token) && !token.matches("\\d+")) {
                keywords.add(token);
            }
        }
        return keywords;
    }
}
