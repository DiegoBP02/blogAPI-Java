package com.example.demo.controllers;

import com.example.demo.dtos.CommentDTO;
import com.example.demo.dtos.PostDTO;
import com.example.demo.entities.Comment;
import com.example.demo.entities.Post;
import com.example.demo.entities.User;
import com.example.demo.repositories.PostRepository;
import com.example.demo.services.CommentService;
import com.example.demo.services.PostService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(value = "/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @PostMapping
    public ResponseEntity<Comment> createComment(@RequestBody CommentDTO comment, HttpServletRequest request) {
        User user = (User) request.getAttribute("user");

        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(user.getId()).toUri();

        return ResponseEntity.created(uri).body(commentService.create(comment, user));
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<Comment>> findAll(){
        return ResponseEntity.ok().body(commentService.findAll());
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<Comment> findById(@PathVariable Long id){
        return ResponseEntity.ok().body(commentService.findById(id));
    }

    @RequestMapping(method = RequestMethod.PATCH,value = "/{id}")
    public ResponseEntity<Comment> update(@PathVariable Long id, @RequestBody Comment obj){
        return ResponseEntity.ok().body(commentService.update(id, obj));
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Comment> delete(@PathVariable Long id){
        commentService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
