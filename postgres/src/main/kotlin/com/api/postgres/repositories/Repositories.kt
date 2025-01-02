package com.api.postgres.repositories


import com.api.postgres.CastProjection
import com.api.postgres.CommentProjection
import com.api.postgres.CrewProjection
import com.api.postgres.PostProjection
import com.api.postgres.TrailerInteractionProjection
import com.api.postgres.UserPostInteractionProjection
import com.api.postgres.UserPreferencesProjection
import com.api.postgres.UserProjection
import com.api.postgres.models.*


import org.springframework.data.jpa.repository.JpaRepository  // Import JpaRepository
import org.springframework.data.jpa.repository.Query  // Import for @Query annotation
import org.springframework.data.repository.query.Param  // Import for @Param annotation
import org.springframework.stereotype.Repository  // Import for @Repository annotation
import org.springframework.data.jpa.repository.Modifying

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
            c.parent_comment_id as parentCommentId
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
    @Query(
        value = """
    INSERT INTO comments (user_id, post_id, content, sentiment, timestamp, parent_comment_id)
    VALUES (:userId, :postId, :content, :sentiment, :timestamp, :parentCommentId)
    """,
        nativeQuery = true
    )
    fun insertComment(
        @Param("userId") userId: Int,
        @Param("postId") postId: Int,
        @Param("content") content: String,
        @Param("sentiment") sentiment: String?,
        @Param("timestamp") timestamp: String?,
        @Param("parentCommentId") parentCommentId: Int?
    )

    @Modifying
    @Query(
        value = """
        INSERT INTO comments (parent_comment_id, user_id, post_id, content, sentiment, timestamp)
        VALUES (:parentCommentId, :userId, :postId, :content, :sentiment, :timestamp)
        """,
        nativeQuery = true
    )
    fun insertReply(
        @Param("parentCommentId") parentCommentId: Int,
        @Param("userId") userId: Int,
        @Param("postId") postId: Int,
        @Param("content") content: String,
        @Param("sentiment") sentiment: String?,
        @Param("timestamp") timestamp: String?
    )



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
            c.parent_comment_id as parentCommentId
        FROM comments c
        JOIN users u ON c.user_id = u.user_id
        WHERE c.parent_comment_id = :parentId
        ORDER BY c.timestamp DESC
        LIMIT :limit OFFSET :offset
        """,
        nativeQuery = true
    )
    fun findRepliesByParentId(
        @Param("parentId") parentId: Int,
        @Param("limit") limit: Int,
        @Param("offset") offset: Int
    ): List<CommentProjection>

    @Query(
        value = """
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
            1 as depth,
            ARRAY[c.timestamp] as path
        FROM comments c
        JOIN users u ON c.user_id = u.user_id
        WHERE c.parent_comment_id = :parentId
        AND c.user_id = :userId

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
            rh.depth + 1,
            rh.path || c.timestamp
        FROM comments c
        JOIN users u ON c.user_id = u.user_id
        JOIN reply_hierarchy rh ON c.parent_comment_id = rh.comment_id
        WHERE c.user_id = :userId
    )
    SELECT * FROM reply_hierarchy
    ORDER BY path DESC
    LIMIT :limit OFFSET :offset
""", nativeQuery = true)

    fun findRepliesByParentIdAndUserId(
        @Param("parentId") parentId: Int,
        @Param("userId") userId: Int,
        @Param("limit") limit: Int,
        @Param("offset") offset: Int
    ): List<CommentProjection>

    @Query("""
    SELECT DISTINCT u.username 
    FROM CommentEntity c 
    LEFT JOIN c.parentComment p 
    LEFT JOIN p.user u 
    WHERE c.commentId = :commentId
    """)
    fun findParentCommentUsername(@Param("commentId") commentId: Int): String?
}

@Repository
interface UserTrailerInteractionRepository : JpaRepository<UserTrailerInteraction, Int> {
    @Query("""
        SELECT t.interactionId as interactionId, 
               t.user.userId as userId, 
               t.post.postId as postId, 
               t.timeSpent as timeSpent,
               t.replayCount as replayCount, 
               t.isMuted as isMuted, 
               t.likeState as likeState, 
               t.saveState as saveState,
               t.commentButtonPressed as commentButtonPressed, 
               t.commentMade as commentMade, 
               t.timestamp as timestamp
        FROM UserTrailerInteraction t
        WHERE t.user.userId = :userId
    """)
    fun findDtosByUserId(@Param("userId") userId: Int): List<TrailerInteractionProjection>

    @Query("""
        SELECT t.interactionId as interactionId, 
               t.user.userId as userId, 
               t.post.postId as postId, 
               t.timeSpent as timeSpent,
               t.replayCount as replayCount, 
               t.isMuted as isMuted, 
               t.likeState as likeState, 
               t.saveState as saveState,
               t.commentButtonPressed as commentButtonPressed, 
               t.commentMade as commentMade, 
               t.timestamp as timestamp
        FROM UserTrailerInteraction t
        WHERE t.user.userId = :userId AND t.post.postId = :postId
    """)
    fun findDtoByUserAndPost(
        @Param("userId") userId: Int,
        @Param("postId") postId: Int
    ): TrailerInteractionProjection?

    @Modifying
    @Query("""
        UPDATE UserTrailerInteraction t 
        SET t.timestamp = :timestamp 
        WHERE t.post.postId = :postId
    """)
    fun updateTimestamp(
        @Param("postId") postId: Int,
        @Param("timestamp") timestamp: String
    )

    @Modifying
    @Query("""
        INSERT INTO user_trailer_interactions (
            user_id, post_id, time_spent, replay_count, is_muted,
            like_state, save_state, comment_button_pressed,
            comment_made, timestamp
        ) VALUES (
            :userId, :postId, :timeSpent, :replayCount, :isMuted,
            :likeState, :saveState, :commentButtonPressed,
            :commentMade, :timestamp
        )
    """, nativeQuery = true)
    fun insertInteraction(
        @Param("userId") userId: Int,
        @Param("postId") postId: Int,
        @Param("timeSpent") timeSpent: Long,
        @Param("replayCount") replayCount: Int,
        @Param("isMuted") isMuted: Boolean,
        @Param("likeState") likeState: Boolean,
        @Param("saveState") saveState: Boolean,
        @Param("commentButtonPressed") commentButtonPressed: Boolean,
        @Param("commentMade") commentMade: Boolean,
        @Param("timestamp") timestamp: String
    )

    @Query("""
        SELECT p.postId 
        FROM UserTrailerInteraction t 
        JOIN t.post p 
        WHERE t.user.userId = :userId AND t.likeState = true 
        ORDER BY t.timestamp DESC
    """)
    fun findLikedPostIds(@Param("userId") userId: Int): List<Int>

    @Query("""
        SELECT p.postId 
        FROM UserTrailerInteraction t 
        JOIN t.post p 
        WHERE t.user.userId = :userId AND t.saveState = true 
        ORDER BY t.timestamp DESC
    """)
    fun findSavedPostIds(@Param("userId") userId: Int): List<Int>

    @Query("""
        SELECT p.postId 
        FROM UserTrailerInteraction t 
        JOIN t.post p 
        WHERE t.user.userId = :userId AND t.commentMade = true 
        ORDER BY t.timestamp DESC
    """)
    fun findCommentMadePostIds(@Param("userId") userId: Int): List<Int>
}


@Repository
interface UserPostInteractionRepository : JpaRepository<UserPostInteraction, Long> {

    @Modifying
    @Query("""
        INSERT INTO user_post_interactions 
        (user_id, post_id, time_spent_on_post, like_state, save_state, 
         comment_button_pressed, comment_made, timestamp)
        VALUES (:userId, :postId, :timeSpentOnPost, :likeState, :saveState, 
                :commentButtonPressed, :commentMade, :timestamp)
    """, nativeQuery = true)
    fun insertInteraction(
        @Param("userId") userId: Int,
        @Param("postId") postId: Int,
        @Param("timeSpentOnPost") timeSpentOnPost: Long,
        @Param("likeState") likeState: Boolean,
        @Param("saveState") saveState: Boolean,
        @Param("commentButtonPressed") commentButtonPressed: Boolean,
        @Param("commentMade") commentMade: Boolean,
        @Param("timestamp") timestamp: String
    )

    @Modifying
    @Query("""
        UPDATE user_post_interactions 
        SET timestamp = :timestamp
        WHERE user_id = :userId AND post_id = :postId
    """, nativeQuery = true)
    fun updateTimestamp(
        @Param("userId") userId: Int,
        @Param("postId") postId: Int,
        @Param("timestamp") timestamp: String
    )

    @Query(
        value = """
        SELECT 
            i.interaction_id as interactionId,
            i.user_id as userId,
            i.post_id as postId,
            i.time_spent_on_post as timeSpentOnPost,
            i.like_state as likeState,
            i.save_state as saveState,
            i.comment_button_pressed as commentButtonPressed,
            i.comment_made as commentMade,
            i.timestamp
        FROM user_post_interactions i
        WHERE i.user_id = :userId
        ORDER BY i.timestamp DESC
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
            i.time_spent_on_post as timeSpentOnPost,
            i.like_state as likeState,
            i.save_state as saveState,
            i.comment_button_pressed as commentButtonPressed,
            i.comment_made as commentMade,
            i.timestamp
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

    // These queries are already optimized as they only return post IDs
    @Query("SELECT DISTINCT u.post.postId FROM UserPostInteraction u WHERE u.user.userId = :userId AND u.likeState = true ORDER BY u.timestamp DESC")
    fun findLikedPostsByUserUserId(@Param("userId") userId: Int): List<Int>

    @Query("SELECT DISTINCT u.post.postId FROM UserPostInteraction u WHERE u.user.userId = :userId AND u.saveState = true ORDER BY u.timestamp DESC")
    fun findSavedPostsByUserUserId(@Param("userId") userId: Int): List<Int>

    @Query("SELECT DISTINCT u.post.postId FROM UserPostInteraction u WHERE u.user.userId = :userId AND u.commentMade = true ORDER BY u.timestamp DESC")
    fun findCommentMadePostsByUserUserId(@Param("userId") userId: Int): List<Int>
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
    @Query(
        value = "UPDATE posts SET post_like_count = post_like_count + 1 WHERE post_id = :postId",
        nativeQuery = true
    )
    fun incrementPostLikeCount(@Param("postId") postId: Int)

    @Modifying
    @Query(
        value = "UPDATE posts SET trailer_like_count = trailer_like_count + 1 WHERE post_id = :postId",
        nativeQuery = true
    )
    fun incrementTrailerLikeCount(@Param("postId") postId: Int)
}

@Repository
interface PostGenresRepository : JpaRepository<PostGenres, PostGenreId> {
    @Modifying
    @Query("""
        INSERT INTO post_genres (post_id, genre_id)
        VALUES (:postId, :genreId)
    """, nativeQuery = true)
    fun insertPostGenre(
        @Param("postId") postId: Int,
        @Param("genreId") genreId: Int
    )
}

@Repository
interface PostSubscriptionsRepository : JpaRepository<PostSubscriptions, PostSubscriptionId> {
    @Modifying
    @Query("""
        INSERT INTO post_subscriptions (post_id, provider_id)
        VALUES (:postId, :providerId)
    """, nativeQuery = true)
    fun insertPostSubscription(
        @Param("postId") postId: Int,
        @Param("providerId") providerId: Int
    )
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
    @Query("""
        INSERT INTO cast_members (
            post_id, person_id, name, gender, known_for_department,
            character, episode_count, order_index, popularity, profile_path
        ) VALUES (
            :postId, :personId, :name, :gender, :knownForDepartment,
            :character, :episodeCount, :orderIndex, :popularity, :profilePath
        )
    """, nativeQuery = true)
    fun insertCast(
        @Param("postId") postId: Int,
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
        FROM crew_members c
        WHERE c.post_id = :postId
        ORDER BY c.popularity DESC
        """,
        nativeQuery = true
    )
    fun findDtosByPostId(@Param("postId") postId: Int): List<CrewProjection>

    @Modifying
    @Query("""
        INSERT INTO crew_members (
            post_id, person_id, name, gender, known_for_department,
            job, department, episode_count, popularity, profile_path
        ) VALUES (
            :postId, :personId, :name, :gender, :knownForDepartment,
            :job, :department, :episodeCount, :popularity, :profilePath
        )
    """, nativeQuery = true)
    fun insertCrew(
        @Param("postId") postId: Int,
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

    @Query("SELECT p FROM SubscriptionProvider p WHERE p.providerId = :providerId")
    fun findByProviderId(@Param("providerId") providerId: Int): SubscriptionProvider?
}

@Repository
interface UserRepository : JpaRepository<UserEntity, Int> {
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
    SELECT u.userId as userId,
           u.language as language,
           u.region as region,
           u.minMovie as minMovie,
           u.maxMovie as maxMovie,
           u.minTV as minTV,
           u.maxTV as maxTV,
           u.oldestDate as oldestDate,
           u.recentDate as recentDate,
           (SELECT us.id.providerId FROM UserSubscription us WHERE us.user.userId = u.userId) as subscriptions,
           (SELECT ug.id.genreId FROM UserGenres ug WHERE ug.user.userId = u.userId) as genreIds,
           (SELECT avg.id.genreId FROM UserAvoidGenres avg WHERE avg.user.userId = u.userId) as avoidGenreIds
    FROM UserEntity u
    WHERE u.userId = :userId
""")
    fun findUserPreferencesById(userId: Int): UserPreferencesProjection?

    @Modifying
    @Query("UPDATE UserEntity u SET u.recentLogin = :timestamp WHERE u.username = :username")
    fun updateRecentLogin(
        @Param("username") username: String,
        @Param("timestamp") timestamp: String
    )
}

@Repository
interface UserGenresRepository : JpaRepository<UserGenres, UserGenreId> {
    @Query("SELECT COALESCE(MAX(ug.priority), 0) FROM UserGenres ug WHERE ug.user.userId = :userId")
    fun findMaxPriorityByUserId(@Param("userId") userId: Int): Int

    @Query("SELECT ug.id.genreId FROM UserGenres ug WHERE ug.user.userId = :userId ORDER BY ug.priority")
    fun findGenreIdsByUserId(@Param("userId") userId: Int): List<Int>

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
    @Query("SELECT COALESCE(MAX(us.priority), 0) FROM UserSubscription us WHERE us.user.userId = :userId")
    fun findMaxPriorityByUserId(@Param("userId") userId: Int): Int

    @Query("""
        SELECT us.id.providerId 
        FROM UserSubscription us 
        WHERE us.user.userId = :userId 
        ORDER BY us.priority ASC
    """)
    fun findProviderIdsByUserIdSortedByPriority(@Param("userId") userId: Int): List<Int>

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
}

@Repository
interface UserAvoidGenresRepository : JpaRepository<UserAvoidGenres, UserAvoidGenreId> {
    @Query("SELECT uag.id.genreId FROM UserAvoidGenres uag WHERE uag.user.userId = :userId")
    fun findAvoidGenreIdsByUserId(@Param("userId") userId: Int): List<Int>

    @Modifying
    @Query("""
        INSERT INTO user_avoid_genres (user_id, genre_id)
        VALUES (:userId, :genreId)
    """, nativeQuery = true)
    fun insertUserAvoidGenre(
        @Param("userId") userId: Int,
        @Param("genreId") genreId: Int
    )
}
