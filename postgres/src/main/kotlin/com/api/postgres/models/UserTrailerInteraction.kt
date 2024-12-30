package com.api.postgres.models

import jakarta.persistence.*
import org.hibernate.annotations.DynamicUpdate

@Entity
@DynamicUpdate
@Table(name = "user_trailer_interactions")
data class UserTrailerInteraction(

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

    @Column(name = "time_spent_on_trailer")
    val timeSpent: Long,

    @Column(name = "replay_count")
    val replayCount: Int? = null,

    @Column(name = "like_state")
    val likeState: Boolean,

    @Column(name = "save_state")
    val saveState: Boolean,

    @Column(name = "is_muted")
    val isMuted: Boolean,

    @Column(name = "comment_button_pressed")
    val commentButtonPressed: Boolean,

    @Column(name = "comment_made")
    val commentMade: Boolean,

    @Column(name = "timestamp", length = 75)
    var timestamp: String
) {
    // Default constructor for JPA
    constructor() : this(
        interactionId = null,          // Default interaction ID is null
        user = UserEntity(),           // Initializes with a default UserEntity
        post = PostEntity(),           // Initializes with a default PostEntity
        timeSpent = 0L,               // Default time spent is 0
        replayCount = 0,               // Default replay count is 0
        likeState = false,             // Default like state is false
        saveState = false,             // Default save state is false
        isMuted = false,               // Default muted state is false
        commentButtonPressed = false,  // Default comment button pressed state is false
        commentMade = false,           // Default comment made state is false
        timestamp = ""                 // Default timestamp is an empty string
    )
}
