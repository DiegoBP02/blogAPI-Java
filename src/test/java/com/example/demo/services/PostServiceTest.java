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
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@DisplayName("PostServiceTest")
class PostServiceTest extends ApplicationConfigTest {
    @Autowired
    PostService postService;
    User USER_RECORD = new User("a", "b", "c", Role.ROLE_USER);
    Set<PostCategory> CATEGORIES_RECORD = new HashSet<>(Collections.singleton(PostCategory.valueOf(1)));
    PostDTO POST_DTO_RECORD = new PostDTO("title", "contentmusthaveatleast30characters", CATEGORIES_RECORD);
    Post POST_RECORD = new Post(POST_DTO_RECORD.getTitle(), POST_DTO_RECORD.getContent(), Instant.now(), CATEGORIES_RECORD, USER_RECORD);
    Post POST_RECORD_2 = new Post("z", POST_DTO_RECORD.getContent(), Instant.now().plusSeconds(1), CATEGORIES_RECORD, USER_RECORD);
    List<Post> POST_LIST_RECORD = Arrays.asList(POST_RECORD, POST_RECORD_2);
    Pageable PAGING_RECORD = PageRequest.of(0, 5, Sort.by("title"));
    Page<Post> POSTS_RECORD = new PageImpl<>(POST_LIST_RECORD, PAGING_RECORD, 1);

    @MockBean
    private PostRepository postRepository;

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
        when(postRepository.findAll(any(Pageable.class))).thenReturn(POSTS_RECORD);

        Page<Post> result = postService.findAll(0, 5, "title");

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo(POST_LIST_RECORD);

        verify(postRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("should return the correct page and number of pages")
    void findAllWithPageNo() {
        when(postRepository.findAll(any(Pageable.class))).thenReturn(POSTS_RECORD);

        Page<Post> result = postService.findAll(0, 5, "title");

        assertThat(result).isNotNull();
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getTotalPages()).isEqualTo(1);

        verify(postRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("should return the correct number of items per page")
    void findAllWithPageSize() {
        when(postRepository.findAll(any(Pageable.class))).thenReturn(POSTS_RECORD);

        Page<Post> result = postService.findAll(0, 5, "title");

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(POST_LIST_RECORD.toArray().length);

        verify(postRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("should return the correct sorting properties")
    void findAllWithSortBy() {
        when(postRepository.findAll(any(Pageable.class))).thenReturn(POSTS_RECORD);

        Page<Post> result = postService.findAll(0, 5, "title");

        assertThat(result).isNotNull();
        assertThat(result.getPageable().getPageNumber()).isEqualTo(PAGING_RECORD.getPageNumber());
        assertThat(result.getPageable().getSort()).isEqualTo(PAGING_RECORD.getSort());
        assertThat(result.getPageable().getPageSize()).isEqualTo(PAGING_RECORD.getPageSize());

        verify(postRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("should return the posts, correct page, number of items per page and " +
            "if the page is sorted correctly")
    void findAllCompleteFunction() {
        List<Post> sortedPostList = POST_LIST_RECORD.stream()
                .sorted(Comparator.comparing(Post::getTitle))
                .toList();

        Page<Post> sortedPosts = new PageImpl<>(sortedPostList, PAGING_RECORD, 1);

        when(postRepository.findAll(any(Pageable.class))).thenReturn(sortedPosts);
        Page<Post> result = postService.findAll(0, 5, "title");

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo(sortedPostList);
        assertThat(result.getContent().get(1)).isEqualTo(POST_RECORD_2);
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getTotalElements()).isEqualTo(POST_LIST_RECORD.toArray().length);
        assertThat(result.getPageable().getPageNumber()).isEqualTo(PAGING_RECORD.getPageNumber());
        assertThat(result.getPageable().getSort()).isEqualTo(PAGING_RECORD.getSort());
        assertThat(result.getPageable().getPageSize()).isEqualTo(PAGING_RECORD.getPageSize());

        verify(postRepository, times(1)).findAll(any(Pageable.class));
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
        when(postRepository.findById(any(UUID.class))).thenThrow(new ResourceNotFoundException(anyString()));

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