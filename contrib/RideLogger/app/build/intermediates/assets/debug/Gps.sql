CREATE TABLE Gps (
    rt   REAL PRIMARY KEY,
    aid  INTEGER,
    alt  REAL,
    kph  REAL,
    bear REAL,
    gpsa REAL,
    lat  REAL,
    lon  REAL,
    FOREIGN KEY(aid) REFERENCES Activity(id)
);
