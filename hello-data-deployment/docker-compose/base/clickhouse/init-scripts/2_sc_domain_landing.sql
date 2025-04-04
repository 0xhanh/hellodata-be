-- Default Data Domain dashboard dwh udm schema
CREATE TABLE IF NOT EXISTS sc_domain_staging.actors
(
    id         UInt32,
    first_name String,
    last_name  String,
    gender     FixedString(1),
    created_at DateTime DEFAULT now(),
    updated_at DateTime DEFAULT now(),
    ingested_at DateTime DEFAULT now()
) ENGINE = MergeTree ORDER BY (id, first_name, last_name, gender);

CREATE TABLE IF NOT EXISTS sc_domain_staging.directors
(
    id         UInt32,
    first_name String,
    last_name  String,
    created_at DateTime DEFAULT now(),
    updated_at DateTime DEFAULT now(),
    ingested_at DateTime DEFAULT now()
) ENGINE = MergeTree ORDER BY (id, first_name, last_name);

CREATE TABLE IF NOT EXISTS sc_domain_staging.genres
(
    movie_id UInt32,
    genre    String,
    created_at DateTime DEFAULT now(),
    updated_at DateTime DEFAULT now(),
    ingested_at DateTime DEFAULT now()
) ENGINE = MergeTree ORDER BY (movie_id, genre);

CREATE TABLE IF NOT EXISTS sc_domain_staging.movie_directors
(
    director_id UInt32,
    movie_id    UInt64,
    created_at DateTime DEFAULT now(),
    updated_at DateTime DEFAULT now(),
    ingested_at DateTime DEFAULT now()
) ENGINE = MergeTree ORDER BY (director_id, movie_id);

CREATE TABLE IF NOT EXISTS sc_domain_staging.movies
(
    id   UInt32,
    name String,
    year UInt32,
    rank Float32 DEFAULT 0,
    created_at DateTime DEFAULT now(),
    updated_at DateTime DEFAULT now(),
    ingested_at DateTime DEFAULT now()
) ENGINE = MergeTree ORDER BY (id, name, year);

CREATE TABLE IF NOT EXISTS sc_domain_staging.roles
(
    actor_id   UInt32,
    movie_id   UInt32,
    role       String,
    created_at DateTime DEFAULT now(),
    updated_at DateTime DEFAULT now(),
    ingested_at DateTime DEFAULT now()
) ENGINE = MergeTree ORDER BY (actor_id, movie_id);