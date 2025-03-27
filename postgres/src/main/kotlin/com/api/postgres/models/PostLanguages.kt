package com.api.postgres.models

import jakarta.persistence.*
import org.hibernate.annotations.DynamicUpdate
import java.io.Serializable
import java.sql.Timestamp

@Entity
@DynamicUpdate
@Table(name = "post_languages")
data class PostLanguages(
    @EmbeddedId
    val id: PostLanguageId,

    @ManyToOne
    @MapsId("postId") // Maps the postId part of the composite key
    @JoinColumn(name = "post_id", referencedColumnName = "post_id")
    val post: PostEntity,

    @Column(name = "created_at")
    val createdAt: Timestamp = Timestamp(System.currentTimeMillis())
) {
    // Default constructor for JPA
    constructor() : this(
        id = PostLanguageId(0, ""),
        post = PostEntity(),
        createdAt = Timestamp(System.currentTimeMillis())
    )
}

@Embeddable
data class PostLanguageId(
    @Column(name = "post_id")
    val postId: Int?,

    @Column(name = "language_code", length = 10)
    val languageCode: String
) : Serializable {
    // Default constructor for JPA
    constructor() : this(
        postId = 0, // Provide a default value for postId
        languageCode = "" // Provide a default value for languageCode
    )
}