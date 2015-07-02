CREATE TABLE AntPower (
    rt    REAL PRIMARY KEY,
    aid   INTEGER,
    adn   INTEGER,
    watts REAL,
    FOREIGN KEY(aid) REFERENCES Activity(id)
);


CREATE TABLE AntTorque (
    rt  REAL PRIMARY KEY,
    aid INTEGER,
    adn INTEGER,
    nm  REAL,
    FOREIGN KEY(aid) REFERENCES Activity(id)
);


CREATE TABLE AntCadence (
    rt  REAL PRIMARY KEY,
    aid INTEGER,
    adn INTEGER,
    cad REAL,
    FOREIGN KEY(aid) REFERENCES Activity(id)
);


CREATE TABLE AntSpeed (
    rt  REAL PRIMARY KEY,
    aid INTEGER,
    adn INTEGER,
    kph REAL,
    FOREIGN KEY(aid) REFERENCES Activity(id)
);


CREATE TABLE AntDistance (
    rt  REAL PRIMARY KEY,
    aid INTEGER,
    adn INTEGER,
    km  REAL,
    FOREIGN KEY(aid) REFERENCES Activity(id)
);

