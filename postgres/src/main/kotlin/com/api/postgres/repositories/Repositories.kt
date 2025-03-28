package com.api.postgres.repositories


import com.api.postgres.CastProjection
import com.api.postgres.CommentProjection
import com.api.postgres.CrewProjection
import com.api.postgres.InfoItemProjection
import com.api.postgres.InteractionStatesProjection
import com.api.postgres.PostProjection
import com.api.postgres.ReplyCountProjection
import com.api.postgres.TimestampProjection
import com.api.postgres.TrailerInteractionProjection
import com.api.postgres.UserGenreProjection
import com.api.postgres.UserPostInteractionProjection
import com.api.postgres.UserPreferencesProjection
import com.api.postgres.UserProjection
import com.api.postgres.UserSubscriptionProjection
import com.api.postgres.models.*
import jakarta.transaction.Transactional


import org.springframework.data.jpa.repository.JpaRepository  // Import JpaRepository
import org.springframework.data.jpa.repository.Query  // Import for @Query annotation
import org.springframework.data.repository.query.Param  // Import for @Param annotation
import org.springframework.stereotype.Repository  // Import for @Repository annotation
import org.springframework.data.jpa.repository.Modifying

interface InfoRepository : JpaRepository<InfoItem, Long> {
    fun findByTmdbIdAndTypeAndUserUserId(tmdbId: Int, type: String, userId: Int): InfoItemProjection?

    @Modifying
    @Transactional
    @Query("""
        DELETE FROM info_timestamps 
        WHERE info_id IN (
            SELECT i.id FROM more_information i 
            WHERE i.tmdb_id = :tmdbId 
            AND i.type = :type 
            AND i.user_id = :userId
        )
    """, nativeQuery = true)
    fun deleteExistingSessions(
        @Param("tmdbId") tmdbId: Int,
        @Param("type") type: String,
        @Param("userId") userId: Int
    )

    @Modifying
    @Transactional
    @Query("""
        INSERT INTO info_timestamps (info_id, session_index, start_timestamp, end_timestamp)
        SELECT i.id, :session_index, :startTimestamp, :endTimestamp
        FROM more_information i
        WHERE i.tmdb_id = :tmdbId 
        AND i.type = :type 
        AND i.user_id = :userId
    """, nativeQuery = true)
    fun insertSession(
        @Param("tmdbId") tmdbId: Int,
        @Param("type") type: String,
        @Param("userId") userId: Int,
        @Param("session_index") sessionIndex: Int,
        @Param("startTimestamp") startTimestamp: String,
        @Param("endTimestamp") endTimestamp: String
    )

    @Modifying
    @Transactional
    @Query("""
        INSERT INTO more_information (tmdb_id, type, user_id)
        VALUES (:tmdbId, :type, :userId)
    """, nativeQuery = true)
    fun insertNewInfo(
        @Param("tmdbId") tmdbId: Int,
        @Param("type") type: String,
        @Param("userId") userId: Int
    )
}

@Repository
interface CommentRepository : JpaRepository<CommentEntity, Int> {


    @Query(
        value = """
        SELECT 
            c.comment_id as commentId,
            c.user_id as userId,
            u.username as username,
            c.post_id as postId,
            c.content as content,
            c.sentiment as sentiment,
            c.timestamp as timestamp,
            c.parent_comment_id as parentCommentId,
            c.comment_type as commentType
        FROM comments c
        JOIN users u ON c.user_id = u.user_id
        WHERE c.post_id = :postId
        ORDER BY c.timestamp DESC
        LIMIT :limit OFFSET :offset
        """,
        nativeQuery = true
    )
    fun findCommentsByPostId(
        @Param("postId") postId: Int,
        @Param("limit") limit: Int,
        @Param("offset") offset: Int
    ): List<CommentProjection>

    @Modifying
    @Transactional
    @Query(
        value = """
    INSERT INTO comments (user_id, post_id, content, sentiment, timestamp, parent_comment_id, comment_type)
    VALUES (:userId, :postId, :content, :sentiment, :timestamp, :parentCommentId, :comment_type)
    """,
        nativeQuery = true
    )
    fun insertComment(
        @Param("userId") userId: Int,
        @Param("postId") postId: Int,
        @Param("content") content: String,
        @Param("sentiment") sentiment: String?,
        @Param("timestamp") timestamp: String?,
        @Param("parentCommentId") parentCommentId: Int?,
        @Param("comment_type") commentType: String
    ): Int

    // Add separate query for getting the last inserted ID
    @Query(
        value = "SELECT lastval()",
        nativeQuery = true
    )
    fun getLastInsertedId(): Int

    @Query(
        value = """
    SELECT 
        c.comment_id as commentId,
        c.user_id as userId,
        u.username as username,
        c.post_id as postId,
        c.content as content,
        c.sentiment as sentiment,
        c.timestamp as timestamp,
        c.parent_comment_id as parentCommentId,
        c.comment_type ad commentType
    FROM comments c
    JOIN users u ON c.user_id = u.user_id
    WHERE c.parent_comment_id = :parentId
    AND c.user_id = :userId
    ORDER BY c.timestamp DESC
    LIMIT :limit OFFSET :offset
    """,
        nativeQuery = true
    )
    fun findRepliesByParentIdAndUserId(
        @Param("parentId") parentId: Int,
        @Param("userId") userId: Int,
        @Param("limit") limit: Int,
        @Param("offset") offset: Int
    ): List<CommentProjection>

    @Query("""
    WITH RECURSIVE reply_hierarchy AS (
        -- Base case: direct replies to the parent comment
        SELECT 
            c.comment_id,
            c.user_id,
            u.username,
            c.post_id,
            c.content,
            c.sentiment,
            c.timestamp,
            c.parent_comment_id,
            c.comment_type,
            1 as depth
        FROM comments c
        JOIN users u ON c.user_id = u.user_id
        WHERE c.parent_comment_id = :parentId
        
        UNION ALL
        
        -- Recursive case: replies to replies
        SELECT 
            c.comment_id,
            c.user_id,
            u.username,
            c.post_id,
            c.content,
            c.sentiment,
            c.timestamp,
            c.parent_comment_id,
            c.comment_type,
            CASE 
                WHEN rh.depth >= 10 THEN rh.depth
                ELSE rh.depth + 1
            END as depth
        FROM comments c
        JOIN users u ON c.user_id = u.user_id
        JOIN reply_hierarchy rh ON c.parent_comment_id = rh.comment_id
        WHERE rh.depth < 10
    )
    SELECT 
        comment_id as commentId,
        user_id as userId,
        username,
        post_id as postId,
        content,
        sentiment,
        timestamp,
        parent_comment_id as parentCommentId,
        comment_type as commentType,
        depth
    FROM reply_hierarchy 
    ORDER BY timestamp DESC 
    LIMIT :limit OFFSET :offset
""", nativeQuery = true)
    fun findRepliesByParentId(
        @Param("parentId") parentId: Int,
        @Param("limit") limit: Int,
        @Param("offset") offset: Int
    ): List<CommentProjection>

    @Query("""
    WITH RECURSIVE comment_hierarchy AS (
        -- Base case: start with the given comment
        SELECT comment_id, parent_comment_id, 1 as level
        FROM comments
        WHERE comment_id = :commentId
        
        UNION ALL
        
        -- Recursive case: join with parent comments
        SELECT c.comment_id, c.parent_comment_id, ch.level + 1
        FROM comments c
        INNER JOIN comment_hierarchy ch ON c.comment_id = ch.parent_comment_id
    )
    SELECT comment_id
    FROM comment_hierarchy
    WHERE parent_comment_id IS NULL
    LIMIT 1
    """, nativeQuery = true)
    fun findRootParentId(commentId: Int): Int?


    @Query("""
   SELECT u.username
   FROM comments c
   JOIN users u ON u.user_id = 
       (SELECT user_id 
        FROM comments 
        WHERE comment_id = c.parent_comment_id)
   WHERE c.parent_comment_id = :parentCommentId
""", nativeQuery = true)
    fun findParentCommentUsername(@Param("parentCommentId") parentCommentId: Int): String?


    @Query(
        value = """
    WITH RECURSIVE reply_tree AS (
    -- Base case: direct replies
    SELECT 
        c.comment_id,
        c.parent_comment_id,
        1 as level
    FROM comments c
    WHERE c.parent_comment_id IN (:parentIds)
    
    UNION ALL
    
    -- Recursive case: replies to replies
    SELECT 
        c.comment_id,
        rt.parent_comment_id,
        rt.level + 1
    FROM comments c
    INNER JOIN reply_tree rt ON c.parent_comment_id = rt.comment_id
    )
    SELECT 
        parent_comment_id as parentId,
        COUNT(*) as replyCount
    FROM reply_tree
    GROUP BY parent_comment_id;
    """,
        nativeQuery = true
    )
    fun getReplyCountsForComments(@Param("parentIds") parentIds: List<Int>): List<ReplyCountProjection>

    @Query("""
    SELECT 
        c.comment_id as commentId,
        c.user_id as userId,
        u.username as username,
        c.post_id as postId,
        c.content as content,
        c.sentiment as sentiment,
        c.timestamp as timestamp,
        c.parent_comment_id as parentCommentId,
        c.comment_type as commentType
    FROM comments c
    JOIN users u ON c.user_id = u.user_id
    WHERE c.user_id = :userId 
    AND c.comment_type = :commentType
    ORDER BY c.timestamp DESC
    LIMIT :limit
    OFFSET :offset
""", nativeQuery = true)
    fun findCommentsByUserIdAndType(
        @Param("userId") userId: Int,
        @Param("commentType") commentType: String,
        @Param("limit") limit: Int,
        @Param("offset") offset: Int
    ): List<CommentProjection>


    @Query("""
WITH RECURSIVE comment_hierarchy AS (
    -- Base case: start with the parent comment
    SELECT c.comment_id, c.user_id, u.username, c.post_id, c.content, 
           c.sentiment, c.timestamp, c.parent_comment_id, c.comment_type
    FROM comments c
    JOIN users u ON c.user_id = u.user_id
    WHERE c.comment_id = :parentCommentId
    
    UNION ALL
    
    -- Recursive case: keep joining with parents until we find one with no parent
    SELECT c.comment_id, c.user_id, u.username, c.post_id, c.content, 
           c.sentiment, c.timestamp, c.parent_comment_id, c.comment_type
    FROM comments c
    JOIN users u ON c.user_id = u.user_id
    INNER JOIN comment_hierarchy ch ON c.comment_id = ch.parent_comment_id
)
SELECT comment_id as commentId, user_id as userId, username, post_id as postId,
       content, sentiment, timestamp, parent_comment_id as parentCommentId, 
       comment_type as commentType
FROM comment_hierarchy
WHERE parent_comment_id IS NULL
LIMIT 1
""", nativeQuery = true)
    fun findRootCommentWithData(parentCommentId: Int): CommentProjection?

}

@Repository
interface UserTrailerInteractionRepository : JpaRepository<UserTrailerInteraction, Int> {
    @Query("""
        SELECT t.interactionId as interactionId, 
               t.user.userId as userId, 
               t.post.postId as postId, 
               t.startTimestamp as startTimestamp,
               t.endTimestamp as endTimestamp,
               t.replayCount as replayCount, 
               t.isMuted as isMuted, 
               t.likeState as likeState, 
               t.saveState as saveState,
               t.commentButtonPressed as commentButtonPressed
        FROM UserTrailerInteraction t
        WHERE t.user.userId = :userId
    """)
    fun findDtosByUserId(@Param("userId") userId: Int): List<TrailerInteractionProjection>

    @Query("""
        SELECT t.interactionId as interactionId, 
               t.user.userId as userId, 
               t.post.postId as postId, 
               t.startTimestamp as startTimestamp,
               t.endTimestamp as endTimestamp,
               t.replayCount as replayCount, 
               t.isMuted as isMuted, 
               t.likeState as likeState, 
               t.saveState as saveState,
               t.commentButtonPressed as commentButtonPressed
        FROM UserTrailerInteraction t
        WHERE t.user.userId = :userId AND t.post.postId = :postId
    """)
    fun findDtoByUserAndPost(
        @Param("userId") userId: Int,
        @Param("postId") postId: Int
    ): TrailerInteractionProjection?

    @Modifying
    @Transactional
    @Query("""
   INSERT INTO user_trailer_interactions (
       user_id, post_id, start_timestamp, end_timestamp, replay_count, is_muted,
       like_state, save_state, comment_button_pressed
   ) VALUES (
       :userId, :postId, :startTimestamp, :endTimestamp, :replayCount, :isMuted,
       :likeState, :saveState, :commentButtonPressed
   )
""", nativeQuery = true)
    fun insertInteraction(
        @Param("userId") userId: Int,
        @Param("postId") postId: Int,
        @Param("startTimestamp") startTimestamp: String,
        @Param("endTimestamp") endTimestamp: String,
        @Param("replayCount") replayCount: Int,
        @Param("isMuted") isMuted: Boolean,
        @Param("likeState") likeState: Boolean,
        @Param("saveState") saveState: Boolean,
        @Param("commentButtonPressed") commentButtonPressed: Boolean
    ): Int

    @Query("""
    SELECT DISTINCT p.postId as postId, t.startTimestamp as startTimestamp
    FROM UserTrailerInteraction t 
    JOIN t.post p 
    WHERE t.user.userId = :userId 
    AND t.likeState = true
    AND t.startTimestamp = (
        SELECT MAX(t2.startTimestamp)
        FROM UserTrailerInteraction t2
        WHERE t2.post.postId = p.postId 
        AND t2.user.userId = t.user.userId
    )
    ORDER BY t.startTimestamp DESC
""")
    fun findLikedPostIds(@Param("userId") userId: Int): List<TimestampProjection>

    @Query("""
    SELECT DISTINCT p.postId as postId, t.startTimestamp as startTimestamp
    FROM UserTrailerInteraction t 
    JOIN t.post p 
    WHERE t.user.userId = :userId 
    AND t.saveState = true
    AND t.startTimestamp = (
        SELECT MAX(t2.startTimestamp)
        FROM UserTrailerInteraction t2
        WHERE t2.post.postId = p.postId 
        AND t2.user.userId = t.user.userId
    )
    ORDER BY t.startTimestamp DESC
""")
    fun findSavedPostIds(@Param("userId") userId: Int): List<TimestampProjection>


    @Query("""
    SELECT 
        COALESCE(like_state, false) as likeState,
        COALESCE(save_state, false) as saveState
    FROM user_trailer_interactions
    WHERE user_id = :userId 
    AND post_id = :postId
    AND start_timestamp = (
        SELECT MAX(start_timestamp)
        FROM user_trailer_interactions
        WHERE user_id = :userId 
        AND post_id = :postId
    )
""", nativeQuery = true)
    fun getTrailerInteractionStates(
        @Param("userId") userId: Int,
        @Param("postId") postId: Int
    ): InteractionStatesProjection?

    @Query("""
    SELECT uti.post_id FROM user_trailer_interactions uti
    WHERE uti.user_id = :userId
    ORDER BY uti.start_timestamp DESC
    LIMIT :limit
""", nativeQuery = true)
    fun findMostRecentPostIdsByUserId(
        @Param("userId") userId: Int,
        @Param("limit") limit: Int
    ): List<Int>


    /**
     * Check if a list of trailers have any interaction records for a user
     */
    @Query("""
    SELECT uti.post_id FROM user_trailer_interactions uti
    WHERE uti.user_id = :userId
    AND uti.post_id IN (:postIds)
""", nativeQuery = true)
    fun findInteractedPostIds(
        @Param("userId") userId: Int,
        @Param("postIds") postIds: List<Int>
    ): List<Int>
}



@Repository
interface UserPostInteractionRepository : JpaRepository<UserPostInteraction, Long> {

    @Modifying
    @Transactional
    @Query("""
    INSERT INTO user_post_interactions 
    (user_id, post_id, start_timestamp, end_timestamp, like_state, save_state, 
     comment_button_pressed)
    VALUES (:userId, :postId, :startTimestamp, :endTimestamp, :likeState, :saveState, 
            :commentButtonPressed)
""", nativeQuery = true)
    fun insertInteraction(
        @Param("userId") userId: Int,
        @Param("postId") postId: Int,
        @Param("startTimestamp") startTimestamp: String,
        @Param("endTimestamp") endTimestamp: String,
        @Param("likeState") likeState: Boolean,
        @Param("saveState") saveState: Boolean,
        @Param("commentButtonPressed") commentButtonPressed: Boolean,
    )

    @Modifying
    @Transactional
    @Query("""
        UPDATE user_post_interactions 
        SET start_timestamp = :startTimestamp,
            end_timestamp = :endTimestamp
        WHERE user_id = :userId AND post_id = :postId
    """, nativeQuery = true)
    fun updateTimestamps(
        @Param("userId") userId: Int,
        @Param("postId") postId: Int,
        @Param("startTimestamp") startTimestamp: String,
        @Param("endTimestamp") endTimestamp: String
    )

    @Query(
        value = """
        SELECT 
            i.interaction_id as interactionId,
            i.user_id as userId,
            i.post_id as postId,
            i.start_timestamp as startTimestamp,
            i.end_timestamp as endTimestamp,
            i.like_state as likeState,
            i.save_state as saveState,
            i.comment_button_pressed as commentButtonPressed
        FROM user_post_interactions i
        WHERE i.user_id = :userId
        ORDER BY i.start_timestamp DESC
        """,
        nativeQuery = true
    )
    fun findInteractionsByUserId(@Param("userId") userId: Int): List<UserPostInteractionProjection>

    @Query(
        value = """
        SELECT 
            i.interaction_id as interactionId,
            i.user_id as userId,
            i.post_id as postId,
            i.start_timestamp as startTimestamp,
            i.end_timestamp as endTimestamp,
            i.like_state as likeState,
            i.save_state as saveState,
            i.comment_button_pressed as commentButtonPressed
        FROM user_post_interactions i
        WHERE i.user_id = :userId 
        AND i.post_id = :postId
        """,
        nativeQuery = true
    )
    fun findInteractionByUserAndPost(
        @Param("userId") userId: Int,
        @Param("postId") postId: Int
    ): UserPostInteractionProjection

    @Query("""
    SELECT DISTINCT u.post.postId as postId, u.startTimestamp as startTimestamp
    FROM UserPostInteraction u
    WHERE u.user.userId = :userId 
    AND u.likeState = true
    AND u.startTimestamp = (
        SELECT MAX(u2.startTimestamp)
        FROM UserPostInteraction u2
        WHERE u2.post.postId = u.post.postId 
        AND u2.user.userId = u.user.userId
    )
    ORDER BY u.startTimestamp DESC
""")
    fun findLikedPostsByUserUserId(@Param("userId") userId: Int): List<TimestampProjection>

    @Query("""
    SELECT DISTINCT u.post.postId as postId, u.startTimestamp as startTimestamp
    FROM UserPostInteraction u
    WHERE u.user.userId = :userId 
    AND u.saveState = true
    AND u.startTimestamp = (
        SELECT MAX(u2.startTimestamp)
        FROM UserPostInteraction u2
        WHERE u2.post.postId = u.post.postId 
        AND u2.user.userId = u.user.userId
    )
    ORDER BY u.startTimestamp DESC
""")
    fun findSavedPostsByUserUserId(@Param("userId") userId: Int): List<TimestampProjection>

    @Query("""
    SELECT 
        COALESCE(like_state, false) as likeState,
        COALESCE(save_state, false) as saveState
    FROM user_post_interactions
    WHERE user_id = :userId 
    AND post_id = :postId
    ORDER BY start_timestamp DESC
    LIMIT 1
""", nativeQuery = true)
    fun getPostInteractionStates(
        @Param("userId") userId: Int,
        @Param("postId") postId: Int
    ): InteractionStatesProjection?

    @Query("""
    SELECT upi.post_id FROM user_post_interactions upi
    WHERE upi.user_id = :userId
    ORDER BY upi.start_timestamp DESC
    LIMIT :limit
""", nativeQuery = true)
    fun findMostRecentPostIdsByUserId(
        @Param("userId") userId: Int,
        @Param("limit") limit: Int
    ): List<Int>

    /**
     * Check if a list of posts have any interaction records for a user
     */
    @Query("""
    SELECT upi.post_id FROM user_post_interactions upi
    WHERE upi.user_id = :userId
    AND upi.post_id IN (:postIds)
""", nativeQuery = true)
    fun findInteractedPostIds(
        @Param("userId") userId: Int,
        @Param("postIds") postIds: List<Int>
    ): List<Int>

}

@Repository
interface PostRepository : JpaRepository<PostEntity, Int> {

    @Query(
        value = """
        SELECT 
            p.post_id as postId,
            p.tmdb_id as tmdbId,
            p.type,
            p.title,
            p.subscription,
            p.release_date as releaseDate,
            p.overview,
            p.poster_path as posterPath,
            p.vote_average as voteAverage,
            p.vote_count as voteCount,
            p.original_language as originalLanguage,
            p.original_title as originalTitle,
            p.popularity,
            p.genre_ids as genreIds,
            p.post_like_count as postLikeCount,
            p.trailer_like_count as trailerLikeCount,
            p.video_key as videoKey
        FROM posts p
        ORDER BY p.post_id
        LIMIT :limit OFFSET :offset
        """,
        nativeQuery = true
    )
    fun findAllDtosByOrderByPostId(
        @Param("limit") limit: Int,
        @Param("offset") offset: Int
    ): List<PostProjection>

    @Query(
        value = """
        SELECT 
            p.post_id as postId,
            p.tmdb_id as tmdbId,
            p.type,
            p.title,
            p.subscription,
            p.release_date as releaseDate,
            p.overview,
            p.poster_path as posterPath,
            p.vote_average as voteAverage,
            p.vote_count as voteCount,
            p.original_language as originalLanguage,
            p.original_title as originalTitle,
            p.popularity,
            p.genre_ids as genreIds,
            p.post_like_count as postLikeCount,
            p.trailer_like_count as trailerLikeCount,
            p.video_key as videoKey
        FROM posts p
        WHERE p.post_id = :postId
        """,
        nativeQuery = true
    )
    fun findDtoById(@Param("postId") postId: Int): PostProjection

    @Query(
        value = "SELECT post_id FROM posts WHERE tmdb_id = :tmdbId",
        nativeQuery = true
    )
    fun findPostIdByTmdbId(@Param("tmdbId") tmdbId: Int): Int?

    @Modifying
    @Transactional
    @Query(
        value = "UPDATE posts SET post_like_count = post_like_count + 1 WHERE post_id = :postId",
        nativeQuery = true
    )
    fun incrementPostLikeCount(@Param("postId") postId: Int)

    @Modifying
    @Transactional
    @Query(
        value = "UPDATE posts SET trailer_like_count = trailer_like_count + 1 WHERE post_id = :postId",
        nativeQuery = true
    )
    fun incrementTrailerLikeCount(@Param("postId") postId: Int)

    @Query(
        value = """
    SELECT 
        p.post_id as postId,
        p.tmdb_id as tmdbId,
        p.type,
        p.title,
        p.subscription,
        p.release_date as releaseDate,
        p.overview,
        p.poster_path as posterPath,
        p.vote_average as voteAverage,
        p.vote_count as voteCount,
        p.original_language as originalLanguage,
        p.original_title as originalTitle,
        p.popularity,
        p.genre_ids as genreIds,
        p.post_like_count as postLikeCount,
        p.trailer_like_count as trailerLikeCount,
        p.video_key as videoKey    
    FROM posts p 
    WHERE p.post_id IN (:postIds)
    ORDER BY POSITION(',' || p.post_id || ',' in ',' || array_to_string(:interactionIds, ',') || ',')
    LIMIT :limit OFFSET :offset
    """,
        nativeQuery = true
    )
    fun findPostDtosByIdsWithPaging(
        @Param("interactionIds") interactionIds: List<Int>
    ): List<PostProjection>

}

@Repository
interface PostGenresRepository : JpaRepository<PostGenres, PostGenreId> {
    @Modifying
    @Transactional
    @Query("""
        INSERT INTO post_genres (post_id, genre_id)
        VALUES (:postId, :genreId)
    """, nativeQuery = true)
    fun insertPostGenre(
        @Param("postId") postId: Int,
        @Param("genreId") genreId: Int
    )

    @Query("""
        SELECT pg.post_id FROM post_genres pg
        WHERE pg.post_id IN (:postIds)
        AND pg.genre_id IN (:genreIds)
    """, nativeQuery = true)
    fun findPostIdsWithGenres(
        @Param("postIds") postIds: List<Int>,
        @Param("genreIds") genreIds: List<Int>
    ): List<Int>
}

@Repository
interface PostSubscriptionsRepository : JpaRepository<PostSubscriptions, PostSubscriptionId> {
    @Modifying
    @Transactional
    @Query("""
        INSERT INTO post_subscriptions (post_id, provider_id)
        VALUES (:postId, :providerId)
    """, nativeQuery = true)
    fun insertPostSubscription(
        @Param("postId") postId: Int,
        @Param("providerId") providerId: Int
    )

    @Query("""
        SELECT ps.post_id FROM post_subscriptions ps
        WHERE ps.provider_id IN (:subscriptions)
    """, nativeQuery = true)
    fun findPostIdsBySubscriptions(
        @Param("subscriptions") subscriptions: List<Int>
    ): List<Int>
}

@Repository
interface PostLanguagesRepository : JpaRepository<PostLanguages, PostLanguageId> {
    @Modifying
    @Transactional
    @Query("""
        INSERT INTO post_languages (post_id, language_code, created_at)
        VALUES (:postId, :languageCode, CURRENT_TIMESTAMP)
    """, nativeQuery = true)
    fun insertPostLanguage(
        @Param("postId") postId: Int,
        @Param("languageCode") languageCode: String
    )

    @Query("""
    SELECT pl.post_id FROM post_languages pl 
    WHERE pl.language_code = :languageCode
    ORDER BY pl.created_at DESC
    """, nativeQuery = true)
    fun findPostIdsByLanguage(
        @Param("languageCode") languageCode: String
    ): List<Int>

    @Query("SELECT COUNT(*) FROM post_languages WHERE language_code = :language", nativeQuery = true)
    fun countByLanguage(@Param("language") language: String): Int

    @Query("SELECT post_id FROM post_languages WHERE created_at > to_timestamp(:timestamp / 1000.0) ORDER BY created_at DESC", nativeQuery = true)
    fun findPostIdsInsertedAfter(@Param("timestamp") timestamp: Long): List<Int>
}

@Repository
interface CastRepository : JpaRepository<CastEntity, Int> {
    @Query(
        value = """
        SELECT 
            c.person_id as personId,
            c.name,
            c.gender,
            c.known_for_department as knownForDepartment,
            c.character,
            c.episode_count as episodeCount,
            c.order_index as orderIndex,
            c.popularity,
            c.profile_path as profilePath
        FROM cast_members c
        WHERE c.post_id = :postId
        ORDER BY c.order_index ASC
        """,
        nativeQuery = true
    )
    fun findDtosByPostId(@Param("postId") postId: Int): List<CastProjection>


    @Modifying
    @Transactional
    @Query("""
        INSERT INTO cast_members (
            tmdb_id, person_id, name, gender, known_for_department,
            character, episode_count, order_index, popularity, profile_path
        ) VALUES (
            :tmdbId, :personId, :name, :gender, :knownForDepartment,
            :character, :episodeCount, :orderIndex, :popularity, :profilePath
        )
    """, nativeQuery = true)
    fun insertCast(
        @Param("tmdbId") tmdbId: Int,
        @Param("personId") personId: Int,
        @Param("name") name: String,
        @Param("gender") gender: Int,
        @Param("knownForDepartment") knownForDepartment: String,
        @Param("character") character: String,
        @Param("episodeCount") episodeCount: Int,
        @Param("orderIndex") orderIndex: Int,
        @Param("popularity") popularity: Double,
        @Param("profilePath") profilePath: String
    )
}

@Repository
interface CrewRepository : JpaRepository<CrewEntity, Int> {
    @Query(
        value = """
        SELECT 
            c.person_id as personId,
            c.name,
            c.gender,
            c.known_for_department as knownForDepartment,
            c.job,
            c.department,
            c.episode_count as episodeCount,
            c.popularity,
            c.profile_path as profilePath
        FROM crew c
        WHERE c.post_id = :postId
        ORDER BY c.popularity DESC
        """,
        nativeQuery = true
    )
    fun findDtosByPostId(@Param("postId") postId: Int): List<CrewProjection>

    @Modifying
    @Transactional
    @Query("""
        INSERT INTO crew (
            tmdb_id, person_id, name, gender, known_for_department,
            job, department, episode_count, popularity, profile_path
        ) VALUES (
            :tmdbId, :personId, :name, :gender, :knownForDepartment,
            :job, :department, :episodeCount, :popularity, :profilePath
        )
    """, nativeQuery = true)
    fun insertCrew(
        @Param("tmdbId") tmdbId: Int,
        @Param("personId") personId: Int,
        @Param("name") name: String,
        @Param("gender") gender: Int,
        @Param("knownForDepartment") knownForDepartment: String,
        @Param("job") job: String,
        @Param("department") department: String,
        @Param("episodeCount") episodeCount: Int,
        @Param("popularity") popularity: Double,
        @Param("profilePath") profilePath: String
    )
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

    @Query("SELECT p.providerName FROM SubscriptionProvider p WHERE p.providerId = :providerId")
    fun findProviderNameById(@Param("providerId") providerId: Int): String?
}

@Repository
interface UserRepository : JpaRepository<UserEntity, Int> {
    @Query("SELECT u FROM UserEntity u WHERE u.username = :username")
    fun findByUsername(@Param("username") username: String): UserEntity?

    @Query("""
        SELECT u.userId as userId, 
               u.name as name, 
               u.username as username, 
               u.email as email, 
               u.language as language,
               u.region as region, 
               u.minMovie as minMovie, 
               u.maxMovie as maxMovie,
               u.minTV as minTV, 
               u.maxTV as maxTV,
               u.oldestDate as oldestDate, 
               u.recentDate as recentDate,
               u.createdAt as createdAt, 
               u.recentLogin as recentLogin
        FROM UserEntity u
        WHERE u.username = :username AND u.password = :password
    """)
    fun findDtoByUsernameAndPassword(
        @Param("username") username: String,
        @Param("password") password: String
    ): UserProjection?

    @Query("""
        SELECT u FROM UserEntity u 
        WHERE u.username = :username AND u.password = :password
    """)
    fun findByUsernameAndPassword(
        @Param("username") username: String,
        @Param("password") password: String
    ): UserEntity?


    @Query("""
    SELECT 
        u.user_id AS userId,
        u.language AS language,
        u.region AS region,
        u.min_movie AS minMovie,
        u.max_movie AS maxMovie,
        u.min_tv AS minTv,
        u.max_tv AS maxTv,
        u.oldest_date AS oldestDate,
        u.recent_date AS recentDate,
        (SELECT ARRAY_AGG(provider_id) FROM user_subscriptions WHERE user_id = u.user_id) AS providerIds,
        (SELECT ARRAY_AGG(genre_id) FROM user_genres WHERE user_id = u.user_id) AS genreIds,
        (SELECT ARRAY_AGG(genre_id) FROM user_avoid_genres WHERE user_id = u.user_id) AS avoidGenreIds
    FROM users u 
    WHERE u.user_id = :userId
""", nativeQuery = true)

    fun findUserPreferencesById(userId: Int): UserPreferencesProjection?

    @Modifying
    @Transactional
    @Query("UPDATE UserEntity u SET u.recentLogin = :timestamp WHERE u.username = :username")
    fun updateRecentLogin(
        @Param("username") username: String,
        @Param("timestamp") timestamp: String
    )
}

@Repository
interface UserGenresRepository : JpaRepository<UserGenres, UserGenreId> {
    @Query("""
        SELECT ug.id.userId as userId, 
               ug.id.genreId as genreId, 
               g.genreName as genreName, 
               ug.priority as priority 
        FROM UserGenres ug 
        JOIN ug.genre g 
        WHERE ug.id.userId = :userId 
        ORDER BY ug.priority
    """)
    fun findProjectedByUserIdOrderByPriority(userId: Int): List<UserGenreProjection>

    @Query("SELECT ug.id.genreId FROM UserGenres ug WHERE ug.id.userId = :userId ORDER BY ug.priority")
    fun findGenreIdsByUserId(@Param("userId") userId: Int): List<Int>

    @Query("SELECT COALESCE(MAX(ug.priority), 0) FROM UserGenres ug WHERE ug.id.userId = :userId")
    fun findMaxPriorityByUserId(@Param("userId") userId: Int): Int

    @Query("SELECT ug.id.genreId FROM UserGenres ug WHERE ug.id.userId = :userId ORDER BY ug.priority")
    fun findGenreIdsByUserIdOrderedByPriority(@Param("userId") userId: Int): List<Int>

    @Transactional
    @Modifying
    @Query("""
        INSERT INTO user_genres (user_id, genre_id, priority)
        VALUES (:userId, :genreId, :priority)
    """, nativeQuery = true)
    fun insertUserGenre(
        @Param("userId") userId: Int,
        @Param("genreId") genreId: Int,
        @Param("priority") priority: Int
    )
}

@Repository
interface UserSubscriptionRepository : JpaRepository<UserSubscription, UserSubscriptionId> {
    @Query("""
        SELECT us.id.userId as userId, 
               us.id.providerId as providerId, 
               p.providerName as providerName, 
               us.priority as priority 
        FROM UserSubscription us 
        JOIN us.provider p 
        WHERE us.id.userId = :userId 
        ORDER BY us.priority
    """)
    fun findProjectedByUserIdOrderByPriority(userId: Int): List<UserSubscriptionProjection>

    @Query("""
        SELECT us.id.providerId 
        FROM UserSubscription us 
        WHERE us.id.userId = :userId 
        ORDER BY us.priority ASC
    """)
    fun findProviderIdsByUserIdSortedByPriority(@Param("userId") userId: Int): List<Int>

    @Transactional
    @Modifying
    @Query("""
        INSERT INTO user_subscriptions (user_id, provider_id, priority)
        VALUES (:userId, :providerId, :priority)
    """, nativeQuery = true)
    fun insertUserSubscription(
        @Param("userId") userId: Int,
        @Param("providerId") providerId: Int,
        @Param("priority") priority: Int
    )

    @Query("SELECT MAX(us.priority) FROM UserSubscription us WHERE us.id.userId = :userId")
    fun findMaxPriorityByUserId(@Param("userId") userId: Int): Int?
}

@Repository
interface UserAvoidGenresRepository : JpaRepository<UserAvoidGenres, UserAvoidGenreId> {
    @Query(value = "SELECT g.* FROM genre g INNER JOIN user_avoid_genres uag ON g.genre_id = uag.genre_id WHERE uag.user_id = :userId", nativeQuery = true)
    fun findGenresByUserId(@Param("userId") userId: Int): List<GenreEntity>

    @Query(value = "SELECT genre_id FROM user_avoid_genres WHERE user_id = :userId", nativeQuery = true)
    fun findGenreIdsByUserId(@Param("userId") userId: Int): List<Int>

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM user_avoid_genres WHERE user_id = :userId AND genre_id = :genreId", nativeQuery = true)
    fun deleteByUserIdAndGenreId(@Param("userId") userId: Int, @Param("genreId") genreId: Int)

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO user_avoid_genres (user_id, genre_id) VALUES (:userId, :genreId) ON CONFLICT DO NOTHING", nativeQuery = true)
    fun insertUserAvoidGenre(@Param("userId") userId: Int, @Param("genreId") genreId: Int)

}
