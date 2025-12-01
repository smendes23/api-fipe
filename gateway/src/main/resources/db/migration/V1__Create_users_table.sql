CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    roles VARCHAR(500) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true
);

CREATE INDEX idx_users_username ON users(username);

INSERT INTO users (username, password, roles, enabled) VALUES
    ('admin', '$2a$10$HFuapgHgToulZuAJM31UPeCRt1trvRoI4jXf8uHU5Z1nE9thz/L6C', 'ROLE_ADMIN,ROLE_USER', true),
    ('userEntity', '$2a$10$CMjgHJqV7hJXlXdRmYKFxeKWFqLfqQlYvVxqZqXqJ7fJZqXqJ7fJZ', 'ROLE_USER', true);
