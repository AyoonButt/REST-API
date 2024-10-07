package com.example.api.postgres

import org.jetbrains.exposed.sql.Database
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PostgresApplication{

    @PostConstruct
    fun init() {
        connectToDatabase()
		createTables
    }
}

fun main(args: Array<String>) {
	runApplication<PostgresApplication>(*args)
}

fun connectToDatabase() {
    try {
        Database.connect(
            url = "jdbc:postgresql://recommendation-app.c76yuoa4ivv0.us-east-2.rds.amazonaws.com:5432/postgres",
            driver = "org.postgresql.Driver",
            user = "postgres",  // e.g., "postgres"
            password = "Watermelon2131"  // your local database password
        )
    } catch (e: Exception) {
        Log.e("DatabaseConnection", "Connection to local DB failed: ${e.message}", e)
    }
}


fun createTables() {
    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(
            Users,
            Posts,
            UserPostInteractions,
            UserTrailerInteractions,
            Comments,
            SubscriptionProviders,
            Genres,
            UserSubscriptions,
            PostGenres
        )
    }
}