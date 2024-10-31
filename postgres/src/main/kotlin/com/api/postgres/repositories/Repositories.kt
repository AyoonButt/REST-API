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
}

@Repository
interface CastRepository : JpaRepository<CastEntity, Int> {
    fun findByPostId(postId: Int): List<CastEntity>
}

@Repository
interface CrewRepository : JpaRepository<CrewEntity, Int> {
    fun findByPostId(postId: Int): List<CrewEntity>
}


@Repository
interface GenreRepository : JpaRepository<GenreEntity, Int> {
    fun findByGenreNameContainingIgnoreCase(query: String): List<GenreEntity>
}

interface ProviderRepository : JpaRepository<SubscriptionProvider, Int> {
    @Query("SELECT p FROM SubscriptionProvider p ORDER BY p.providerId")
    fun findAllWithLimitAndOffset(@Param("limit") limit: Int, @Param("offset") offset: Int): List<SubscriptionProvider>
}


@Repository
interface UserRepository : JpaRepository<UserEntity, Int> {

    fun findByUsernameAndPassword(username: String, password: String): UserEntity?

    @Query("SELECT u FROM UserEntity u WHERE u.userId = :userId")
    fun fetchUserParams(@Param("userId") userId: Int): UserEntity?
}


@Repository
interface UserGenresRepository : JpaRepository<UserGenres, UserGenreId> {
    // Retrieve all genre IDs for a specific user
    fun findByIdUserId(userId: Int): List<UserGenres>
}

@Repository
interface UserAvoidGenresRepository : JpaRepository<UserAvoidGenres, UserAvoidGenreId> {
    // Retrieve all genre IDs for a specific user
    fun findByIdUserId(userId: Int): List<UserGenres>
}

@Repository
interface UserSubscriptionRepository : JpaRepository<UserSubscription, UserSubscriptionId> {
    // Retrieve all provider IDs for a specific user
    fun findByIdUserId(userId: Int): List<UserSubscription>
}

