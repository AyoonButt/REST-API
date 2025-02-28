package com.api.postgres.models

import jakarta.persistence.*
import org.hibernate.annotations.DynamicUpdate

@Entity
@DynamicUpdate
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

    @Column(name = "start_timestamp", length = 75, nullable = false)
    var startTimestamp: String,

    @Column(name = "end_timestamp", length = 75, nullable = false)
    var endTimestamp: String,

    @Column(name = "like_state", nullable = false)
    val likeState: Boolean = false,

    @Column(name = "save_state", nullable = false)
    val saveState: Boolean = false,

    @Column(name = "comment_button_pressed", nullable = false)
    val commentButtonPressed: Boolean = false,

) {
    // Default constructor for JPA
    constructor() : this(
        interactionId = null,
        user = UserEntity(),
        post = PostEntity(),
        startTimestamp = "",
        endTimestamp = "",
        likeState = false,
        saveState = false,
        commentButtonPressed = false,
    )
}