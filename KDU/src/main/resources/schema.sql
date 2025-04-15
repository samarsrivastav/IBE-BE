CREATE TABLE property (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL
);
CREATE TABLE configuration (
    id SERIAL PRIMARY KEY,
    property_id int NOT NULL,
    show_guest BOOLEAN NOT NULL DEFAULT FALSE,
    wheel_chair_option BOOLEAN NOT NULL DEFAULT FALSE,
    max_guest_per_room int NOT NULL DEFAULT 4,
    show_room_number BOOLEAN NOT NULL DEFAULT TRUE,
    guest_types JSONB NULL -- Stores guest type and age details as key-value pairs
);
