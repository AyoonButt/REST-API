package com.api.postgres.model

import jakarta.persistence.*

@Entity
@Table(name = "user_subscriptions")
data class UserSubscription(
    @EmbeddedId
    val id: UserSubscriptionId,

    @Column(name = "priority")
    val priority: Int
)

@Embeddable
data class UserSubscriptionId(
    @Column(name = "user_id")
    val userId: Int,

    @Column(name = "provider_id")
    val providerId: Int
) : Serializable
