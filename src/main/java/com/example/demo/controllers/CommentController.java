package com.example.demo.controllers;

import com.example.demo.dtos.CommentDTO;
import com.example.demo.entities.Comment;
import com.example.demo.services.CommentService;
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
@RequestMapping(value = "/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @PostMapping
    public ResponseEntity<Comment> createComment(@Valid @RequestBody CommentDTO comment) {
        Comment result = commentService.create(comment);

        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(result.getId()).toUri();

        return ResponseEntity.created(uri).body(result);
    }

    @GetMapping
    public ResponseEntity<List<Comment>> findAll() {
        return ResponseEntity.ok().body(commentService.findAll());
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<Comment> findById(@PathVariable UUID id) {
        return ResponseEntity.ok().body(commentService.findById(id));
    }

    @PatchMapping(value = "/{id}")
    public ResponseEntity<Comment> update(@PathVariable UUID id, @RequestBody Comment obj) {
        return ResponseEntity.ok().body(commentService.update(id, obj));
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Comment> delete(@PathVariable UUID id) {
        commentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/upvote")
    public ResponseEntity<String> increaseUpvote(@PathVariable UUID id) {
        boolean result = commentService.increaseUpvote(id);
        HttpStatus status = result ? HttpStatus.OK : HttpStatus.CONFLICT;
        String message = result ? "Successful!" : "User already upvoted! You can only upvote once!";
        return ResponseEntity.status(status).body(message);
    }

}
