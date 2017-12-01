CREATE TABLE locations (
    id SERIAL PRIMARY KEY
);

CREATE TABLE games (
    id SERIAL PRIMARY KEY,
    owner_id INTEGER REFERENCES verified_users (id) NOT NULL,
    location_id INTEGER REFERENCES locations (id) NOT NULL,
    start_date TIMESTAMPTZ NOT NULL,
    end_date TIMESTAMPTZ NOT NULL
);