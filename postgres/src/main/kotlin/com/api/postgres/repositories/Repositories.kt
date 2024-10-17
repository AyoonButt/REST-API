import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository



@Repository
interface CommentRepository : JpaRepository<CommentEntity, Int> {
    fun findByPostId(postId: Int): List<CommentEntity>
}

@Repository
interface UserTrailerInteractionRepository : JpaRepository<UserTrailerInteractionEntity, Int> {
    fun findByUserIdAndPostId(userId: Int, postId: Int): UserTrailerInteractionEntity?
    fun findByUserIdAndTrailerLikeStateIsTrueOrderByTimestampDesc(userId: Int): List<UserTrailerInteractionEntity>
    fun findByUserIdAndTrailerSaveStateIsTrueOrderByTimestampDesc(userId: Int): List<UserTrailerInteractionEntity>
    fun findByUserIdAndCommentMadeIsTrueOrderByTimestampDesc(userId: Int): List<UserTrailerInteractionEntity>
}

@Repository
interface UserPostInteractionRepository : JpaRepository<UserPostInteraction, Long> {
    
    @Query("SELECT u FROM UserPostInteraction u WHERE u.userId = :userId")
    fun findByUserId(@Param("userId") userId: Int): List<UserPostInteraction>

    @Query("SELECT u FROM UserPostInteraction u WHERE u.userId = :userId AND u.postId = :postId")
    fun findByUserIdAndPostId(@Param("userId") userId: Int, @Param("postId") postId: Int): UserPostInteraction?

    @Query("SELECT DISTINCT u.postId FROM UserPostInteraction u WHERE u.userId = :userId AND u.likeState = true ORDER BY u.timestamp DESC")
    fun findLikedPostsByUserId(@Param("userId") userId: Int): List<Int>

    @Query("SELECT DISTINCT u.postId FROM UserPostInteraction u WHERE u.userId = :userId AND u.saveState = true ORDER BY u.timestamp DESC")
    fun findSavedPostsByUserId(@Param("userId") userId: Int): List<Int>

    @Query("SELECT DISTINCT u.postId FROM UserPostInteraction u WHERE u.userId = :userId AND u.commentMade = true ORDER BY u.timestamp DESC")
    fun findCommentMadePostsByUserId(@Param("userId") userId: Int): List<Int>
}

@Repository
interface PostRepository : JpaRepository<Post, Int> {
    fun findAllByOrderByPostId(limit: Int, offset: Int): List<Post>
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
interface GenreRepository : JpaRepository<Genre, Long> {
    fun findByNameContainingIgnoreCase(query: String): List<Genre>
}

interface ProviderRepository : JpaRepository<Provider, Int> {
    fun findAllByOrderById(limit: Int, offset: Int): List<Provider>
}

@Repository
interface UserRepository : JpaRepository<User, Int> {
    fun findByUsernameAndPswd(username: String, password: String): User?

    @Query("SELECT u FROM User u WHERE u.userId = :userId")
    fun fetchUserParams(userId: Int): User?
}

