package com.example.demo.services;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.dtos.PostDTO;
import com.example.demo.entities.Post;
import com.example.demo.entities.User;
import com.example.demo.entities.enums.PostCategory;
import com.example.demo.entities.enums.Role;
import com.example.demo.repositories.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.*;

@DisplayName("PostServiceTest")
class PostServiceTest extends ApplicationConfigTest {
    @MockBean
    private PostRepository postRepository;

    @Autowired
    PostService postService;

    @Test
    @DisplayName("should create a post")
    public void createPost() {
        Set<PostCategory> categories = new HashSet<>(Collections.singleton(PostCategory.valueOf(1)));
        User user = new User("a", "b", "c", Role.ROLE_USER);
        PostDTO dto = new PostDTO("title", "content", categories);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        Post post = mock(Post.class);
        when(postRepository.save(ArgumentMatchers.any(Post.class))).thenReturn(post);

        postService.create(dto);

        verify(postRepository, times(1)).save(ArgumentMatchers.any(Post.class));
    }
}