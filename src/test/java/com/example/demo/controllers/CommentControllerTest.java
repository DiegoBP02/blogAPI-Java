package com.example.demo.controllers;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.dtos.CommentDTO;
import com.example.demo.entities.Comment;
import com.example.demo.entities.Post;
import com.example.demo.entities.User;
import com.example.demo.entities.enums.PostCategory;
import com.example.demo.entities.enums.Role;
import com.example.demo.services.CommentService;
import com.example.demo.services.exceptions.ResourceNotFoundException;
import com.example.demo.services.exceptions.UnauthorizedAccessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("CommentControllerTest")
class CommentControllerTest extends ApplicationConfigTest {
    @MockBean
    private CommentService commentService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String PATH = "/comments";

    User USER_RECORD = new User("a", "b", "c", Role.ROLE_USER);
    Set<PostCategory> CATEGORIES_RECORD = new HashSet<>(Collections.singleton(PostCategory.valueOf(1)));
    Post POST_RECORD = new Post("title", "contentmusthaveatleast30characters", Instant.now(), CATEGORIES_RECORD, USER_RECORD);
    CommentDTO COMMENT_DTO_RECORD = new CommentDTO("content", UUID.randomUUID());
    Comment COMMENT_RECORD = new Comment(COMMENT_DTO_RECORD.getContent(), Instant.now(), POST_RECORD, USER_RECORD);

    @Test
    @WithMockUser()
    @DisplayName("should create a comment")
    void createComment() throws Exception {
        ReflectionTestUtils.setField(POST_RECORD, "id", UUID.randomUUID());
        CommentDTO commentDTO = new CommentDTO("content", UUID.randomUUID());

        when(commentService.create(any(CommentDTO.class))).thenReturn(COMMENT_RECORD);

        MockHttpServletRequestBuilder mockRequest = MockMvcRequestBuilders
                .post(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(commentDTO));

        mockMvc.perform(mockRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$.content", is(COMMENT_RECORD.getContent())));

        verify(commentService, times(1)).create(any(CommentDTO.class));
    }

    @Test
    @WithMockUser()
    @DisplayName("should throw MethodArgumentNotValidException for invalid request body")
    void createCommentInvalidBody() throws Exception {
        CommentDTO commentDTO = new CommentDTO();

        MockHttpServletRequestBuilder mockRequest = MockMvcRequestBuilders
                .post(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(commentDTO));

        mockMvc.perform(mockRequest)
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException));
    }

    @Test
    @WithMockUser()
    @DisplayName("should return a list of comments")
    void findAll() throws Exception {
        List<Comment> comments = new ArrayList<>(Collections.singletonList(COMMENT_RECORD));

        when(commentService.findAll()).thenReturn(comments);

        mockMvc.perform(MockMvcRequestBuilders
                        .get(PATH)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].content", is(COMMENT_RECORD.getContent())));

        verify(commentService, times(1)).findAll();
    }

    @Test
    @WithMockUser
    @DisplayName("should return a comment")
    void findById() throws Exception {
        when(commentService.findById(any(UUID.class))).thenReturn(COMMENT_RECORD);

        mockMvc.perform(MockMvcRequestBuilders
                        .get(PATH + "/" + UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$.content", is(COMMENT_RECORD.getContent())));

        verify(commentService, times(1)).findById(any(UUID.class));
    }

    @Test
    @WithMockUser
    @DisplayName("should throw ResourceNotFoundException for invalid id")
    void findByIdResourceNotFoundException() throws Exception {
        UUID randomId = UUID.randomUUID();
        when(commentService.findById(any(UUID.class))).thenThrow(new ResourceNotFoundException(randomId));

        mockMvc.perform(MockMvcRequestBuilders
                        .get(PATH + "/" + UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(result ->
                        assertTrue(result.getResolvedException() instanceof ResourceNotFoundException))
                .andExpect(result ->
                        assertEquals("Resource not found. Id " + randomId, Objects.requireNonNull(result.getResolvedException()).getMessage()));

        verify(commentService, times(1)).findById(any(UUID.class));
    }

    @Test
    @WithMockUser
    @DisplayName("should update a comment")
    void update() throws Exception {
        Comment updatedComment = new Comment("new content", Instant.now(), POST_RECORD, USER_RECORD);

        when(commentService.update(any(UUID.class), any(Comment.class))).thenReturn(updatedComment);

        MockHttpServletRequestBuilder mockRequest = MockMvcRequestBuilders
                .patch(PATH + "/" + UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(updatedComment));

        mockMvc.perform(mockRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$.content", is(updatedComment.getContent())));

        verify(commentService, times(1)).update(any(UUID.class), any(Comment.class));
    }

    @Test
    @WithMockUser
    @DisplayName("should throw UnauthorizedAccessException for invalid checkOwnership")
    void updateUnauthorizedAccessException() throws Exception {
        Comment updatedComment = new Comment("new content", Instant.now(), POST_RECORD, USER_RECORD);

        when(commentService.update(any(UUID.class), any(Comment.class)))
                .thenThrow(new UnauthorizedAccessException("You are not authorized to update this object. It does not belong to you"));

        MockHttpServletRequestBuilder mockRequest = MockMvcRequestBuilders
                .patch(PATH + "/" + UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(updatedComment));

        mockMvc.perform(mockRequest)
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException() instanceof UnauthorizedAccessException))
                .andExpect(result ->
                        assertEquals("You are not authorized to update this object. It does not belong to you", Objects.requireNonNull(result.getResolvedException()).getMessage()));


        verify(commentService, times(1)).update(any(UUID.class), any(Comment.class));
    }

    @Test
    @WithMockUser
    @DisplayName("should delete a comment")
    void delete() throws Exception {
        doNothing().when(commentService).delete(any(UUID.class));

        mockMvc.perform(MockMvcRequestBuilders
                        .delete(PATH + "/" + UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(commentService, times(1)).delete(any(UUID.class));
    }

    @Test
    @WithMockUser
    @DisplayName("should throw UnauthorizedAccessException for invalid checkOwnership")
    void deleteUnauthorizedAccessException() throws Exception {
        doThrow(new UnauthorizedAccessException("You are not authorized to update this object. It does not belong to you"))
                .when(commentService).delete(any(UUID.class));

        mockMvc.perform(MockMvcRequestBuilders
                        .delete(PATH + "/" + UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException() instanceof UnauthorizedAccessException))
                .andExpect(result ->
                        assertEquals("You are not authorized to update this object. It does not belong to you", Objects.requireNonNull(result.getResolvedException()).getMessage()));


        verify(commentService, times(1)).delete(any(UUID.class));
    }

    @Test
    @WithMockUser
    @DisplayName("should increase the upvote")
    void increaseUpvote() throws Exception {
        when(commentService.increaseUpvote(any(UUID.class))).thenAnswer(invocation -> {
            COMMENT_RECORD.increaseUpvote(UUID.randomUUID());
            return true;
        });

        MockHttpServletRequestBuilder mockRequest = MockMvcRequestBuilders
                .post(PATH + "/" + UUID.randomUUID().toString() + "/upvote")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(mockRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(content().string("Successful!"))
        ;

        assertThat(COMMENT_RECORD.getUpvotes()).isEqualTo(1);
        assertThat(COMMENT_RECORD.getUsersUpvotesId()).size().isEqualTo(1);

        verify(commentService, times(1)).increaseUpvote(any(UUID.class));
    }

    @Test
    @WithMockUser
    @DisplayName("should return conflict if user already upvoted")
    void increaseUpvoteConflict() throws Exception {
        when(commentService.increaseUpvote(any(UUID.class))).thenAnswer(invocation -> {
            return false;
        });

        MockHttpServletRequestBuilder mockRequest = MockMvcRequestBuilders
                .post(PATH + "/" + UUID.randomUUID().toString() + "/upvote")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(mockRequest)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(content().string("User already upvoted! You can only upvote once!"))
        ;

        verify(commentService, times(1)).increaseUpvote(any(UUID.class));
    }
}