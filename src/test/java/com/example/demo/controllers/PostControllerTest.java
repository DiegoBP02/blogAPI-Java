package com.example.demo.controllers;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.dtos.PostDTO;
import com.example.demo.entities.Post;
import com.example.demo.entities.User;
import com.example.demo.entities.enums.PostCategory;
import com.example.demo.entities.enums.Role;
import com.example.demo.services.PostService;
import com.example.demo.services.exceptions.ResourceNotFoundException;
import com.example.demo.services.exceptions.UnauthorizedAccessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("PostControllerTest")
class PostControllerTest extends ApplicationConfigTest {

    private static final String PATH = "/posts";
    Set<PostCategory> CATEGORIES_RECORD = new HashSet<>(Collections.singleton(PostCategory.valueOf(1)));
    PostDTO POSTDTO_RECORD = new PostDTO("title", "contentmusthaveatleast30characters", CATEGORIES_RECORD);
    Post POST_RECORD = new Post(POSTDTO_RECORD.getTitle(), POSTDTO_RECORD.getContent(), Instant.now(), CATEGORIES_RECORD, USER_RECORD);
    User USER_RECORD = new User("a", "b", "c", Role.ROLE_USER);
    @MockBean
    private PostService postService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser()
    @DisplayName("should create a post")
    void createPost() throws Exception {
        when(postService.create(any(PostDTO.class))).thenReturn(POST_RECORD);

        MockHttpServletRequestBuilder mockRequest = MockMvcRequestBuilders
                .post(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(POSTDTO_RECORD));

        mockMvc.perform(mockRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$.title", is(POST_RECORD.getTitle())));

        verify(postService, times(1)).create(any(PostDTO.class));
    }

    @Test
    @WithMockUser()
    @DisplayName("should throw MethodArgumentNotValidException for invalid request body")
    void createPostInvalidBody() throws Exception {
        PostDTO postDTO = new PostDTO();

        MockHttpServletRequestBuilder mockRequest = MockMvcRequestBuilders
                .post(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(postDTO));

        mockMvc.perform(mockRequest)
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException));
    }

    @Test
    @WithMockUser()
    @DisplayName("should return a list of posts")
    void findAll() throws Exception {
        List<Post> postList = Collections.singletonList(POST_RECORD);
        Pageable paging = PageRequest.of(0, 5, Sort.by("title"));
        Page<Post> posts = new PageImpl<>(postList, paging, 1);
        when(postService.findAll(anyInt(), anyInt(), anyString())).thenReturn(posts);

        mockMvc.perform(MockMvcRequestBuilders
                        .get(PATH)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title", is(POST_RECORD.getTitle())));

        verify(postService, times(1)).findAll(anyInt(), anyInt(), anyString());
    }

    @Test
    @WithMockUser()
    @DisplayName("should return a list of posts with pagination information")
    void findAllPagination() throws Exception {
        List<Post> postList = Collections.singletonList(POST_RECORD);
        Pageable paging = PageRequest.of(0, 5, Sort.by("title"));
        Page<Post> posts = new PageImpl<>(postList, paging, 1);
        when(postService.findAll(anyInt(), anyInt(), anyString())).thenReturn(posts);

        mockMvc.perform(MockMvcRequestBuilders
                        .get(PATH)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title", is(POST_RECORD.getTitle())))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.pageable.sort.empty").value(false))
                .andExpect(jsonPath("$.pageable.sort.sorted").value(true))
                .andExpect(jsonPath("$.pageable.sort.unsorted").value(false))
                .andExpect(jsonPath("$.pageable.offset").value(0))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.pageable.pageSize").value(5))
                .andExpect(jsonPath("$.pageable.unpaged").value(false))
                .andExpect(jsonPath("$.pageable.paged").value(true))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.sort.empty").value(false))
                .andExpect(jsonPath("$.sort.sorted").value(true))
                .andExpect(jsonPath("$.sort.unsorted").value(false))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.numberOfElements").value(1))
                .andExpect(jsonPath("$.empty").value(false));


        verify(postService, times(1)).findAll(anyInt(), anyInt(), anyString());
    }

    @Test
    @WithMockUser
    @DisplayName("should return a post")
    void findById() throws Exception {
        when(postService.findById(any(UUID.class))).thenReturn(POST_RECORD);
        mockMvc.perform(MockMvcRequestBuilders
                        .get(PATH + "/" + UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$.title", is(POST_RECORD.getTitle())));

        verify(postService, times(1)).findById(any(UUID.class));
    }

    @Test
    @WithMockUser
    @DisplayName("should throw ResourceNotFoundException for invalid id")
    void findByIdResourceNotFoundException() throws Exception {
        UUID randomId = UUID.randomUUID();
        when(postService.findById(any(UUID.class))).thenThrow(new ResourceNotFoundException(randomId));

        mockMvc.perform(MockMvcRequestBuilders
                        .get(PATH + "/" + UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(result ->
                        assertTrue(result.getResolvedException() instanceof ResourceNotFoundException))
                .andExpect(result ->
                        assertEquals("Resource not found. Id " + randomId, Objects.requireNonNull(result.getResolvedException()).getMessage()));

        verify(postService, times(1)).findById(any(UUID.class));
    }

    @Test
    @WithMockUser
    @DisplayName("should update a post")
    void update() throws Exception {
        Post updatedPost = new Post("new title", "contentmusthaveatleast30characters", Instant.now(), CATEGORIES_RECORD, USER_RECORD);

        when(postService.update(any(UUID.class), any(Post.class))).thenReturn(updatedPost);

        MockHttpServletRequestBuilder mockRequest = MockMvcRequestBuilders
                .patch(PATH + "/" + UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(updatedPost));

        mockMvc.perform(mockRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$.title", is(updatedPost.getTitle())));

        verify(postService, times(1)).update(any(UUID.class), any(Post.class));
    }

    @Test
    @WithMockUser
    @DisplayName("should throw UnauthorizedAccessException for invalid checkOwnership")
    void updateUnauthorizedAccessException() throws Exception {
        Post updatedPost = new Post("new title", "contentmusthaveatleast30characters", Instant.now(), CATEGORIES_RECORD, USER_RECORD);
        when(postService.update(any(UUID.class), any(Post.class)))
                .thenThrow(new UnauthorizedAccessException("You are not authorized to update this object. It does not belong to you"));

        MockHttpServletRequestBuilder mockRequest = MockMvcRequestBuilders
                .patch(PATH + "/" + UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(updatedPost));

        mockMvc.perform(mockRequest)
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException() instanceof UnauthorizedAccessException))
                .andExpect(result ->
                        assertEquals("You are not authorized to update this object. It does not belong to you", Objects.requireNonNull(result.getResolvedException()).getMessage()));


        verify(postService, times(1)).update(any(UUID.class), any(Post.class));
    }

    @Test
    @WithMockUser
    @DisplayName("should delete a post")
    void delete() throws Exception {
        doNothing().when(postService).delete(any(UUID.class));

        mockMvc.perform(MockMvcRequestBuilders
                        .delete(PATH + "/" + UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(postService, times(1)).delete(any(UUID.class));
    }

    @Test
    @WithMockUser
    @DisplayName("should throw UnauthorizedAccessException for invalid checkOwnership")
    void deleteUnauthorizedAccessException() throws Exception {
        doThrow(new UnauthorizedAccessException("You are not authorized to update this object. It does not belong to you"))
                .when(postService).delete(any(UUID.class));

        mockMvc.perform(MockMvcRequestBuilders
                        .delete(PATH + "/" + UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException() instanceof UnauthorizedAccessException))
                .andExpect(result ->
                        assertEquals("You are not authorized to update this object. It does not belong to you", Objects.requireNonNull(result.getResolvedException()).getMessage()));


        verify(postService, times(1)).delete(any(UUID.class));
    }

    @Test
    @WithMockUser
    @DisplayName("should increase the upvote")
    void increaseUpvote() throws Exception {
        Post post = new Post("title", "contentmusthaveatleast30characters", Instant.now(), CATEGORIES_RECORD, USER_RECORD);
        when(postService.increaseUpvote(any(UUID.class))).thenAnswer(invocation -> {
            post.increaseUpvote(UUID.randomUUID());
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

        assertThat(post.getUpvotes()).isEqualTo(1);
        assertThat(post.getUsersUpvotesId()).size().isEqualTo(1);

        verify(postService, times(1)).increaseUpvote(any(UUID.class));
    }

    @Test
    @WithMockUser
    @DisplayName("should return conflict if user already upvoted")
    void increaseUpvoteConflict() throws Exception {
        Post post = new Post("title", "contentmusthaveatleast30characters", Instant.now(), CATEGORIES_RECORD, USER_RECORD);
        when(postService.increaseUpvote(any(UUID.class))).thenAnswer(invocation -> {
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

        verify(postService, times(1)).increaseUpvote(any(UUID.class));
    }
}