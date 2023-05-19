package com.example.demo.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity(name = "comments")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String content;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "GMT")
    private Instant publishDate;
    private int upvotes;

    @JsonIgnore
    private Set<UUID> usersUpvotesId = new HashSet<>();

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User author;

    public Comment(){}

    public Comment(String content, Instant publishDate, Post post, User author) {
        this.content = content;
        this.publishDate = publishDate;
        this.post = post;
        this.author = author;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(Instant publishDate) {
        this.publishDate = publishDate;
    }

    public int getUpvotes() {
        return upvotes;
    }

    public void setUpvotes(int upvotes) {
        this.upvotes = upvotes;
    }

    public Post getPost() {
        return post;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public Set<UUID> getUsersUpvotesId() {
        return usersUpvotesId;
    }

    public void increaseUpvote(UUID id){
        this.usersUpvotesId.add(id);
        this.upvotes++;
    }
}
