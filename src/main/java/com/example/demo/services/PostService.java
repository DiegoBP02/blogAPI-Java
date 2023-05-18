package com.example.demo.services;

import com.example.demo.dtos.PostDTO;
import com.example.demo.entities.Post;
import com.example.demo.entities.User;
import com.example.demo.repositories.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    public Post create(PostDTO post, User user) {
        return postRepository.save(new Post(post.getTitle(), post.getContent(), Instant.now(), user));
    }
}
