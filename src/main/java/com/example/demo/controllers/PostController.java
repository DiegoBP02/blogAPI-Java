package com.example.demo.controllers;

import com.example.demo.dtos.PostDTO;
import com.example.demo.entities.Post;
import com.example.demo.services.PostService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping(value = "/posts")
public class PostController {

    @Autowired
    private PostService postService;

    @PostMapping
    public ResponseEntity<Post> createPost(@Valid @RequestBody PostDTO post) {
        Post result = postService.create(post);

        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(result.getId()).toUri();

        return ResponseEntity.created(uri).body(result);
    }

    @GetMapping
    public ResponseEntity<Page<Post>> findAll(
            @RequestParam(defaultValue = "0") Integer pageNo,
            @RequestParam(defaultValue = "5") Integer pageSize,
            @RequestParam(defaultValue = "title") String sortBy
    ) {
        Page<Post> posts = postService.findAll(pageNo, pageSize, sortBy);
        return ResponseEntity.ok().body(posts);
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<Post> findById(@PathVariable UUID id) {
        return ResponseEntity.ok().body(postService.findById(id));
    }

    @PatchMapping(value = "/{id}")
    public ResponseEntity<Post> update(@PathVariable UUID id, @RequestBody Post obj) {
        return ResponseEntity.ok().body(postService.update(id, obj));
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Post> delete(@PathVariable UUID id) {
        postService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/upvote")
    public ResponseEntity<String> increaseUpvote(@PathVariable UUID id) {
        boolean result = postService.increaseUpvote(id);
        HttpStatus status = result ? HttpStatus.OK : HttpStatus.CONFLICT;
        String message = result ? "Successful!" : "User already upvoted! You can only upvote once!";
        return ResponseEntity.status(status).body(message);
    }
}
