package com.api.postgres.model

import jakarta.persistence.*

@Entity
@Table(name = "subscription_providers")
data class SubscriptionProvider(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "provider_id")
    val providerId: Int? = null,

    @Column(name = "provider_name", length = 255, nullable = false)
    val providerName: String
)
