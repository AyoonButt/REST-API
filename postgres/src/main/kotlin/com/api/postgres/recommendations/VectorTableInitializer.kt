package com.api.postgres.recommendations

import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service

@Service
class VectorTableInitializer(
    private val jdbcTemplate: JdbcTemplate
) {
    private val logger = LoggerFactory.getLogger(VectorTableInitializer::class.java)

    /**
     * Create vector tables if they don't exist
     */
    fun initializeTables() {
        try {
            logger.info("Creating vector tables if needed")

            // Install pgvector extension
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector")

            // Create user vectors table with pgvector
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user_vectors (
                    user_id INT PRIMARY KEY,
                    vector vector(32) NOT NULL,
                    dimension INT NOT NULL,
                    updated_at TIMESTAMP NOT NULL
                )
            """)

            // Create post vectors table with pgvector
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS post_vectors (
                    post_id INT PRIMARY KEY,
                    vector vector(32) NOT NULL,
                    dimension INT NOT NULL,
                    updated_at TIMESTAMP NOT NULL
                )
            """)

            // Create user behavior profiles table
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user_behavior_profiles (
                    user_id INT PRIMARY KEY,
                    profile JSONB NOT NULL,
                    dominant_type VARCHAR(255) NOT NULL,
                    updated_at TIMESTAMP NOT NULL
                )
            """)

            // Create HNSW index for faster similarity search on user vectors
            jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS user_vectors_hnsw_idx 
                ON user_vectors USING hnsw (vector vector_cosine_ops)
            """)

            // Create HNSW index for faster similarity search on post vectors
            jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS post_vectors_hnsw_idx 
                ON post_vectors USING hnsw (vector vector_cosine_ops)
            """)

            // Create index on updated_at for monitoring vector freshness
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS user_vectors_updated_idx ON user_vectors(updated_at)")
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS post_vectors_updated_idx ON post_vectors(updated_at)")

            // Create index for finding users by dominant behavior type
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS user_behavior_dominant_type_idx ON user_behavior_profiles(dominant_type)")

            logger.info("Vector tables created successfully")
        } catch (e: Exception) {
            logger.error("Error creating vector tables: ${e.message}")
            throw e
        }
    }
}