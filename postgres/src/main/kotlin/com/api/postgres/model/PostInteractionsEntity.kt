package com.api.postgres.model

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
    val user: User,

    @ManyToOne
    @JoinColumn(name = "post_id")
    val post: Post,

    @Column(name = "time_spent_on_post")
    val timeSpentOnPost: Long,

    @Column(name = "like_state")
    val likeState: Boolean,

    @Column(name = "save_state")
    val saveState: Boolean,

    @Column(name = "comment_button_pressed")
    val commentButtonPressed: Boolean,

    @Column(name = "comment_made")
    val commentMade: Boolean,

    @Column(name = "timestamp", length = 75)
    val timestamp: String
)
