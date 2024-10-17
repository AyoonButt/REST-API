package com.api.postgres.model

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
    val user: User,

    @ManyToOne
    @JoinColumn(name = "post_id")
    val post: Post,

    @Column(name = "time_spent_on_trailer")
    val timeSpentOnTrailer: Long,

    @Column(name = "like_state")
    val likeState: Boolean,

    @Column(name = "timestamp", length = 75)
    val timestamp: String
)
