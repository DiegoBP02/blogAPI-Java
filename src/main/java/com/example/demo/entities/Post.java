package com.example.demo.entities;

import com.example.demo.entities.enums.PostCategory;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
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

    public Post(String title, String content, Instant publishDate, Set<PostCategory> categories, User author) {
        this.title = title;
        this.content = content;
        this.publishDate = publishDate;
        this.categories = categories;
        this.author = author;
    }

    public void increaseUpvote(UUID id) {
        this.usersUpvotesId.add(id);
        this.upvotes++;
    }

}
