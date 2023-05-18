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

}
