package com.example.demo.entities;

import com.example.demo.entities.enums.PostCategory;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String title;
    private String content;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "GMT")
    private Instant publishDate;
    private int upvotes;
    private Set<PostCategory> categories;

    @JsonIgnore
    private Set<UUID> usersUpvotesId = new HashSet<>();

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User author;

    @JsonIgnore
    @OneToMany(mappedBy = "post")
    private Set<Comment> comments = new HashSet<>();

    public Post() {
    }

    public Post(String title, String content, Instant publishDate, Set<PostCategory> categories, User author) {
        this.title = title;
        this.content = content;
        this.publishDate = publishDate;
        this.categories = categories;
        this.author = author;
    }

    public Set<Comment> getComments() {
        return comments;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Set<UUID> getUsersUpvotesId() {
        return usersUpvotesId;
    }

    public void increaseUpvote(UUID id) {
        this.usersUpvotesId.add(id);
        this.upvotes++;
    }

    public Set<PostCategory> getCategories() {
        return categories;
    }

    public void setCategories(Set<PostCategory> categories) {
        this.categories = categories;
    }
}
