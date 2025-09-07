CREATE TABLE IF NOT EXISTS content(
    id VARCHAR(40) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    subject VARCHAR(100) NOT NULL,
    grade VARCHAR(50) NOT NULL,
    chapter VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    source_object VARCHAR(512),
    created_at TIMESTAMPTZ NOT NULL
    );
