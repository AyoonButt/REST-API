package com.api.postgres.model

import jakarta.persistence.*


@Entity
@Table(name = "post_subscriptions")
data class PostSubscription(
    @EmbeddedId
    val id: PostSubscriptionId
)

@Embeddable
data class PostSubscriptionId(
    @Column(name = "post_id")
    val postId: Int,

    @Column(name = "provider_id")
    val providerId: Int
) : Serializable
