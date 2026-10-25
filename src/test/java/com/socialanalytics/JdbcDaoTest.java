package com.socialanalytics;

import com.socialanalytics.dao.CommentDAO;
import com.socialanalytics.dao.PostDAO;
import com.socialanalytics.dao.SocialAccountDAO;
import com.socialanalytics.dao.UserDAO;
import com.socialanalytics.dao.impl.JdbcCommentDAO;
import com.socialanalytics.dao.impl.JdbcPostDAO;
import com.socialanalytics.dao.impl.JdbcSocialAccountDAO;
import com.socialanalytics.dao.impl.JdbcUserDAO;
import com.socialanalytics.database.DatabaseManager;
import com.socialanalytics.model.*;
import com.socialanalytics.util.SecurityUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class JdbcDaoTest {
    private static DatabaseManager dbManager;
    private static UserDAO userDAO;
    private static SocialAccountDAO socialAccountDAO;
    private static PostDAO postDAO;
    private static CommentDAO commentDAO;

    @BeforeAll
    public static void setup() {
        dbManager = DatabaseManager.getInstance();
        dbManager.initializeSchema();
        userDAO = new JdbcUserDAO(dbManager);
        socialAccountDAO = new JdbcSocialAccountDAO(dbManager);
        postDAO = new JdbcPostDAO(dbManager);
        commentDAO = new JdbcCommentDAO(dbManager);
    }

    @Test
    public void testUserCreationAndAuthentication() {
        String username = "testuser_" + System.currentTimeMillis();
        String salt = SecurityUtil.generateSalt();
        String hash = SecurityUtil.hashPassword("secret123", salt);

        User u = new User(null, username, username + "@test.com", hash, salt, LocalDateTime.now());
        User created = userDAO.create(u);

        assertNotNull(created.getId());
        Optional<User> fetched = userDAO.findByUsername(username);
        assertTrue(fetched.isPresent());
        assertEquals(username, fetched.get().getUsername());

        // Verify password
        assertTrue(SecurityUtil.verifyPassword("secret123", fetched.get().getSalt(), fetched.get().getPasswordHash()));
        assertFalse(SecurityUtil.verifyPassword("wrongpass", fetched.get().getSalt(), fetched.get().getPasswordHash()));
    }

    @Test
    public void testSqlInjectionPrevention() {
        // Attack payload
        String injectionAttempt = "' OR '1'='1";
        Optional<User> result = userDAO.findByUsername(injectionAttempt);
        // PreparedStatement ensures this searches literally for that string, not injecting SQL
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSocialAccountAndPostPersistence() {
        String username = "accountuser_" + System.currentTimeMillis();
        String salt = SecurityUtil.generateSalt();
        User u = userDAO.create(new User(null, username, username + "@test.com", "h", salt, LocalDateTime.now()));

        SocialAccount acc = new SocialAccount();
        acc.setUserId(u.getId());
        acc.setPlatform(Platform.INSTAGRAM);
        acc.setPlatformAccountId("acc_" + System.currentTimeMillis());
        acc.setUsername("ig_test");
        acc.setDisplayName("IG Test Page");
        acc.setFollowerCount(1500);
        acc.setConnected(true);
        acc.setDemo(true);
        acc.setAccessToken("token_sample");

        SocialAccount savedAcc = socialAccountDAO.save(acc);
        assertNotNull(savedAcc.getId());

        // Save Posts with Transaction
        Post p1 = new Post("post_test_1", savedAcc.getId(), Platform.INSTAGRAM, "Hello #Java #AI", LocalDateTime.now(), "IMAGE", 50, 10, 2, 800L, "");
        p1.addHashtag("Java");
        p1.addHashtag("AI");

        postDAO.saveAll(List.of(p1));

        Optional<Post> fetchedPost = postDAO.findById("post_test_1");
        assertTrue(fetchedPost.isPresent());
        assertEquals(50, fetchedPost.get().getLikes());
        assertEquals(2, fetchedPost.get().getHashtags().size());

        // Save Comment
        Comment c = new Comment("comm_test_1", "post_test_1", Platform.INSTAGRAM, "Audience", "Super clean code!", LocalDateTime.now());
        commentDAO.save(c);

        List<Comment> postComments = commentDAO.findByPostId("post_test_1");
        assertEquals(1, postComments.size());
        assertEquals("Super clean code!", postComments.get(0).getText());
    }
}
