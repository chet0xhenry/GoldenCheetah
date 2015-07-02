CREATE TABLE AntHeartRate (
    rt  REAL PRIMARY KEY,
    aid INTEGER,
    adn INTEGER,
    hr  REAL,
    FOREIGN KEY(aid) REFERENCES Activity(id)
);
