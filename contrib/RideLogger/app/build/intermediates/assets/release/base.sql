CREATE TABLE %TABLENAME% (
    rt REAL PRIMARY KEY,
    activity_id   INTEGER,
    FOREIGN KEY(activity_id) REFERENCES activity(id)
);
