package com.api.postgres.models

import jakarta.persistence.*

@Entity
@Table(name = "more_information")
data class InfoItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "tmdb_id", nullable = false)
    val tmdbId: Int,

    @Column(nullable = false)
    val type: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_info_user")
    )
    val user: UserEntity,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "info_timestamps",
        joinColumns = [JoinColumn(name = "info_id")],
        foreignKey = ForeignKey(name = "fk_info_timestamps")
    )
    @OrderColumn(name = "session_index")
    val sessions: MutableList<ViewingSession> = mutableListOf()
) {
    constructor() : this(0, 0, "", UserEntity(), mutableListOf())
}

@Embeddable
data class ViewingSession(
    @Column(name = "start_timestamp")
    val startTimestamp: String,

    @Column(name = "end_timestamp")
    val endTimestamp: String
) {
    constructor() : this("", "")
}