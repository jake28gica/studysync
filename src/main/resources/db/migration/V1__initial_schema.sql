-- Users Table
CREATE TABLE IF NOT EXISTS users (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     email VARCHAR(255) UNIQUE NOT NULL,
                                     username VARCHAR(100) UNIQUE NOT NULL,
                                     password_hash VARCHAR(255),
                                     first_name VARCHAR(100),
                                     last_name VARCHAR(100),
                                     profile_picture_url TEXT,
                                     provider VARCHAR(50),
                                     provider_id VARCHAR(255),
                                     enabled BOOLEAN DEFAULT TRUE,
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Roles Table
CREATE TABLE IF NOT EXISTS roles (
                                     id BIGSERIAL PRIMARY KEY,
                                     name VARCHAR(50) UNIQUE NOT NULL
);

-- User Roles Junction
CREATE TABLE IF NOT EXISTS user_roles (
                                          user_id UUID REFERENCES users(id) ON DELETE CASCADE,
                                          role_id BIGINT REFERENCES roles(id) ON DELETE CASCADE,
                                          PRIMARY KEY (user_id, role_id)
);

-- Refresh Tokens
CREATE TABLE IF NOT EXISTS refresh_tokens (
                                              id BIGSERIAL PRIMARY KEY,
                                              user_id UUID REFERENCES users(id) ON DELETE CASCADE,
                                              token VARCHAR(500) UNIQUE NOT NULL,
                                              expiry_date TIMESTAMP NOT NULL,
                                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert default roles
INSERT INTO roles (name) VALUES ('ROLE_USER') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('ROLE_ADMIN') ON CONFLICT (name) DO NOTHING;

-- Create indexes for performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_provider ON users(provider, provider_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);