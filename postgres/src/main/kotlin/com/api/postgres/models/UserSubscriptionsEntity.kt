package com.api.postgres.models

import jakarta.persistence.*
import java.io.Serializable

@Entity
@Table(name = "user_subscriptions")
data class UserSubscription(
    @EmbeddedId
    val id: UserSubscriptionId,

    @Column(name = "priority")
    val priority: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")  // Maps the userId in the embedded key
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    val user: UserEntity  // Foreign key relationship to UserEntity
) {
    // Default constructor for JPA
    constructor() : this(
        id = UserSubscriptionId(0, 0), // Default values for the embedded key
        priority = 0,                  // Default priority value
        user = UserEntity()            // Default UserEntity
    )
}

@Embeddable
data class UserSubscriptionId(
    @Column(name = "user_id")
    val userId: Int,

    @Column(name = "provider_id")
    val providerId: Int
) : Serializable {
    // Default constructor for JPA
    constructor() : this(0, 0) // Default values for the embedded ID
}

