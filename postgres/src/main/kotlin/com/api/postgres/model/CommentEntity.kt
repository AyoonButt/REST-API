package com.api.postgres.model

import jakarta.persistence.*

import jakarta.persistence.*

@Entity
@Table(name = "comments")
data class Comment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    val commentId: Int? = null,

    @ManyToOne
    @JoinColumn(name = "user_id")
    val user: User,

    @ManyToOne
    @JoinColumn(name = "post_id")
    val post: Post,

    @Column(name = "content", nullable = false)
    val content: String,

    @Column(name = "sentiment", length = 50)
    val sentiment: String,

    @Column(name = "timestamp", length = 75)
    val timestamp: String
)
