CREATE TABLE tokens
(
    id          UUID PRIMARY KEY,
    content     VARCHAR(255) NOT NULL,
    type        VARCHAR(50)  NOT NULL,
    expire_time TIMESTAMP    NOT NULL,
    user_id     UUID         NOT NULL,

    CONSTRAINT fk_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
