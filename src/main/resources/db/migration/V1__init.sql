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

CREATE TABLE teams (
    id SERIAL PRIMARY KEY,
    name VARCHAR NOT NULL,
    user_id INTEGER REFERENCES verified_users (id) NOT NULL
);

CREATE TABLE team_verified_users (
    team_id INTEGER REFERENCES teams (id) NOT NULL,
    v_user_id INTEGER REFERENCES verified_users (id) NOT NULL
);

CREATE TABLE team_invited_users (
    team_id INTEGER REFERENCES teams (id) NOT NULL,
    email VARCHAR NOT NULL CONSTRAINT ti_email_unique UNIQUE
);

CREATE TABLE games (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES verified_users (id) NOT NULL,
    team_id INTEGER REFERENCES teams (id) NOT NULL,
    location VARCHAR NOT NULL,
    opponent VARCHAR NOT NULL,
    start_date TIMESTAMPTZ NOT NULL
);