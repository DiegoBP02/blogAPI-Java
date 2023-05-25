package com.example.demo.services;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.dtos.CommentDTO;
import com.example.demo.entities.Comment;
import com.example.demo.entities.Post;
import com.example.demo.entities.User;
import com.example.demo.entities.enums.PostCategory;
import com.example.demo.entities.enums.Role;
import com.example.demo.repositories.CommentRepository;
import com.example.demo.services.exceptions.ResourceNotFoundException;
import com.example.demo.services.exceptions.UnauthorizedAccessException;
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

@DisplayName("CommentServiceTest")
class CommentServiceTest extends ApplicationConfigTest {
    @MockBean
    private CommentRepository commentRepository;

    @MockBean
    private PostService postService;

    @Autowired
    CommentService commentService;

    Set<PostCategory> CATEGORIES_RECORD = new HashSet<>(Collections.singleton(PostCategory.valueOf(1)));
    User USER_RECORD = new User("a", "b", "c", Role.ROLE_USER);
    Post POST_RECORD = new Post("title", "contentmusthaveatleast30characters", Instant.now(), CATEGORIES_RECORD, USER_RECORD);
    CommentDTO COMMENT_DTO_RECORD = new CommentDTO("content", UUID.randomUUID());
    Comment COMMENT_RECORD = new Comment(COMMENT_DTO_RECORD.getContent(), Instant.now(), POST_RECORD, USER_RECORD);

    @Test
    @DisplayName("should create a comment")
    void create() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(USER_RECORD);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(commentRepository.save(any(Comment.class))).thenReturn(COMMENT_RECORD);
        when(postService.findById(any(UUID.class))).thenReturn(POST_RECORD);

        Comment result = commentService.create(COMMENT_DTO_RECORD);

        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(Comment.class);
        assertThat(result.getContent()).isEqualTo(COMMENT_RECORD.getContent());
        assertThat(result.getPublishDate()).isEqualTo(COMMENT_RECORD.getPublishDate());
        assertThat(result.getAuthor()).isEqualTo(COMMENT_RECORD.getAuthor());
        assertThat(result.getUpvotes()).isEqualTo(COMMENT_RECORD.getUpvotes());
        assertThat(result.getUsersUpvotesId()).isEqualTo(COMMENT_RECORD.getUsersUpvotesId());

        verify(authentication, times(1)).getPrincipal();
        verify(securityContext, times(1)).getAuthentication();
        verify(commentRepository, times(1)).save(any(Comment.class));
        verify(postService, times(1)).findById(any(UUID.class));
    }

    @Test
    @DisplayName("should get all comments")
    void findAll() {
        List<Comment> comments = Collections.singletonList(COMMENT_RECORD);

        when(commentRepository.findAll()).thenReturn(comments);

        List<Comment> result = commentService.findAll();

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(comments);

        verify(commentRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("should get a comment")
    void findById() {
        when(commentRepository.findById(any(UUID.class))).thenReturn(Optional.of(COMMENT_RECORD));

        Comment result = commentService.findById(UUID.randomUUID());

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(COMMENT_RECORD);

        verify(commentRepository, times(1)).findById(any(UUID.class));
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException if no comment is found")
    void findByIdNotFound() {
        when(commentRepository.findById(any(UUID.class))).thenThrow(ResourceNotFoundException.class);

        assertThrows(ResourceNotFoundException.class, () -> postService.findById(UUID.randomUUID()));

        verify(commentRepository, times(1)).findById(any(UUID.class));
    }

    @Test
    @DisplayName("should update a comment")
    void update() {
        ReflectionTestUtils.setField(USER_RECORD, "id", UUID.randomUUID());

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(USER_RECORD);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(commentRepository.getReferenceById(any(UUID.class))).thenReturn(COMMENT_RECORD);
        when(commentRepository.save(any(Comment.class))).thenReturn(COMMENT_RECORD);

        COMMENT_RECORD.setContent("new content");

        Comment result = commentService.update(UUID.randomUUID(), COMMENT_RECORD);

        assertThat(result.getContent()).isEqualTo(COMMENT_RECORD.getContent());

        verify(authentication, times(1)).getPrincipal();
        verify(securityContext, times(1)).getAuthentication();
        verify(commentRepository, times(1)).getReferenceById(any(UUID.class));
        verify(commentRepository, times(1)).save(any(Comment.class));
    }

    @Test
    @DisplayName("should throw UnauthorizedAccessException if checkOwnership is invalid")
    void updateUnauthorizedAccessException() {
        User user2 = new User("x","y","z", Role.ROLE_USER);
        ReflectionTestUtils.setField(user2, "id", UUID.randomUUID());

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user2);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(commentRepository.getReferenceById(any(UUID.class))).thenReturn(COMMENT_RECORD);
        when(commentRepository.save(any(Comment.class))).thenReturn(COMMENT_RECORD);

        assertThrows(UnauthorizedAccessException.class,
                () -> commentService.update(UUID.randomUUID(), COMMENT_RECORD));

        verify(authentication, times(1)).getPrincipal();
        verify(securityContext, times(1)).getAuthentication();
        verify(commentRepository, times(1)).getReferenceById(any(UUID.class));
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("should delete a comment")
    void delete() {
        ReflectionTestUtils.setField(USER_RECORD, "id", UUID.randomUUID());

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(USER_RECORD);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(commentRepository.getReferenceById(any(UUID.class))).thenReturn(COMMENT_RECORD);
        doNothing().when(commentRepository).deleteById(any(UUID.class));

        commentService.delete(UUID.randomUUID());

        verify(authentication, times(1)).getPrincipal();
        verify(securityContext, times(1)).getAuthentication();
        verify(commentRepository, times(1)).getReferenceById(any(UUID.class));
        verify(commentRepository, times(1)).deleteById(any(UUID.class));
    }

    @Test
    @DisplayName("should throw UnauthorizedAccessException if checkOwnership is invalid")
    void deleteUnauthorizedAccessException() {
        User user2 = new User("x","y","z", Role.ROLE_USER);
        ReflectionTestUtils.setField(user2, "id", UUID.randomUUID());

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user2);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(commentRepository.getReferenceById(any(UUID.class))).thenReturn(COMMENT_RECORD);
        doNothing().when(commentRepository).deleteById(any(UUID.class));

        assertThrows(UnauthorizedAccessException.class,
                () -> commentService.delete(UUID.randomUUID()));

        verify(authentication, times(1)).getPrincipal();
        verify(securityContext, times(1)).getAuthentication();
        verify(commentRepository, times(1)).getReferenceById(any(UUID.class));
        verify(commentRepository, never()).deleteById(any(UUID.class));
    }

    @Test
    @DisplayName("should bypass checkOwnership if user is an admin")
    void deleteAdmin() {
        User user = new User("x","y","z", Role.ROLE_ADMIN);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(commentRepository.getReferenceById(any(UUID.class))).thenReturn(COMMENT_RECORD);
        doNothing().when(commentRepository).deleteById(any(UUID.class));

        commentService.delete(UUID.randomUUID());

        verify(authentication, times(1)).getPrincipal();
        verify(securityContext, times(1)).getAuthentication();
        verify(commentRepository, times(1)).getReferenceById(any(UUID.class));
        verify(commentRepository, times(1)).deleteById(any(UUID.class));
    }

    @Test
    @DisplayName("should increase the upvote")
    void increaseUpvote() {
        ReflectionTestUtils.setField(USER_RECORD, "id", UUID.randomUUID());

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(USER_RECORD);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(commentRepository.findById(any(UUID.class))).thenReturn(Optional.of(COMMENT_RECORD));

        boolean result = commentService.increaseUpvote(UUID.randomUUID());

        assertThat(result).isTrue();

        verify(authentication, times(1)).getPrincipal();
        verify(securityContext, times(1)).getAuthentication();
        verify(commentRepository, times(1)).findById(any(UUID.class));
        verify(commentRepository, times(1)).save(any(Comment.class));
    }

    @Test
    @DisplayName("should not increase the upvote if user already upvoted")
    void increaseUpvoteDeny() {
        ReflectionTestUtils.setField(USER_RECORD, "id", UUID.randomUUID());
        COMMENT_RECORD.increaseUpvote(USER_RECORD.getId());

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(USER_RECORD);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(commentRepository.findById(any(UUID.class))).thenReturn(Optional.of(COMMENT_RECORD));

        boolean result = commentService.increaseUpvote(UUID.randomUUID());

        assertThat(result).isFalse();

        verify(authentication, times(1)).getPrincipal();
        verify(securityContext, times(1)).getAuthentication();
        verify(commentRepository, times(1)).findById(any(UUID.class));
        verify(commentRepository, never()).save(any(Comment.class));
    }
}