DROP TABLE IF EXISTS reservations;
DROP TABLE IF EXISTS inquiries;
DROP TABLE IF EXISTS workshop_time_slots;
DROP TABLE IF EXISTS workshop_schedules;
DROP TABLE IF EXISTS system_settings;
DROP TABLE IF EXISTS admin_users;

CREATE TABLE admin_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE workshop_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    schedule_date DATE NOT NULL UNIQUE,
    is_open BOOLEAN NOT NULL,
    note VARCHAR(500),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE workshop_time_slots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    schedule_id BIGINT NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    capacity INT NOT NULL,
    reserved_count INT NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_time_slot_schedule FOREIGN KEY (schedule_id) REFERENCES workshop_schedules(id),
    CONSTRAINT uq_time_slot_schedule_start UNIQUE (schedule_id, start_time),
    CONSTRAINT chk_time_slot_capacity CHECK (capacity >= 0),
    CONSTRAINT chk_time_slot_reserved CHECK (reserved_count >= 0 AND reserved_count <= capacity)
);

CREATE TABLE reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_code VARCHAR(50) NOT NULL UNIQUE,
    time_slot_id BIGINT NOT NULL,
    reservation_date DATE NOT NULL,
    reservation_time TIME NOT NULL,
    reservation_count INT NOT NULL,
    participant_count INT,
    male_count INT,
    female_count INT,
    male_under_10_count INT,
    male_20s_count INT,
    male_30s_count INT,
    male_40s_count INT,
    male_50s_count INT,
    male_60_plus_count INT,
    female_under_10_count INT,
    female_20s_count INT,
    female_30s_count INT,
    female_40s_count INT,
    female_50s_count INT,
    female_60_plus_count INT,
    customer_family_name VARCHAR(100) NOT NULL,
    customer_given_name VARCHAR(100) NOT NULL,
    customer_family_kana VARCHAR(100) NOT NULL,
    customer_given_kana VARCHAR(100) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    customer_tel VARCHAR(50),
    customer_message TEXT,
    total_amount INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    admin_memo TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_reservation_time_slot FOREIGN KEY (time_slot_id) REFERENCES workshop_time_slots(id),
    CONSTRAINT chk_reservation_count CHECK (reservation_count > 0),
    CONSTRAINT chk_reservation_participant_count CHECK (participant_count IS NULL OR participant_count >= 0),
    CONSTRAINT chk_reservation_male_count CHECK (male_count IS NULL OR male_count >= 0),
    CONSTRAINT chk_reservation_female_count CHECK (female_count IS NULL OR female_count >= 0),
    CONSTRAINT chk_reservation_male_under_10_count CHECK (male_under_10_count IS NULL OR male_under_10_count >= 0),
    CONSTRAINT chk_reservation_male_20s_count CHECK (male_20s_count IS NULL OR male_20s_count >= 0),
    CONSTRAINT chk_reservation_male_30s_count CHECK (male_30s_count IS NULL OR male_30s_count >= 0),
    CONSTRAINT chk_reservation_male_40s_count CHECK (male_40s_count IS NULL OR male_40s_count >= 0),
    CONSTRAINT chk_reservation_male_50s_count CHECK (male_50s_count IS NULL OR male_50s_count >= 0),
    CONSTRAINT chk_reservation_male_60_plus_count CHECK (male_60_plus_count IS NULL OR male_60_plus_count >= 0),
    CONSTRAINT chk_reservation_female_under_10_count CHECK (female_under_10_count IS NULL OR female_under_10_count >= 0),
    CONSTRAINT chk_reservation_female_20s_count CHECK (female_20s_count IS NULL OR female_20s_count >= 0),
    CONSTRAINT chk_reservation_female_30s_count CHECK (female_30s_count IS NULL OR female_30s_count >= 0),
    CONSTRAINT chk_reservation_female_40s_count CHECK (female_40s_count IS NULL OR female_40s_count >= 0),
    CONSTRAINT chk_reservation_female_50s_count CHECK (female_50s_count IS NULL OR female_50s_count >= 0),
    CONSTRAINT chk_reservation_female_60_plus_count CHECK (female_60_plus_count IS NULL OR female_60_plus_count >= 0)
);

CREATE TABLE inquiries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inquiry_code VARCHAR(50) NOT NULL UNIQUE,
    customer_family_name VARCHAR(100) NOT NULL,
    customer_given_name VARCHAR(100) NOT NULL,
    customer_family_kana VARCHAR(100) NOT NULL,
    customer_given_kana VARCHAR(100) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    customer_tel VARCHAR(50),
    customer_message TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    admin_memo TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE system_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value VARCHAR(500) NOT NULL,
    description VARCHAR(500),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);
