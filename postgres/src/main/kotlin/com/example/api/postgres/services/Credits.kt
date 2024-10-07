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
}
