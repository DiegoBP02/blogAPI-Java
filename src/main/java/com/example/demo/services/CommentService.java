package com.example.demo.services;

import com.example.demo.dtos.CommentDTO;
import com.example.demo.dtos.PostDTO;
import com.example.demo.entities.Comment;
import com.example.demo.entities.Post;
import com.example.demo.entities.User;
import com.example.demo.entities.enums.Role;
import com.example.demo.repositories.CommentRepository;
import com.example.demo.repositories.PostRepository;
import com.example.demo.services.exceptions.DatabaseException;
import com.example.demo.services.exceptions.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.demo.services.utils.checkOwnership.checkOwnership;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostService postService;

    public Comment create(CommentDTO comment) {
        try {
            User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
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
            User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            checkOwnership(user, entity.getAuthor().getId());
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
            Comment entity = commentRepository.getReferenceById(id);

            User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String role = user.getAuthorities().stream().toList().get(0).getAuthority();

            if(!role.equals(Role.ROLE_ADMIN.toString())) {
                checkOwnership(user, entity.getAuthor().getId());
            }

            commentRepository.deleteById(id);
        } catch(EmptyResultDataAccessException e){
            throw new ResourceNotFoundException(id);
        } catch(DataIntegrityViolationException e){
            throw new DatabaseException(e.getMessage());
        }
    }

    public String increaseUpvote(UUID id) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID userId = user.getId();
        Comment comment = this.findById(id);
        if(comment.getUsersUpvotesId().contains(userId)){
            return "User already upvoted! You can only upvote once!";
        }
        comment.increaseUpvote(userId);
        commentRepository.save(comment);
        return "Successful!";
    }
}
