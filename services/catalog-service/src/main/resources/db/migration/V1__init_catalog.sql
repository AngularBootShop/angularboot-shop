CREATE TABLE IF NOT EXISTS products (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    sku VARCHAR(64) NOT NULL UNIQUE,
    description TEXT,
    price NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    stock INTEGER NOT NULL CHECK (stock >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_products_sku ON products(sku);
CREATE INDEX IF NOT EXISTS idx_products_name ON products(name);

-- Seed initial catalog items
INSERT INTO products (name, sku, description, price, stock, version)
VALUES 
('Wireless Noise-Canceling Headphones', 'TECH-HEADPHONE-001', 'Premium over-ear headphones with active noise cancellation and 40h battery life.', 249.99, 50, 0),
('Mechanical Keyboard RGB', 'TECH-KEYBOARD-002', 'Custom mechanical gaming and typing keyboard with hot-swappable switches.', 129.50, 35, 0),
('Ultra-HD 4K Curved Monitor 32"', 'TECH-MONITOR-003', '32-inch 4K UHD 144Hz HDR curved gaming and productivity monitor.', 499.00, 20, 0),
('Ergonomic Wireless Mouse', 'TECH-MOUSE-004', 'Precision wireless mouse with customizable macro buttons and hyper-fast scroll.', 79.99, 100, 0),
('Thunderbolt 4 Docking Station', 'TECH-DOCK-005', 'High-speed 100W power delivery docking station supporting dual 4K displays.', 199.95, 15, 0);
