package com.socialanalytics.connector.demo;

import com.socialanalytics.model.Comment;
import com.socialanalytics.model.Platform;
import com.socialanalytics.model.Post;
import com.socialanalytics.model.SocialAccount;
import com.socialanalytics.util.TextUtil;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates realistic, multi-platform social media accounts, posts, and comments
 * for DEMO MODE. Explicitly marked as Demo data to strictly adhere to transparency guidelines.
 */
public final class DemoDataGenerator {

    private DemoDataGenerator() {
    }

    public static List<SocialAccount> generateDemoAccounts(Long userId) {
        List<SocialAccount> accounts = new ArrayList<>();

        // 1. Instagram
        SocialAccount ig = new SocialAccount();
        ig.setUserId(userId);
        ig.setPlatform(Platform.INSTAGRAM);
        ig.setPlatformAccountId("demo_ig_101");
        ig.setUsername("techpulse_official");
        ig.setDisplayName("TechPulse Official");
        ig.setProfileUrl("https://instagram.com/techpulse_official");
        ig.setFollowerCount(48200);
        ig.setFollowingCount(420);
        ig.setConnected(true);
        ig.setDemo(true);
        ig.setAccessToken("demo_token_instagram_simulation");
        ig.setLastSyncedAt(LocalDateTime.now().minusMinutes(12));
        accounts.add(ig);

        // 2. Facebook
        SocialAccount fb = new SocialAccount();
        fb.setUserId(userId);
        fb.setPlatform(Platform.FACEBOOK);
        fb.setPlatformAccountId("demo_fb_202");
        fb.setUsername("TechVisionGlobal");
        fb.setDisplayName("TechVision Global Page");
        fb.setProfileUrl("https://facebook.com/techvisionglobal");
        fb.setFollowerCount(72500);
        fb.setFollowingCount(120);
        fb.setConnected(true);
        fb.setDemo(true);
        fb.setAccessToken("demo_token_facebook_simulation");
        fb.setLastSyncedAt(LocalDateTime.now().minusMinutes(25));
        accounts.add(fb);

        // 3. LinkedIn
        SocialAccount li = new SocialAccount();
        li.setUserId(userId);
        li.setPlatform(Platform.LINKEDIN);
        li.setPlatformAccountId("demo_li_303");
        li.setUsername("enterprise-cloud-ai");
        li.setDisplayName("Enterprise Cloud & AI Solutions");
        li.setProfileUrl("https://linkedin.com/company/enterprise-cloud-ai");
        li.setFollowerCount(19800);
        li.setFollowingCount(85);
        li.setConnected(true);
        li.setDemo(true);
        li.setAccessToken("demo_token_linkedin_simulation");
        li.setLastSyncedAt(LocalDateTime.now().minusMinutes(5));
        accounts.add(li);

        // 4. X
        SocialAccount x = new SocialAccount();
        x.setUserId(userId);
        x.setPlatform(Platform.X);
        x.setPlatformAccountId("demo_x_404");
        x.setUsername("DevInsights");
        x.setDisplayName("Dev Insights & AI");
        x.setProfileUrl("https://twitter.com/DevInsights");
        x.setFollowerCount(34100);
        x.setFollowingCount(610);
        x.setConnected(true);
        x.setDemo(true);
        x.setAccessToken("demo_token_x_simulation");
        x.setLastSyncedAt(LocalDateTime.now().minusMinutes(3));
        accounts.add(x);

        return accounts;
    }

    public static List<Post> generateDemoPosts(Long accountId, Platform platform) {
        List<Post> posts = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusDays(28);

        switch (platform) {
            case INSTAGRAM:
                posts.addAll(generateInstagramPosts(accountId, baseTime));
                break;
            case FACEBOOK:
                posts.addAll(generateFacebookPosts(accountId, baseTime));
                break;
            case LINKEDIN:
                posts.addAll(generateLinkedInPosts(accountId, baseTime));
                break;
            case X:
                posts.addAll(generateXPosts(accountId, baseTime));
                break;
        }

        return posts;
    }

    private static List<Post> generateInstagramPosts(Long accountId, LocalDateTime baseTime) {
        List<Post> list = new ArrayList<>();
        String[] samples = {
                "How Generative AI is reshaping developer workflows in 2026. Code smarter, not harder! #AI #MachineLearning #Coding #Innovation",
                "Behind the scenes at our Cloud infrastructure summit! Scaling microservices effortlessly. #Cloud #DevOps #Kubernetes #Tech",
                "Top 5 Cybersecurity practices every software team should adopt today. Prevent breaches! #Cybersecurity #InfoSec #Security",
                "Exploring full-stack Java with modern desktop & web dashboards. What's your favourite framework? #Java #WebDev #SoftwareEngineering",
                "Artificial Intelligence meets Big Data: extracting high-value customer insights in real-time. #AI #DataScience #BigData",
                "Automated CI/CD pipelines cut deployment friction by 70%. Here is our blueprint. #DevOps #Cloud #Automation",
                "Mobile app performance optimization guide. Keep latency under 100ms. #MobileDev #Tech #Performance",
                "AI agents are transforming Pair Programming. The future of software architecture is collaborative! #AI #Innovation #FutureTech",
                "Zero Trust architecture deep dive: Never trust, always verify. #Cybersecurity #Security #Enterprise",
                "Weekend coding challenge: What is your favorite data structure to optimize search queries? #Coding #Java #Tech"
        };
        String[] types = {"REEL", "CAROUSEL", "IMAGE", "VIDEO", "CAROUSEL", "IMAGE", "REEL", "VIDEO", "CAROUSEL", "IMAGE"};
        long[] likes = {12450, 4800, 7900, 3100, 11200, 6200, 2900, 14300, 8900, 4100};
        long[] comments = {823, 312, 450, 180, 710, 290, 140, 940, 520, 230};
        long[] views = {45000, 21000, 32000, 14000, 49000, 28000, 12000, 61000, 39000, 19000};
        int[] hours = {20, 19, 14, 11, 20, 18, 10, 21, 19, 16}; // peak around 7-9 PM

        for (int i = 0; i < samples.length; i++) {
            Post p = new Post();
            p.setId("demo_ig_post_" + (i + 1));
            p.setAccountId(accountId);
            p.setPlatform(Platform.INSTAGRAM);
            p.setContent(samples[i]);
            p.setContentType(types[i]);
            p.setLikes(likes[i]);
            p.setCommentsCount(comments[i]);
            p.setShares(null); // Instagram standard API doesn't support shares
            p.setViews(views[i]);
            p.setPermalink("https://instagram.com/p/demo_ig_" + (i + 1));
            LocalDateTime ts = baseTime.plusDays(i * 2 + 1).withHour(hours[i]).withMinute(15 + i * 3);
            p.setTimestamp(ts);
            for (String tag : TextUtil.extractHashtags(samples[i])) {
                p.addHashtag(tag);
            }
            list.add(p);
        }
        return list;
    }

    private static List<Post> generateFacebookPosts(Long accountId, LocalDateTime baseTime) {
        List<Post> list = new ArrayList<>();
        String[] samples = {
                "Cloud migration strategies: How modern enterprises reduce legacy technical debt with Kubernetes and AWS. #Cloud #DevOps #Enterprise",
                "New Whitepaper: Artificial Intelligence adoption trends in business applications. Read the full summary! #AI #Innovation #Tech",
                "Cybersecurity bulletin: Latest ransomware vectors and how to protect enterprise endpoints effectively. #Cybersecurity #Security #IT",
                "Community Spotlight: Meet the engineering teams building resilient Java full-stack applications. #Java #SoftwareEngineering #Tech",
                "Web performance metrics: Why Core Web Vitals directly impact your user retention and conversion. #WebDev #Analytics #SEO",
                "Generative AI security considerations: Guardrails, data privacy, and compliance. #AI #Cybersecurity #Compliance",
                "Microservices vs Modular Monolith: Which architecture fits your product stage in 2026? #SoftwareArchitecture #Cloud #DevOps"
        };
        long[] likes = {3400, 5600, 4100, 2300, 1800, 6200, 4900};
        long[] comments = {210, 390, 280, 140, 95, 410, 320};
        long[] shares = {180, 420, 260, 90, 75, 390, 310};
        int[] hours = {19, 20, 15, 12, 17, 20, 19};

        for (int i = 0; i < samples.length; i++) {
            Post p = new Post();
            p.setId("demo_fb_post_" + (i + 1));
            p.setAccountId(accountId);
            p.setPlatform(Platform.FACEBOOK);
            p.setContent(samples[i]);
            p.setContentType("STATUS");
            p.setLikes(likes[i]);
            p.setCommentsCount(comments[i]);
            p.setShares(shares[i]);
            p.setViews(null); // Facebook standard page API doesn't provide impressions without insight permissions
            p.setPermalink("https://facebook.com/demo_fb_" + (i + 1));
            LocalDateTime ts = baseTime.plusDays(i * 3 + 2).withHour(hours[i]).withMinute(20 + i * 4);
            p.setTimestamp(ts);
            for (String tag : TextUtil.extractHashtags(samples[i])) {
                p.addHashtag(tag);
            }
            list.add(p);
        }
        return list;
    }

    private static List<Post> generateLinkedInPosts(Long accountId, LocalDateTime baseTime) {
        List<Post> list = new ArrayList<>();
        String[] samples = {
                "The evolution of Cloud Computing: Serverless and AI-driven automation are redefining infrastructure scalability. Excited to share our roadmap! #Cloud #AI #Leadership #Engineering",
                "Addressing the Cybersecurity talent gap: Upskilling internal engineering teams to combat emerging threats. #Cybersecurity #InfoSec #Workplace",
                "Artificial Intelligence is moving from hype to measurable ROI. Key operational learnings from enterprise deployments. #AI #MachineLearning #Enterprise",
                "Why rigorous software engineering principles like clean code and OOP remain timeless regardless of language trends. #Java #SoftwareEngineering #Tech",
                "Excited to announce our open-source release for social analytics and multithreaded trend computation! #Innovation #Analytics #TechCommunity"
        };
        long[] likes = {4100, 2800, 6900, 3200, 5400};
        long[] comments = {310, 190, 480, 220, 360};
        long[] shares = {240, 130, 410, 180, 290};
        int[] hours = {9, 11, 14, 16, 18};

        for (int i = 0; i < samples.length; i++) {
            Post p = new Post();
            p.setId("demo_li_post_" + (i + 1));
            p.setAccountId(accountId);
            p.setPlatform(Platform.LINKEDIN);
            p.setContent(samples[i]);
            p.setContentType("ARTICLE");
            p.setLikes(likes[i]);
            p.setCommentsCount(comments[i]);
            p.setShares(shares[i]);
            p.setViews(22000L + (i * 5000));
            p.setPermalink("https://linkedin.com/feed/update/demo_li_" + (i + 1));
            LocalDateTime ts = baseTime.plusDays(i * 4 + 1).withHour(hours[i]).withMinute(30);
            p.setTimestamp(ts);
            for (String tag : TextUtil.extractHashtags(samples[i])) {
                p.addHashtag(tag);
            }
            list.add(p);
        }
        return list;
    }

    private static List<Post> generateXPosts(Long accountId, LocalDateTime baseTime) {
        List<Post> list = new ArrayList<>();
        String[] samples = {
                "Breaking: Next-gen AI models demonstrate 40% reduction in inference latency. The pace of open-source innovation is unreal. #AI #MachineLearning",
                "Friendly reminder: sanitize your SQL queries and never concatenate user input. PreparedStatements save careers! #Cybersecurity #Java #Coding",
                "Cloud outage post-mortem: Always test your multi-region failover before peak traffic hits. #Cloud #DevOps #SRE",
                "Java 21 virtual threads + reactive pipelines = incredible throughput with minimal memory overhead. #Java #Concurrency",
                "Top emerging tech topic of the week: Autonomous AI agents orchestrating full lifecycle workflows. What are you building? #AI #Tech",
                "Zero-day vulnerability patched in popular logging library. Update immediately! #Cybersecurity #SecurityAlert",
                "Building resilient desktop apps with JavaFX and CSS. Smooth 60 FPS charts! #Java #JavaFX #UI",
                "Why data quality matters more than model size for AI fine-tuning. Garbage in, garbage out. #DataScience #AI"
        };
        long[] likes = {8900, 4200, 6100, 5300, 11500, 7800, 3400, 9200};
        long[] replies = {410, 180, 290, 240, 720, 510, 130, 490};
        long[] retweets = {950, 410, 620, 540, 1420, 890, 280, 1100};
        long[] views = {88000, 39000, 62000, 49000, 125000, 74000, 29000, 98000};
        int[] hours = {20, 14, 18, 16, 21, 15, 19, 20};

        for (int i = 0; i < samples.length; i++) {
            Post p = new Post();
            p.setId("demo_x_tweet_" + (i + 1));
            p.setAccountId(accountId);
            p.setPlatform(Platform.X);
            p.setContent(samples[i]);
            p.setContentType("TWEET");
            p.setLikes(likes[i]);
            p.setCommentsCount(replies[i]);
            p.setShares(retweets[i]);
            p.setViews(views[i]);
            p.setPermalink("https://x.com/DevInsights/status/demo_x_" + (i + 1));
            LocalDateTime ts = baseTime.plusDays(i * 3 + 1).withHour(hours[i]).withMinute(10 + i * 5);
            p.setTimestamp(ts);
            for (String tag : TextUtil.extractHashtags(samples[i])) {
                p.addHashtag(tag);
            }
            list.add(p);
        }
        return list;
    }

    public static List<Comment> generateDemoComments(String postId, Platform platform) {
        List<Comment> comments = new ArrayList<>();
        String[][] commentData = {
                {"Sarah Miller", "This is an amazing breakdown! Extremely useful and well structured.", "POSITIVE", "0.85"},
                {"Alex Turner", "Great insights, really appreciate the detailed explanations here.", "POSITIVE", "0.78"},
                {"David Ross", "Terrible experience with the previous release. Completely broke our deployment and useless support.", "NEGATIVE", "-0.82"},
                {"Elena Petrova", "Excellent recommendations. We implemented zero trust and saw immediate improvements.", "POSITIVE", "0.88"},
                {"Michael Chen", "Interesting comparison. How does this benchmark against older architectures?", "NEUTRAL", "0.05"},
                {"Rachel Green", "Poor documentation made this very hard to reproduce. Needs serious improvement.", "NEGATIVE", "-0.65"},
                {"Daniel Kim", "Love this approach! Clean, maintainable, and highly efficient.", "POSITIVE", "0.90"},
                {"Sofia Gomez", "Standard overview, nothing particularly new but decent summary.", "NEUTRAL", "0.10"},
                {"Marcus Vance", "Bad performance under high concurrency. We had to roll back immediately.", "NEGATIVE", "-0.75"},
                {"Aisha Patel", "Brilliant work! The best tutorial on this topic so far.", "POSITIVE", "0.92"}
        };

        for (int i = 0; i < commentData.length; i++) {
            Comment c = new Comment();
            c.setCommentId("demo_comm_" + postId + "_" + (i + 1));
            c.setPostId(postId);
            c.setPlatform(platform);
            c.setAuthorName(commentData[i][0]);
            c.setText(commentData[i][1]);
            c.setSentiment(Comment.SentimentType.valueOf(commentData[i][2]));
            c.setSentimentScore(Double.parseDouble(commentData[i][3]));
            c.setTimestamp(LocalDateTime.now().minusHours(2 + i * 3));
            comments.add(c);
        }

        return comments;
    }
}
