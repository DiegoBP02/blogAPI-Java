package com.example.demo.controllers;

import com.example.demo.dtos.PostDTO;
import com.example.demo.entities.Post;
import com.example.demo.services.PostService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
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
    public ResponseEntity<List<Post>> findAll(){
        return ResponseEntity.ok().body(postService.findAll());
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<Post> findById(@PathVariable UUID id){
        return ResponseEntity.ok().body(postService.findById(id));
    }

    @PatchMapping(value = "/{id}")
    public ResponseEntity<Post> update(@PathVariable UUID id, @RequestBody Post obj){
        return ResponseEntity.ok().body(postService.update(id, obj));
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Post> delete(@PathVariable UUID id){
        postService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/upvote")
    public ResponseEntity<String> increaseUpvote(@PathVariable UUID id){
        String result = postService.increaseUpvote(id);
        HttpStatus status = result.equals("Successful!") ? HttpStatus.OK : HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(result);
    }
}
