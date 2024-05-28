--liquibase formatted sql

--changeset jpark:00-initial-schema
CREATE TABLE IF NOT EXISTS transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    date INTEGER NOT NULL,
    description TEXT NOT NULL,
    amount REAL NOT NULL,
    category TEXT NOT NULL,
    critical INTEGER NOT NULL,
    type TEXT NOT NULL,
    originHash TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS bookmarks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name STRING NOT NULL,
    bookmark INTEGER NOT NULL,
    run_timestamp INTEGER NOT NULL
);
