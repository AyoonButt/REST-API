package com.api.postgres.models

import jakarta.persistence.*

@Entity
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
)