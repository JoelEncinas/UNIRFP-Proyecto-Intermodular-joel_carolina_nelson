import { calculateDistanceMeters } from './distance';

describe('calculateDistanceMeters', () => {
  it('returns 0 for identical coordinates', () => {
    const point = { latitude: 40.4168, longitude: -3.7038 };

    expect(calculateDistanceMeters(point, point)).toBe(0);
  });

  it('returns a positive distance for different coordinates', () => {
    const from = { latitude: 40.4168, longitude: -3.7038 };
    const to = { latitude: 40.418, longitude: -3.7038 };

    const distance = calculateDistanceMeters(from, to);

    expect(distance).toBeGreaterThan(0);
    expect(distance).toBeLessThan(200);
  });
});
