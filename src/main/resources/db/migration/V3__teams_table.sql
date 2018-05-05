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