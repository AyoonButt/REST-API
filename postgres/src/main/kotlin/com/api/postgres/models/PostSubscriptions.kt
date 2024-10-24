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
)

@Embeddable
data class PostSubscriptionId(
    @Column(name = "post_id")
    val postId: Int,

    @Column(name = "provider_id")
    val providerId: Int
) : Serializable
