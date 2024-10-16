package com.example.api.postgres.services

class Credits {

    fun parseAndInsertCredits(creditsJson: String, postId: Int) {
        // Convert JSON string to JSONObject
        val credits = JSONObject(creditsJson)
    
        // Parse and insert cast details
        val castArray = credits.getJSONArray("cast")
        for (i in 0 until castArray.length()) {
            val castMember = castArray.getJSONObject(i)
            val roles = castMember.getJSONArray("roles").getJSONObject(0)
    
            transaction {
                Cast.insert {
                    it[Cast.postId] = postId
                    it[personId] = castMember.getInt("id")
                    it[name] = castMember.getString("name")
                    it[gender] = castMember.optInt("gender", -1)
                    it[knownForDepartment] = castMember.getString("known_for_department")
                    it[character] = roles.optString("character", "")
                    it[episodeCount] = roles.optInt("episode_count", 0)
                    it[orderIndex] = castMember.optInt("order", -1)
                    it[popularity] = castMember.optDouble("popularity", 0.0).toBigDecimal()
                    it[profilePath] = castMember.optString("profile_path", null)
                }
            }
        }
    
        // Parse and insert crew details
        val crewArray = credits.getJSONArray("crew")
        for (i in 0 until crewArray.length()) {
            val crewMember = crewArray.getJSONObject(i)
            val jobs = crewMember.getJSONArray("jobs").getJSONObject(0)
    
            transaction {
                Crew.insert {
                    it[Crew.postId] = postId
                    it[personId] = crewMember.getInt("id")
                    it[name] = crewMember.getString("name")
                    it[gender] = crewMember.optInt("gender", -1)
                    it[knownForDepartment] = crewMember.getString("known_for_department")
                    it[job] = jobs.optString("job", "")
                    it[department] = crewMember.optString("department", "")
                    it[episodeCount] = jobs.optInt("episode_count", 0)
                    it[popularity] = crewMember.optDouble("popularity", 0.0).toBigDecimal()
                    it[profilePath] = crewMember.optString("profile_path", null)
                }
            }
        }
    }

    fun loadCreditsFromDatabase(postId: Int): Map<String, List<Map<String, Any?>>> {
        val castList = mutableListOf<Map<String, Any?>>()
        val crewList = mutableListOf<Map<String, Any?>>()

        transaction {
            // Load cast details from the database
            Cast.select { Cast.postId eq postId }.forEach { row ->
                castList.add(
                    mapOf(
                        "personId" to row[Cast.personId],
                        "name" to row[Cast.name],
                        "gender" to row[Cast.gender],
                        "knownForDepartment" to row[Cast.knownForDepartment],
                        "character" to row[Cast.character],
                        "episodeCount" to row[Cast.episodeCount],
                        "orderIndex" to row[Cast.orderIndex],
                        "popularity" to row[Cast.popularity],
                        "profilePath" to row[Cast.profilePath]
                    )
                )
            }

            // Load crew details from the database
            Crew.select { Crew.postId eq postId }.forEach { row ->
                crewList.add(
                    mapOf(
                        "personId" to row[Crew.personId],
                        "name" to row[Crew.name],
                        "gender" to row[Crew.gender],
                        "knownForDepartment" to row[Crew.knownForDepartment],
                        "job" to row[Crew.job],
                        "department" to row[Crew.department],
                        "episodeCount" to row[Crew.episodeCount],
                        "popularity" to row[Crew.popularity],
                        "profilePath" to row[Crew.profilePath]
                    )
                )
            }
        }

        // Return the cast and crew information as a map
        return mapOf(
            "cast" to castList,
            "crew" to crewList
        )
    }
}
