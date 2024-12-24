package com.api.postgres.repositories

import com.api.postgres.models.*
import org.springframework.data.domain.Pageable


import org.springframework.data.jpa.repository.JpaRepository  // Import JpaRepository
import org.springframework.data.jpa.repository.Query  // Import for @Query annotation
import org.springframework.data.repository.query.Param  // Import for @Param annotation
import org.springframework.stereotype.Repository  // Import for @Repository annotation
import org.springframework.data.domain.Page


@Repository
interface CommentRepository : JpaRepository<CommentEntity, Int> {
    fun findByPostPostId(postId: Int): List<CommentEntity>

    @Query("SELECT c FROM CommentEntity c WHERE c.parentComment.commentId = :parentCommentId")
    fun findAllReplies(@Param("parentCommentId") parentCommentId: Int, pageable: Pageable): Page<CommentEntity>

    @Query("SELECT c FROM CommentEntity c WHERE c.parentComment.commentId = :parentCommentId AND c.user.userId = :userId")
    fun findReplies(
        @Param("parentCommentId") parentCommentId: Int,
        @Param("userId") userId: Int,
        pageable: Pageable
    ): Page<CommentEntity>
}


@Repository
interface UserTrailerInteractionRepository : JpaRepository<UserTrailerInteraction, Int> {
    fun findByUserUserId(userId: Int): List<UserTrailerInteraction>
    fun findByUserUserIdAndPostPostId(userId: Int, postId: Int): UserTrailerInteraction?
    fun findByUserUserIdAndLikeStateIsTrueOrderByTimestampDesc(userId: Int): List<UserTrailerInteraction>
    fun findByUserUserIdAndSaveStateIsTrueOrderByTimestampDesc(userId: Int): List<UserTrailerInteraction>
    fun findByUserUserIdAndCommentMadeIsTrueOrderByTimestampDesc(userId: Int): List<UserTrailerInteraction>
}


@Repository
interface UserPostInteractionRepository : JpaRepository<UserPostInteraction, Long> {
    @Query("SELECT u FROM UserPostInteraction u WHERE u.user.userId = :userId")
    fun findByUserUserId(@Param("userId") userId: Int): List<UserPostInteraction>

    @Query("SELECT u FROM UserPostInteraction u WHERE u.user.userId = :userId AND u.post.postId = :postId")
    fun findByUserUserIdAndPostPostId(@Param("userId") userId: Int, @Param("postId") postId: Int): UserPostInteraction?

    @Query("SELECT DISTINCT u.post.postId FROM UserPostInteraction u WHERE u.user.userId = :userId AND u.likeState = true ORDER BY u.timestamp DESC")
    fun findLikedPostsByUserUserId(@Param("userId") userId: Int): List<Int>

    @Query("SELECT DISTINCT u.post.postId FROM UserPostInteraction u WHERE u.user.userId = :userId AND u.saveState = true ORDER BY u.timestamp DESC")
    fun findSavedPostsByUserUserId(@Param("userId") userId: Int): List<Int>

    @Query("SELECT DISTINCT u.post.postId FROM UserPostInteraction u WHERE u.user.userId = :userId AND u.commentMade = true ORDER BY u.timestamp DESC")
    fun findCommentMadePostsByUserUserId(@Param("userId") userId: Int): List<Int>
}


@Repository
interface PostRepository : JpaRepository<PostEntity, Int> {
    @Query("SELECT p FROM PostEntity p ORDER BY p.postId")
    fun findAllByOrderByPostId(@Param("limit") limit: Int, @Param("offset") offset: Int): List<PostEntity>

    @Query("SELECT p FROM PostEntity p WHERE p.postId = :postId")
    fun findPostById(@Param("postId") postId: Int): PostEntity?

    @Query("SELECT p.postId FROM PostEntity p WHERE p.tmdbId = :tmdbId")
    fun findPostIdByTmdbId(@Param("tmdbId") tmdbId: Int): Int?
}

@Repository
interface CastRepository : JpaRepository<CastEntity, Int> {
    fun findByPostPostId(postId: Int): List<CastEntity>
}

@Repository
interface CrewRepository : JpaRepository<CrewEntity, Int> {
    fun findByPostPostId(postId: Int): List<CrewEntity>
}


@Repository
interface GenreRepository : JpaRepository<GenreEntity, Int> {
    fun findByGenreNameContainingIgnoreCase(query: String): List<GenreEntity>

    @Query("SELECT g FROM GenreEntity g WHERE g.genreName IN :names")
    fun findAllGenreIdsByNames(@Param("names") names: List<String>): List<GenreEntity>

    @Query("SELECT g FROM GenreEntity g WHERE g.genreId = :genreId")
    fun findByGenreId(@Param("genreId") genreId: Int): GenreEntity?

}


interface ProviderRepository : JpaRepository<SubscriptionProvider, Int> {

    @Query("SELECT p FROM SubscriptionProvider p WHERE p.providerName IN :names")
    fun findAllProviderIdsByNames(@Param("names") names: List<String>): List<SubscriptionProvider>

    fun findByProviderNameContainingIgnoreCase(query: String): List<SubscriptionProvider>

    @Query("SELECT p FROM SubscriptionProvider p WHERE p.providerId = :providerId")
    fun findByProviderId(@Param("providerId") providerId: Int): SubscriptionProvider?
}

@Repository
interface PostGenresRepository : JpaRepository<PostGenres, PostGenreId> {

}

@Repository
interface PostSubscriptionsRepository : JpaRepository<PostSubscriptions, PostSubscriptionId> {

}

@Repository
interface UserRepository : JpaRepository<UserEntity, Int> {

    fun findByUsernameAndPassword(username: String, password: String): UserEntity?

    @Query("SELECT u FROM UserEntity u WHERE u.userId = :userId")
    fun fetchUserParams(@Param("userId") userId: Int): UserEntity?

    fun findByUsername(username: String): UserEntity?

}


@Repository
interface UserGenresRepository : JpaRepository<UserGenres, UserGenreId> {
    @Query("SELECT COALESCE(MAX(ug.priority), 0) FROM UserGenres ug WHERE ug.user.userId = :userId")
    fun findMaxPriorityByUserId(@Param("userId") userId: Int): Int


    // Retrieve all genre IDs for a specific user
    fun findByIdUserId(userId: Int): List<UserGenres>
}

@Repository
interface UserAvoidGenresRepository : JpaRepository<UserAvoidGenres, UserAvoidGenreId> {
    // Retrieve all genre IDs for a specific user
    fun findByIdUserId(userId: Int): List<UserAvoidGenres>
}

@Repository
interface UserSubscriptionRepository : JpaRepository<UserSubscription, UserSubscriptionId> {
    // Retrieve all provider IDs for a specific user
    fun findByIdUserId(userId: Int): List<UserSubscription>

    @Query("SELECT COALESCE(MAX(us.priority), 0) FROM UserSubscription us WHERE us.user.userId = :userId")
    fun findMaxPriorityByUserId(@Param("userId") userId: Int): Int


    // Custom query with ORDER BY for sorting by priority in descending order
    @Query("SELECT us.id.providerId FROM UserSubscription us WHERE us.user.userId = :userId ORDER BY us.priority ASC")
    fun findProviderIdsByUserIdSortedByPriority(@Param("userId") userId: Int): List<Int>

}

