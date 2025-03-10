package com.api.postgres.models

import jakarta.persistence.*
import org.hibernate.annotations.DynamicUpdate
import java.io.Serializable

@Entity
@DynamicUpdate
@Table(name = "post_languages")
data class PostLanguages(
    @EmbeddedId
    val id: PostLanguageId,

    @ManyToOne
    @MapsId("postId") // Maps the postId part of the composite key
    @JoinColumn(name = "post_id", referencedColumnName = "post_id")
    val post: PostEntity // Assuming PostEntity exists
) {
    // Default constructor for JPA
    constructor() : this(
        id = PostLanguageId(0, ""), // Provide default values for the composite key
        post = PostEntity() // Create a default instance of PostEntity
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