package com.api.postgres.models

import jakarta.persistence.*

@Entity
@Table(name = "user_post_interactions")
data class UserPostInteraction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "interaction_id")
    val interactionId: Int? = null,

    @ManyToOne
    @JoinColumn(name = "user_id")
    val user: UserEntity,

    @ManyToOne
    @JoinColumn(name = "post_id")
    val post: PostEntity,

    @Column(name = "time_spent_on_post")
    val timeSpentOnPost: Long,

    @Column(name = "like_state", nullable = false)
    val likeState: Boolean = false,

    @Column(name = "save_state", nullable = false)
    val saveState: Boolean = false,

    @Column(name = "comment_button_pressed", nullable = false)
    val commentButtonPressed: Boolean = false,

    @Column(name = "comment_made", nullable = false)
    val commentMade: Boolean = false,

    @Column(name = "timestamp", length = 75, nullable = false)
    var timestamp: String
) {
    // Default constructor for JPA
    constructor() : this(
        interactionId = null,
        user = UserEntity(),             // Provide a default UserEntity
        post = PostEntity(),             // Provide a default PostEntity
        timeSpentOnPost = 0L,           // Default time spent
        likeState = false,               // Default like state
        saveState = false,               // Default save state
        commentButtonPressed = false,    // Default button state
        commentMade = false,             // Default comment made state
        timestamp = ""                   // Default timestamp
    )
}

