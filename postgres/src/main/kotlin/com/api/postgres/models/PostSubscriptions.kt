package com.api.postgres.models

import jakarta.persistence.*
import java.io.Serializable

@Entity
@Table(name = "post_subscriptions")
data class PostSubscriptions(
    @EmbeddedId
    val id: PostSubscriptionId,

    @ManyToOne
    @MapsId("postId") // Maps the postId part of the composite key
    @JoinColumn(name = "post_id", referencedColumnName = "post_id")
    val post: PostEntity, // Assuming PostEntity exists

    @ManyToOne
    @MapsId("providerId") // Maps the providerId part of the composite key
    @JoinColumn(name = "provider_id", referencedColumnName = "provider_id")
    val provider: SubscriptionProvider // Assuming SubscriptionProvider exists
) {
    // Default constructor for JPA
    constructor() : this(
        id = PostSubscriptionId(0, 0), // Provide default values for the composite key
        post = PostEntity(), // Create a default instance of PostEntity (ensure it has a no-arg constructor)
        provider = SubscriptionProvider() // Create a default instance of SubscriptionProvider (ensure it has a no-arg constructor)
    )
}

@Embeddable
data class PostSubscriptionId(
    @Column(name = "post_id")
    val postId: Int,

    @Column(name = "provider_id")
    val providerId: Int
) : Serializable {
    // Default constructor for JPA
    constructor() : this(
        postId = 0, // Provide a default value for postId
        providerId = 0 // Provide a default value for providerId
    )
}
