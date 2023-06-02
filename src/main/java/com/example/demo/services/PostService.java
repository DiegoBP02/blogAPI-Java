package com.example.demo.services;

import com.example.demo.dtos.PostDTO;
import com.example.demo.entities.Post;
import com.example.demo.entities.User;
import com.example.demo.entities.enums.Role;
import com.example.demo.repositories.PostRepository;
import com.example.demo.services.exceptions.DatabaseException;
import com.example.demo.services.exceptions.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.demo.services.utils.checkOwnership.checkOwnership;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    public Post create(PostDTO post) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return postRepository.save(new Post(post.getTitle(), post.getContent(), Instant.now(), post.getCategories(), user));
    }

    public Page<Post> findAll(Integer pageNo, Integer pageSize, String sortBy) {
        Pageable paging = PageRequest.of(pageNo,pageSize, Sort.by(sortBy));

        return postRepository.findAll(paging);
    }

    public Post findById(UUID id) {
        Optional<Post> post = postRepository.findById(id);
        return post.orElseThrow(() -> new ResourceNotFoundException(id));
    }

    public Post update(UUID id, Post obj) {
        try {
            Post entity = postRepository.getReferenceById(id);
            User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            checkOwnership(user, entity.getAuthor().getId());
            updateData(entity, obj);

            return postRepository.save(entity);
        } catch (EntityNotFoundException e) {
            throw new ResourceNotFoundException(id);
        }
    }

    private void updateData(Post entity, Post obj) {
        entity.setTitle(obj.getTitle());
        entity.setContent(obj.getContent());
    }

    public void delete(UUID id) {
        try {
            Post entity = postRepository.getReferenceById(id);

            User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String role = user.getAuthorities().stream().toList().get(0).getAuthority();

            if (!role.equals(Role.ROLE_ADMIN.toString())) {
                checkOwnership(user, entity.getAuthor().getId());
            }

            postRepository.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new ResourceNotFoundException(id);
        } catch (DataIntegrityViolationException e) {
            throw new DatabaseException(e.getMessage());
        }
    }

    public boolean increaseUpvote(UUID id) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID userId = user.getId();
        Post post = this.findById(id);
        if (post.getUsersUpvotesId().contains(userId)) {
            return false;
        }
        post.increaseUpvote(userId);
        postRepository.save(post);
        return true;
    }
}
