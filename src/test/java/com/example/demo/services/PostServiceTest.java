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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
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

    @Test
    @DisplayName("should create a post")
    void create() {
        Set<PostCategory> categories = new HashSet<>(Collections.singleton(PostCategory.valueOf(1)));
        User user = new User("a", "b", "c", Role.ROLE_USER);
        PostDTO dto = new PostDTO("title", "content", categories);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        Instant instant = Instant.now();
        Post post = new Post(dto.getTitle(), dto.getContent(), instant, dto.getCategories(), user);
        when(postRepository.save(ArgumentMatchers.any(Post.class))).thenReturn(post);

        Post result = postService.create(dto);

        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(Post.class);
        assertThat(result.getTitle()).isEqualTo(dto.getTitle());
        assertThat(result.getCategories()).isEqualTo(dto.getCategories());
        assertThat(result.getUpvotes()).isEqualTo(0);
        assertThat(result.getUsersUpvotesId()).isEmpty();
        assertThat(result.getPublishDate()).isEqualTo(instant);
        assertThat(result.getAuthor()).isEqualTo(user);

        verify(authentication, times(1)).getPrincipal();
        verify(securityContext, times(1)).getAuthentication();
        verify(postRepository, times(1)).save(ArgumentMatchers.any(Post.class));
    }

    @Test
    @DisplayName("should get all posts")
    void findAll() {
        User user = new User("a", "b", "c", Role.ROLE_USER);
        Set<PostCategory> categories = new HashSet<>(Collections.singleton(PostCategory.valueOf(1)));
        Post post1 = new Post("title", "content", Instant.now(), categories, user);
        Post post2 = new Post("title", "content", Instant.now(), categories, user);
        List<Post> posts = Arrays.asList(post1, post2);

        when(postRepository.findAll()).thenReturn(posts);

        List<Post> result = postService.findAll();

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(posts);

        verify(postRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("should get a post")
    void findById() {
        User user = new User("a", "b", "c", Role.ROLE_USER);
        Set<PostCategory> categories = new HashSet<>(Collections.singleton(PostCategory.valueOf(1)));
        Post post = new Post("title", "content", Instant.now(), categories, user);

        when(postRepository.findById(ArgumentMatchers.any(UUID.class))).thenReturn(Optional.of(post));

        Post result = postService.findById(UUID.randomUUID());

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(post);

        verify(postRepository, times(1)).findById(ArgumentMatchers.any(UUID.class));
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException if no post is found")
    void findByIdNotFound() {
        when(postRepository.findById(ArgumentMatchers.any(UUID.class))).thenThrow(ResourceNotFoundException.class);

        assertThrows(ResourceNotFoundException.class, () -> postService.findById(UUID.randomUUID()));

        verify(postRepository, times(1)).findById(ArgumentMatchers.any(UUID.class));
    }

    @Test
    @DisplayName("should update a post")
    void update() {
        User user = new User("a", "b", "c", Role.ROLE_USER);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        Set<PostCategory> categories = new HashSet<>(Collections.singleton(PostCategory.valueOf(1)));
        Post post = new Post("title", "content", Instant.now(), categories, user);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(postRepository.getReferenceById(ArgumentMatchers.any(UUID.class))).thenReturn(post);
        when(postRepository.save(ArgumentMatchers.any(Post.class))).thenReturn(post);

        post.setTitle("new title");
        post.setContent("new content");

        Post result = postService.update(UUID.randomUUID(), post);

        assertThat(result.getTitle()).isEqualTo(post.getTitle());
        assertThat(result.getContent()).isEqualTo(post.getContent());

        verify(authentication, times(1)).getPrincipal();
        verify(securityContext, times(1)).getAuthentication();
        verify(postRepository, times(1)).getReferenceById(ArgumentMatchers.any(UUID.class));
        verify(postRepository, times(1)).save(ArgumentMatchers.any(Post.class));
    }

    @Test
    @DisplayName("should throw UnauthorizedAccessException if checkOwnership is invalid")
    void updateUnauthorizedAccessException() {
        User user = new User("a", "b", "c", Role.ROLE_USER);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        Set<PostCategory> categories = new HashSet<>(Collections.singleton(PostCategory.valueOf(1)));
        Post post = new Post("title", "content", Instant.now(), categories, user);

        User user2 = new User("a", "b", "c", Role.ROLE_USER);
        ReflectionTestUtils.setField(user2, "id", UUID.randomUUID());

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user2);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(postRepository.getReferenceById(ArgumentMatchers.any(UUID.class))).thenReturn(post);
        when(postRepository.save(ArgumentMatchers.any(Post.class))).thenReturn(post);

        assertThrows(UnauthorizedAccessException.class, () -> postService.update(UUID.randomUUID(), post));

        verify(authentication, times(1)).getPrincipal();
        verify(securityContext, times(1)).getAuthentication();
        verify(postRepository, times(1)).getReferenceById(ArgumentMatchers.any(UUID.class));
        verify(postRepository, never()).save(ArgumentMatchers.any(Post.class));
    }

    @Test
    @DisplayName("should delete a post")
    void delete() {
        User user = new User("a", "b", "c", Role.ROLE_USER);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        Set<PostCategory> categories = new HashSet<>(Collections.singleton(PostCategory.valueOf(1)));
        Post post = new Post("title", "content", Instant.now(), categories, user);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(postRepository.getReferenceById(ArgumentMatchers.any(UUID.class))).thenReturn(post);
        doNothing().when(postRepository).deleteById(ArgumentMatchers.any(UUID.class));

        postService.delete(UUID.randomUUID());

        verify(authentication, times(1)).getPrincipal();
        verify(securityContext, times(1)).getAuthentication();
        verify(postRepository, times(1)).getReferenceById(ArgumentMatchers.any(UUID.class));
        verify(postRepository, times(1)).deleteById(ArgumentMatchers.any(UUID.class));
    }

    @Test
    @DisplayName("should throw UnauthorizedAccessException if checkOwnership is invalid")
    void deleteUnauthorizedAccessException() {
        User user = new User("a", "b", "c", Role.ROLE_USER);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        User user2 = new User("a", "b", "c", Role.ROLE_USER);
        ReflectionTestUtils.setField(user2, "id", UUID.randomUUID());
        Set<PostCategory> categories = new HashSet<>(Collections.singleton(PostCategory.valueOf(1)));
        Post post = new Post("title", "content", Instant.now(), categories, user2);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(postRepository.getReferenceById(ArgumentMatchers.any(UUID.class))).thenReturn(post);
        doNothing().when(postRepository).deleteById(ArgumentMatchers.any(UUID.class));

        assertThrows(UnauthorizedAccessException.class, () -> postService.delete(UUID.randomUUID()));

        verify(authentication, times(1)).getPrincipal();
        verify(securityContext, times(1)).getAuthentication();
        verify(postRepository, times(1)).getReferenceById(ArgumentMatchers.any(UUID.class));
        verify(postRepository, never()).deleteById(ArgumentMatchers.any(UUID.class));
    }

    @Test
    @DisplayName("should bypass checkOwnership if user is an admin")
    void deleteAdmin() {
        User user = new User("a", "b", "c", Role.ROLE_USER);
        User user2 = new User("a", "b", "c", Role.ROLE_ADMIN);
        Set<PostCategory> categories = new HashSet<>(Collections.singleton(PostCategory.valueOf(1)));
        Post post = new Post("title", "content", Instant.now(), categories, user);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user2);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(postRepository.getReferenceById(ArgumentMatchers.any(UUID.class))).thenReturn(post);
        doNothing().when(postRepository).deleteById(ArgumentMatchers.any(UUID.class));

        postService.delete(UUID.randomUUID());

        verify(authentication, times(1)).getPrincipal();
        verify(securityContext, times(1)).getAuthentication();
        verify(postRepository, times(1)).getReferenceById(ArgumentMatchers.any(UUID.class));
        verify(postRepository, times(1)).deleteById(ArgumentMatchers.any(UUID.class));
    }

    @Test
    @DisplayName("should increase the upvote")
    void increaseUpvote() {
        User user = new User("a", "b", "c", Role.ROLE_USER);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        Set<PostCategory> categories = new HashSet<>(Collections.singleton(PostCategory.valueOf(1)));
        Post post = new Post("title", "content", Instant.now(), categories, user);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(postRepository.findById(ArgumentMatchers.any(UUID.class))).thenReturn(Optional.of(post));

        String result = postService.increaseUpvote(UUID.randomUUID());

        assertThat(result).isEqualTo("Successful!");

        verify(authentication, times(1)).getPrincipal();
        verify(securityContext, times(1)).getAuthentication();
        verify(postRepository, times(1)).findById(ArgumentMatchers.any(UUID.class));
        verify(postRepository, times(1)).save(ArgumentMatchers.any(Post.class));
    }

    @Test
    @DisplayName("should not increase the upvote if user already upvoted")
    void increaseUpvoteDeny() {
        User user = new User("a", "b", "c", Role.ROLE_USER);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        Set<PostCategory> categories = new HashSet<>(Collections.singleton(PostCategory.valueOf(1)));
        Post post = new Post("title", "content", Instant.now(), categories, user);
        post.increaseUpvote(user.getId());

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(postRepository.findById(ArgumentMatchers.any(UUID.class))).thenReturn(Optional.of(post));

        String result = postService.increaseUpvote(UUID.randomUUID());

        assertThat(result).isEqualTo("User already upvoted! You can only upvote once!");

        verify(authentication, times(1)).getPrincipal();
        verify(securityContext, times(1)).getAuthentication();
        verify(postRepository, times(1)).findById(ArgumentMatchers.any(UUID.class));
        verify(postRepository, never()).save(ArgumentMatchers.any(Post.class));
    }

}