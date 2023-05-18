package com.example.demo.services;

import com.example.demo.dtos.PostDTO;
import com.example.demo.entities.Post;
import com.example.demo.entities.User;
import com.example.demo.repositories.PostRepository;
import com.example.demo.services.exceptions.DatabaseException;
import com.example.demo.services.exceptions.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    public Post create(PostDTO post, User user) {
        return postRepository.save(new Post(post.getTitle(), post.getContent(), Instant.now(), user));
    }

    public List<Post> findAll() {
        return postRepository.findAll();
    }

    public Post findById(Long id) {
        Optional<Post> post = postRepository.findById(id);
        return post.orElseThrow(() -> new ResourceNotFoundException(id));
    }

    public Post update(Long id, Post obj) {
        try{
            Post entity = postRepository.getReferenceById(id);
            updateData(entity, obj);
            return postRepository.save(entity);
        } catch(EntityNotFoundException e){
            throw new ResourceNotFoundException(id);
        }
    }

    private void updateData(Post entity, Post obj) {
        entity.setTitle(obj.getTitle());
        entity.setContent(obj.getContent());
    }

    public void delete(Long id) {
        try {
            postRepository.deleteById(id);
        } catch(EmptyResultDataAccessException e){
            throw new ResourceNotFoundException(id);
        } catch(DataIntegrityViolationException e){
            throw new DatabaseException(e.getMessage());
        }
    }

    public String increaseUpvote(Long id, Long userId) {
        Post post = this.findById(id);
        if(post.getUsersUpvotesId().contains(userId)){
            return "User already upvoted!";
        }
        post.increaseUpvote(userId);
        postRepository.save(post);
        return "Successful!";
    }
}
