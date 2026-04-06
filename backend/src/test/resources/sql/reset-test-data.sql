SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM payments;
DELETE FROM bookings;
DELETE FROM bikes;
DELETE FROM stations;
DELETE FROM users;
SET FOREIGN_KEY_CHECKS = 1;

ALTER TABLE users AUTO_INCREMENT = 1;
ALTER TABLE stations AUTO_INCREMENT = 1;
ALTER TABLE bikes AUTO_INCREMENT = 1;
ALTER TABLE bookings AUTO_INCREMENT = 1;
ALTER TABLE payments AUTO_INCREMENT = 1;

INSERT INTO stations (id, name, address, latitude, longitude, capacity) VALUES
  (1, 'Station Full', 'Full Address', 40.40000000, -3.70000000, 2),
  (2, 'Station Open', 'Open Address', 40.40100000, -3.70100000, 5),
  (3, 'Station Pending', 'Pending Address', 40.40200000, -3.70200000, 5),
  (4, 'Station Extra', 'Extra Address', 40.40300000, -3.70300000, 5);

INSERT INTO users (id, username, email, password, role, balance, created_at) VALUES
  (1, 'admin1', 'admin1@test.com', '$2a$10$kCxHt4INvyMwg1xMUc8bqOVd6P1K8keABlGcNVvLUNIJ3d4ThPe3e', 'ADMIN', 20.00, '2026-01-01 08:00:00'),
  (2, 'rider1', 'rider1@test.com', '$2a$10$kCxHt4INvyMwg1xMUc8bqOVd6P1K8keABlGcNVvLUNIJ3d4ThPe3e', 'RIDER', 10.00, '2026-01-01 08:10:00'),
  (3, 'rider2', 'rider2@test.com', '$2a$10$kCxHt4INvyMwg1xMUc8bqOVd6P1K8keABlGcNVvLUNIJ3d4ThPe3e', 'RIDER', 0.00, '2026-01-01 08:20:00'),
  (4, 'rider_active', 'rider_active@test.com', '$2a$10$kCxHt4INvyMwg1xMUc8bqOVd6P1K8keABlGcNVvLUNIJ3d4ThPe3e', 'RIDER', 5.00, '2026-01-01 08:30:00'),
  (5, 'rider_pending', 'rider_pending@test.com', '$2a$10$kCxHt4INvyMwg1xMUc8bqOVd6P1K8keABlGcNVvLUNIJ3d4ThPe3e', 'RIDER', 5.00, '2026-01-01 08:40:00'),
  (6, 'rider_other', 'rider_other@test.com', '$2a$10$kCxHt4INvyMwg1xMUc8bqOVd6P1K8keABlGcNVvLUNIJ3d4ThPe3e', 'RIDER', 7.00, '2026-01-01 08:50:00');

INSERT INTO bikes (id, model, status, station_id) VALUES
  (1, 'Bike-A', 'AVAILABLE', 2),
  (2, 'Bike-B', 'BUSY', 2),
  (3, 'Bike-C', 'BOOKED', 3),
  (4, 'Bike-D', 'AVAILABLE', 1),
  (5, 'Bike-E', 'MAINTENANCE', 1),
  (6, 'Bike-F', 'AVAILABLE', 2),
  (7, 'Bike-G', 'BUSY', NULL),
  (8, 'Bike-H', 'BOOKED', 3),
  (9, 'Bike-I', 'AVAILABLE', 3);

INSERT INTO bookings (
  id,
  user_id,
  bike_id,
  pickup_station_id,
  dropoff_station_id,
  start_time,
  expiry_time,
  activated_at,
  returned_at,
  status
) VALUES
  (1, 4, 7, 2, NULL, '2026-01-10 09:00:00', NULL, '2026-01-10 09:02:00', NULL, 'ACTIVE'),
  (2, 5, 8, 3, NULL, '2026-01-10 09:05:00', '2030-01-10 09:20:00', NULL, NULL, 'PENDING'),
  (3, 2, 6, 2, 2, '2026-01-09 08:00:00', NULL, '2026-01-09 08:03:00', '2026-01-09 08:30:00', 'COMPLETED'),
  (4, 6, 9, 3, 3, '2026-01-08 07:00:00', NULL, '2026-01-08 07:02:00', '2026-01-08 07:40:00', 'COMPLETED');
