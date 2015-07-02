CREATE TABLE Base (
    rt  REAL PRIMARY KEY,
    aid INTEGER,
    FOREIGN KEY(aid) REFERENCES Activity(id)
);
