CREATE TABLE brands (
                        id SERIAL PRIMARY KEY,
                        code VARCHAR(10) NOT NULL,
                        name VARCHAR(100) NOT NULL,
                        created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_brands_name ON brands(name);
