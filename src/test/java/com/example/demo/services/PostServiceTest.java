package com.example.demo.services;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.dtos.PostDTO;
import com.example.demo.entities.Post;
import com.example.demo.entities.User;
import com.example.demo.entities.enums.PostCategory;
import com.example.demo.entities.enums.Role;
import com.example.demo.repositories.PostRepository;
import com.example.demo.services.exceptions.ResourceNotFoundException;
import com.example.demo.services.exceptions.UnauthorizedAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@DisplayName("PostServiceTest")
class PostServiceTest extends ApplicationConfigTest {
    @MockBean
    private PostRepository postRepository;

    @Autowired
    PostService postService;

    User USER_RECORD = new User("a", "b", "c", Role.ROLE_USER);
    Set<PostCategory> CATEGORIES_RECORD = new HashSet<>(Collections.singleton(PostCategory.valueOf(1)));
    PostDTO POST_DTO_RECORD = new PostDTO("title", "contentmusthaveatleast30characters", CATEGORIES_RECORD);
    Post POST_RECORD = new Post(POST_DTO_RECORD.getTitle(), POST_DTO_RECORD.getContent(), Instant.now(), CATEGORIES_RECORD, USER_RECORD);

    private Authentication authentication;
    private SecurityContext securityContext;

    @BeforeEach
    void setupSecurityContext() {
        authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(USER_RECORD);

        securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("should create a post")
    void create() {
        when(postRepository.save(any(Post.class))).thenReturn(POST_RECORD);

        Post result = postService.create(POST_DTO_RECORD);

        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(Post.class);
        assertThat(result).isEqualTo(POST_RECORD);

        verify(authentication, times(1)).getPrincipal();
        verify(securityContext, times(1)).getAuthentication();
        verify(postRepository, times(1)).save(any(Post.class));
    }

    @Test
    @DisplayName("should get all posts")
    void findAll() {
        List<Post> posts = Collections.singletonList(POST_RECORD);

        when(postRepository.findAll()).thenReturn(posts);

        List<Post> result = postService.findAll();

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(posts);

        verify(postRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("should get a post")
    void findById() {
        when(postRepository.findById(any(UUID.class))).thenReturn(Optional.of(POST_RECORD));

        Post result = postService.findById(UUID.randomUUID());

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(POST_RECORD);

        verify(postRepository, times(1)).findById(any(UUID.class));
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException if no post is found")
    void findByIdNotFound() {
        when(postRepository.findById(any(UUID.class))).thenThrow(ResourceNotFoundException.class);

        assertThrows(ResourceNotFoundException.class, () -> postService.findById(UUID.randomUUID()));

        verify(postRepository, times(1)).findById(any(UUID.class));
    }

    @Test
    @DisplayName("should update a post")
    void update() {
        ReflectionTestUtils.setField(USER_RECORD, "id", UUID.randomUUID());

        when(postRepository.getReferenceById(any(UUID.class))).thenReturn(POST_RECORD);
        when(postRepository.save(any(Post.class))).thenReturn(POST_RECORD);

        POST_RECORD.setTitle("new title");
        POST_RECORD.setContent("new content");

        Post result = postService.update(UUID.randomUUID(), POST_RECORD);

        assertThat(result).isEqualTo(POST_RECORD);

        verify(authentication, times(1)).getPrincipal();
        verify(securityContext, times(1)).getAuthentication();
        verify(postRepository, times(1)).getReferenceById(any(UUID.class));
        verify(postRepository, times(1)).save(any(Post.class));
    }

    @Test
    @DisplayName("should throw UnauthorizedAccessException if checkOwnership is invalid")
    void updateUnauthorizedAccessException() {
        User user2 = new User("a", "b", "c", Role.ROLE_USER);
        ReflectionTestUtils.setField(user2, "id", UUID.randomUUID());

        when(authentication.getPrincipal()).thenReturn(user2);

        when(postRepository.getReferenceById(any(UUID.class))).thenReturn(POST_RECORD);
        when(postRepository.save(any(Post.class))).thenReturn(POST_RECORD);

        assertThrows(UnauthorizedAccessException.class, () -> postService.update(UUID.randomUUID(), POST_RECORD));

        verify(authentication, times(1)).getPrincipal();
        verify(securityContext, times(1)).getAuthentication();
        verify(postRepository, times(1)).getReferenceById(any(UUID.class));
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("should delete a post")
    void delete() {
        ReflectionTestUtils.setField(USER_RECORD, "id", UUID.randomUUID());

        when(postRepository.getReferenceById(any(UUID.class))).thenReturn(POST_RECORD);
        doNothing().when(postRepository).deleteById(any(UUID.class));

        postService.delete(UUID.randomUUID());

        verify(authentication, times(1)).getPrincipal();
        verify(securityContext, times(1)).getAuthentication();
        verify(postRepository, times(1)).getReferenceById(any(UUID.class));
        verify(postRepository, times(1)).deleteById(any(UUID.class));
    }

    @Test
    @DisplayName("should throw UnauthorizedAccessException if checkOwnership is invalid")
    void deleteUnauthorizedAccessException() {
        User user2 = new User("a", "b", "c", Role.ROLE_USER);
        ReflectionTestUtils.setField(user2, "id", UUID.randomUUID());

        when(authentication.getPrincipal()).thenReturn(user2);

        when(postRepository.getReferenceById(any(UUID.class))).thenReturn(POST_RECORD);
        doNothing().when(postRepository).deleteById(any(UUID.class));

        assertThrows(UnauthorizedAccessException.class,
                () -> postService.delete(UUID.randomUUID()));

        verify(authentication, times(1)).getPrincipal();
        verify(securityContext, times(1)).getAuthentication();
        verify(postRepository, times(1)).getReferenceById(any(UUID.class));
        verify(postRepository, never()).deleteById(any(UUID.class));
    }

    @Test
    @DisplayName("should bypass checkOwnership if user is an admin")
    void deleteAdmin() {
        User user2 = new User("a", "b", "c", Role.ROLE_ADMIN);

        when(authentication.getPrincipal()).thenReturn(user2);

        when(postRepository.getReferenceById(any(UUID.class))).thenReturn(POST_RECORD);
        doNothing().when(postRepository).deleteById(any(UUID.class));

        postService.delete(UUID.randomUUID());

        verify(authentication, times(1)).getPrincipal();
        verify(securityContext, times(1)).getAuthentication();
        verify(postRepository, times(1)).getReferenceById(any(UUID.class));
        verify(postRepository, times(1)).deleteById(any(UUID.class));
    }

    @Test
    @DisplayName("should increase the upvote")
    void increaseUpvote() {
        ReflectionTestUtils.setField(USER_RECORD, "id", UUID.randomUUID());

        when(postRepository.findById(any(UUID.class))).thenReturn(Optional.of(POST_RECORD));

        boolean result = postService.increaseUpvote(UUID.randomUUID());

        assertThat(result).isTrue();

        verify(authentication, times(1)).getPrincipal();
        verify(securityContext, times(1)).getAuthentication();
        verify(postRepository, times(1)).findById(any(UUID.class));
        verify(postRepository, times(1)).save(any(Post.class));
    }

    @Test
    @DisplayName("should not increase the upvote if user already upvoted")
    void increaseUpvoteDeny() {
        ReflectionTestUtils.setField(USER_RECORD, "id", UUID.randomUUID());
        POST_RECORD.increaseUpvote(USER_RECORD.getId());

        when(postRepository.findById(any(UUID.class))).thenReturn(Optional.of(POST_RECORD));

        boolean result = postService.increaseUpvote(UUID.randomUUID());

        assertThat(result).isFalse();

        verify(authentication, times(1)).getPrincipal();
        verify(securityContext, times(1)).getAuthentication();
        verify(postRepository, times(1)).findById(any(UUID.class));
        verify(postRepository, never()).save(any(Post.class));
    }

}