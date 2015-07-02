CREATE TABLE AndroidLux (
    rt  REAL PRIMARY KEY,
    aid INTEGER,
    lux REAL,
    FOREIGN KEY(aid) REFERENCES Activity(id)
);


CREATE TABLE AndroidAccel (
    rt   REAL PRIMARY KEY,
    aid  INTEGER,
    ms2x REAL,
    ms2y REAL,
    ms2z REAL,
    FOREIGN KEY(aid) REFERENCES Activity(id)
);


CREATE TABLE AndroidPress (
    rt    REAL PRIMARY KEY,
    aid   INTEGER,
    press REAL,
    FOREIGN KEY(aid) REFERENCES Activity(id)
);


CREATE TABLE AndroidTemp (
    rt   REAL PRIMARY KEY,
    aid  INTEGER,
    temp REAL,
    FOREIGN KEY(aid) REFERENCES Activity(id)
);


CREATE TABLE AndroidField (
    rt  REAL PRIMARY KEY,
    aid INTEGER,
    uTx REAL,
    uTy REAL,
    uTz REAL,
    FOREIGN KEY(aid) REFERENCES Activity(id)
);
