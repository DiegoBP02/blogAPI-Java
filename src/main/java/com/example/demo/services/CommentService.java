package com.example.demo.services;

import com.example.demo.dtos.CommentDTO;
import com.example.demo.dtos.PostDTO;
import com.example.demo.entities.Comment;
import com.example.demo.entities.Post;
import com.example.demo.entities.User;
import com.example.demo.repositories.CommentRepository;
import com.example.demo.repositories.PostRepository;
import com.example.demo.services.exceptions.DatabaseException;
import com.example.demo.services.exceptions.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostService postService;

    public Comment create(CommentDTO comment, User user) {
        try {
            Post post = postService.findById(comment.getPostId());
            return commentRepository.save(new Comment(comment.getContent(), Instant.now(), post, user));
        } catch (EntityNotFoundException e) {
            throw new ResourceNotFoundException(comment.getPostId());
        }
    }

    public List<Comment> findAll() {
        return commentRepository.findAll();
    }

    public Comment findById(UUID id) {
        Optional<Comment> post = commentRepository.findById(id);
        return post.orElseThrow(() -> new ResourceNotFoundException(id));
    }

    public Comment update(UUID id, Comment obj) {
        try{
            Comment entity = commentRepository.getReferenceById(id);
            updateData(entity, obj);
            return commentRepository.save(entity);
        } catch(EntityNotFoundException e){
            throw new ResourceNotFoundException(id);
        }
    }

    private void updateData(Comment entity, Comment obj) {
        entity.setContent(obj.getContent());
    }

    public void delete(UUID id) {
        try {
            commentRepository.deleteById(id);
        } catch(EmptyResultDataAccessException e){
            throw new ResourceNotFoundException(id);
        } catch(DataIntegrityViolationException e){
            throw new DatabaseException(e.getMessage());
        }
    }

    public String increaseUpvote(UUID id, UUID userId) {
        Comment comment = this.findById(id);
        if(comment.getUsersUpvotesId().contains(userId)){
            return "User already upvoted!";
        }
        comment.increaseUpvote(userId);
        commentRepository.save(comment);
        return "Successful!";
    }
}
