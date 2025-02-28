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

    @Column(name = "start_timestamp", length = 75, nullable = false)
    var startTimestamp: String,

    @Column(name = "end_timestamp", length = 75, nullable = false)
    var endTimestamp: String,

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

) {
    // Default constructor for JPA
    constructor() : this(
        interactionId = null,
        user = UserEntity(),
        post = PostEntity(),
        startTimestamp = "",
        endTimestamp = "",
        replayCount = 0,
        likeState = false,
        saveState = false,
        isMuted = false,
        commentButtonPressed = false
    )
}