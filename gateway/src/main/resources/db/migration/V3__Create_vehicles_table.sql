CREATE TABLE vehicles (
                          id SERIAL PRIMARY KEY,
                          code VARCHAR(50) NOT NULL,
                          brand_code VARCHAR(50) NOT NULL,
                          model VARCHAR(200) NOT NULL,
                          observations TEXT,
                          created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                          updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                          CONSTRAINT uk_vehicle_code_brand UNIQUE (code, brand_code)
);

CREATE INDEX IF NOT EXISTS idx_vehicles_brand ON vehicles(brand_code);
CREATE INDEX IF NOT EXISTS idx_vehicles_code ON vehicles(code);
CREATE INDEX IF NOT EXISTS idx_vehicles_model ON vehicles(model);