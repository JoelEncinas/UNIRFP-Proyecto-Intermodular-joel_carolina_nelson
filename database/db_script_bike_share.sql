CREATE DATABASE IF NOT EXISTS bike_share_db;

USE bike_share_db;

-- 1. Users Table (Riders and Admins)
CREATE TABLE users(
id BIGINT AUTO_INCREMENT PRIMARY KEY,
username VARCHAR(50) NOT NULL UNIQUE,
email VARCHAR(100) NOT NULL UNIQUE,
password VARCHAR(255) NOT NULL,
role ENUM ('RIDER', 'ADMIN') DEFAULT 'RIDER',
balance DECIMAL(10, 2) DEFAULT 0.00,
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB; 

-- 2. Stations Table
CREATE TABLE stations(
id BIGINT AUTO_INCREMENT PRIMARY KEY,
name VARCHAR(100) NOT NULL,
address VARCHAR(255),
latitude DECIMAL(10,8),
longitude DECIMAL(11,8),
capacity INT NOT NULL
)ENGINE=InnoDB;

-- 3. Bikes Table
CREATE TABLE bikes(
id BIGINT AUTO_INCREMENT PRIMARY KEY,
model VARCHAR(50),
status ENUM('AVAILABLE', 'BOOKED', 'BUSY', 'MAINTENANCE') DEFAULT 'AVAILABLE',
station_id BIGINT,
FOREIGN KEY (station_id) REFERENCES stations(id) ON DELETE SET NULL
)ENGINE=InnoDB;

-- 4. Bookins table (Se ha añadido una logica de 15 min de reservación)
CREATE TABLE bookings(
id BIGINT AUTO_INCREMENT PRIMARY KEY,
user_id BIGINT NOT NULL, 
bike_id BIGINT NOT NULL, 
start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
expiry_time TIMESTAMP,
status ENUM('PENDING', 'ACTIVE', 'COMPLETED', 'CANCELLED') DEFAULT 'PENDING',
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
FOREIGN KEY (bike_id) REFERENCES bikes(id) ON DELETE CASCADE
)ENGINE=InnoDB;

-- 5. Payments table (History and Audit Trail)
CREATE TABLE payments(
id BIGINT AUTO_INCREMENT PRIMARY KEY,
user_id BIGINT NOT NULL, 
booking_id BIGINT NULL,
amount DECIMAL(10, 2) NOT NULL,
provider VARCHAR(50),  -- Ejemplo: Paypal o Stripe
sandbox_reference VARCHAR(100),
payment_method ENUM('CREDIT_CARD', 'PAYPAL', 'APP_CREDIT') NOT NULL, 
transaction_type ENUM('TOP_UP', 'RENTAL_PAYMENT') NOT NULL,
payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE SET NULL
)ENGINE=InnoDB;

/*
PRIMARY KEYS


users,id,BIGINT,Unique ID for each Rider or Admin.
stations,id,BIGINT,Unique ID for each physical bike station.
bikes,id,BIGINT,Unique ID for the bike (internal database use).
bookings,id,BIGINT,Unique ID for each rental transaction/session.
payments,id,BIGINT,Unique ID for each financial transaction/receipt.


*/


