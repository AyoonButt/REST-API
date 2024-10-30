package com.api.postgres.models


import jakarta.persistence.*

@Entity
@Table(name = "comments")
data class CommentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    val commentId: Int? = null,

    @ManyToOne
    @JoinColumn(name = "user_id")
    val user: UserEntity,

    @ManyToOne
    @JoinColumn(name = "post_id")
    val post: PostEntity,

    @Column(name = "content", nullable = false)
    val content: String,

    @Column(name = "sentiment", length = 50)
    val sentiment: String,

    @Column(name = "timestamp", length = 75)
    val timestamp: String
) {
    // Default constructor for JPA
    constructor() : this(
        commentId = null,
        user = UserEntity(), // Assuming a no-argument constructor in UserEntity
        post = PostEntity(), // Assuming a no-argument constructor in PostEntity
        content = "",
        sentiment = "",
        timestamp = ""
    )
}
