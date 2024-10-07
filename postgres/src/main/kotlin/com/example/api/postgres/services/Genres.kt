class Genres {

    private fun filterGenres(query: String) {
        if (query.isNotEmpty()) {
            val allGenres = DataManager.getGenres()
            val filteredGenres = allGenres
                .map { it to lcs(it.name, query) }
                .sortedByDescending { it.second }
                .map { it.first }
                .take(5)

            updateGenres(filteredGenres)

        } else {
            updateGenres(emptyList())
        }
    }

    private fun filterAvoidGenres(query: String) {
        if (query.isNotEmpty()) {
            val allGenres = DataManager.getGenres()
            val filteredGenres = allGenres
                .map { it to lcs(it.name, query) }
                .sortedByDescending { it.second }
                .map { it.first }
                .take(5)

            updateAvoidGenres(filteredGenres)

        } else {
            updateGenres(emptyList())
        }
    }

    fun insertGenres() {
        val genres = mutableSetOf<Genre>()

    // Initialize with default genres
    init {
        transaction {
            // Check if the database has any genres
            if (Genres.selectAll().empty()) {
                // If no genres in the database, add the default genres
                val defaultGenres = listOf(
                    Genre(28, "Action"),
                    Genre(12, "Adventure"),
                    Genre(16, "Animation"),
                    Genre(35, "Comedy"),
                    Genre(80, "Crime"),
                    Genre(99, "Documentary"),
                    Genre(18, "Drama"),
                    Genre(10751, "Family"),
                    Genre(14, "Fantasy"),
                    Genre(36, "History"),
                    Genre(27, "Horror"),
                    Genre(10402, "Music"),
                    Genre(9648, "Mystery"),
                    Genre(10749, "Romance"),
                    Genre(878, "Science Fiction"),
                    Genre(10770, "TV Movie"),
                    Genre(53, "Thriller"),
                    Genre(10752, "War"),
                    Genre(37, "Western"),
                    Genre(10759, "Action & Adventure"),
                    Genre(10762, "Kids"),
                    Genre(10763, "News"),
                    Genre(10764, "Reality"),
                    Genre(10765, "Sci-Fi & Fantasy"),
                    Genre(10766, "Soap"),
                    Genre(10767, "Talk"),
                    Genre(10768, "War & Politics")
                )

                // Insert default genres into the database
                defaultGenres.forEach { genre ->
                    Genres.insert {
                        it[genreId] = genre.id
                        it[genreName] = genre.name
                    }
                }

                // Add to the in-memory set
                genres.addAll(defaultGenres)
            } else {
                // Load genres from the database
                Genres.selectAll().forEach {
                    val genre = Genre(
                        id = it[Genres.genreId],
                        name = it[Genres.genreName]
                    )
                    genres.add(genre)
                }
            }
        }
    }

    }

    fun addGenre(genre: Genre) {
        transaction {
            Genres.insert {
                it[genreId] = genre.id
                it[genreName] = genre.name
            }
            genres.add(genre)
        }
    }

}
