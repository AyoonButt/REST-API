package com.api.postgres.models

import jakarta.persistence.*

@Entity
@Table(name = "subscription_providers")
data class SubscriptionProvider(
    @Id
    @Column(name = "provider_id")
    val providerId: Int? = null,

    @Column(name = "provider_name", length = 255, nullable = false)
    val providerName: String
)