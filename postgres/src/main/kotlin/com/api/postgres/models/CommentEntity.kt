package com.api.postgres.models

import jakarta.persistence.*
import org.hibernate.annotations.DynamicUpdate

@Entity
@DynamicUpdate
@Table(name = "comments")
data class CommentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    val commentId: Int? = null,

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,

    @ManyToOne
    @JoinColumn(name = "post_id", nullable = false)
    val post: PostEntity,

    @Column(name = "content", nullable = false)
    val content: String,

    @Column(name = "sentiment", length = 50)
    val sentiment: String? = null,

    @Column(name = "timestamp", length = 75)
    val timestamp: String? = null,

    @ManyToOne
    @JoinColumn(name = "parent_comment_id")
    var parentComment: CommentEntity? = null,

    @OneToMany(mappedBy = "parentComment", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val replies: List<CommentEntity> = listOf(),

    @Column(name = "comment_type", nullable = false)
    val commentType: String
) {
    constructor() : this(
        commentId = null,
        user = UserEntity(),
        post = PostEntity(),
        content = "",
        sentiment = null,
        timestamp = null,
        commentType = ""
    )
}