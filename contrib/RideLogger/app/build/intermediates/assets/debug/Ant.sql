CREATE TABLE Ant (
    rt  REAL PRIMARY KEY,
    aid INTEGER,
    adn INTEGER,
    FOREIGN KEY(aid) REFERENCES Activity(id)
);
