CREATE TABLE verified_users (
    id SERIAL PRIMARY KEY,
    email VARCHAR NOT NULL CONSTRAINT vu_email_unique UNIQUE,
    password VARCHAR NOT NULL,
    name VARCHAR NOT NULL,
    roles VARCHAR NOT NULL
);

CREATE TABLE unverified_users (
    email VARCHAR PRIMARY KEY,
    signup_datetime TIMESTAMP NOT NULL,
    token VARCHAR NOT NULL CONSTRAINT uu_token_unique UNIQUE
);