CREATE TABLE users_favorite_posts
(
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT pk_users_favorite_posts PRIMARY KEY (post_id, user_id),
    CONSTRAINT fk_fav_posts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_fav_posts_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE
);

ALTER TABLE posts
    ADD favorite_count BIGINT DEFAULT 0;

UPDATE posts
    SET favorite_count = 0 WHERE favorite_count IS NULL;

ALTER TABLE posts
    ALTER COLUMN favorite_count SET NOT NULL;

CREATE table users_favorite_collection_points
(
    collection_point_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT pk_users_favorite_collection_points PRIMARY KEY (collection_point_id, user_id),
    CONSTRAINT fk_fav_collection_points_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_fav_collection_points_cp FOREIGN KEY (collection_point_id) REFERENCES collection_points(id) ON DELETE CASCADE
);

ALTER TABLE collection_points
    ADD favorite_count BIGINT DEFAULT 0;

UPDATE collection_points
    SET favorite_count = 0 WHERE favorite_count IS NULL;

ALTER TABLE collection_points
    ALTER COLUMN favorite_count SET NOT NULL;