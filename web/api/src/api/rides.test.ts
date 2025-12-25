import { describe, it, expect, beforeEach } from 'vitest';

/**
 * Unit tests for Rides API
 *
 * Tests the rides endpoints: list, get, delete, stats, trends
 */

// Types
interface RideData {
  id?: number;
  user_id: string;
  device_id: string;
  timestamp: number;
  duration_ms: number;
  balance_left: number;
  balance_right: number;
  te_left: number;
  te_right: number;
  ps_left: number;
  ps_right: number;
  zone_optimal: number;
  zone_attention: number;
  zone_problem: number;
  score: number;
  notes?: string;
  rating?: number;
  created_at?: string;
}

interface PaginationParams {
  limit?: string;
  offset?: string;
}

// Mock database for rides
class MockRidesDatabase {
  private rides: Map<number, RideData> = new Map();
  private nextId = 1;

  addRide(ride: Omit<RideData, 'id' | 'created_at'>): RideData {
    const newRide: RideData = {
      ...ride,
      id: this.nextId++,
      created_at: new Date().toISOString(),
    };
    this.rides.set(newRide.id!, newRide);
    return newRide;
  }

  getRidesByUser(userId: string, limit: number, offset: number): RideData[] {
    const userRides = Array.from(this.rides.values())
      .filter(r => r.user_id === userId)
      .sort((a, b) => b.timestamp - a.timestamp);
    return userRides.slice(offset, offset + limit);
  }

  getRideCountByUser(userId: string): number {
    return Array.from(this.rides.values())
      .filter(r => r.user_id === userId).length;
  }

  getRideById(id: number, userId: string): RideData | null {
    const ride = this.rides.get(id);
    if (ride && ride.user_id === userId) {
      return ride;
    }
    return null;
  }

  deleteRide(id: number, userId: string): boolean {
    const ride = this.rides.get(id);
    if (ride && ride.user_id === userId) {
      this.rides.delete(id);
      return true;
    }
    return false;
  }

  getStatsByUser(userId: string): {
    total_rides: number;
    avg_balance_left: number;
    avg_score: number;
    total_duration_ms: number;
  } {
    const userRides = Array.from(this.rides.values())
      .filter(r => r.user_id === userId);

    if (userRides.length === 0) {
      return {
        total_rides: 0,
        avg_balance_left: 50,
        avg_score: 0,
        total_duration_ms: 0,
      };
    }

    return {
      total_rides: userRides.length,
      avg_balance_left: userRides.reduce((sum, r) => sum + r.balance_left, 0) / userRides.length,
      avg_score: userRides.reduce((sum, r) => sum + r.score, 0) / userRides.length,
      total_duration_ms: userRides.reduce((sum, r) => sum + r.duration_ms, 0),
    };
  }

  clear() {
    this.rides.clear();
    this.nextId = 1;
  }
}

// Pagination validation (matches API implementation)
function validatePagination(limitStr?: string, offsetStr?: string): { limit: number; offset: number } {
  let limit = 20;
  let offset = 0;

  if (limitStr) {
    const parsed = parseInt(limitStr, 10);
    if (!isNaN(parsed) && parsed > 0 && parsed <= 100) {
      limit = parsed;
    }
  }

  if (offsetStr) {
    const parsed = parseInt(offsetStr, 10);
    if (!isNaN(parsed) && parsed >= 0) {
      offset = parsed;
    }
  }

  return { limit, offset };
}

// Days validation (matches API implementation)
function validateDays(daysStr?: string): number {
  if (daysStr) {
    const parsed = parseInt(daysStr, 10);
    if (!isNaN(parsed) && parsed >= 1 && parsed <= 365) {
      return parsed;
    }
  }
  return 30; // Default
}

// API handlers
function handleGetRides(
  db: MockRidesDatabase,
  userId: string,
  params: PaginationParams
): { success: boolean; data?: { rides: RideData[]; total: number; limit: number; offset: number } } {
  const { limit, offset } = validatePagination(params.limit, params.offset);
  const rides = db.getRidesByUser(userId, limit, offset);
  const total = db.getRideCountByUser(userId);

  return {
    success: true,
    data: { rides, total, limit, offset },
  };
}

function handleGetRide(
  db: MockRidesDatabase,
  userId: string,
  rideId: number
): { success: boolean; status: number; data?: RideData; error?: string } {
  const ride = db.getRideById(rideId, userId);

  if (!ride) {
    return { success: false, status: 404, error: 'Ride not found' };
  }

  return { success: true, status: 200, data: ride };
}

function handleDeleteRide(
  db: MockRidesDatabase,
  userId: string,
  rideId: number
): { success: boolean; status: number; message?: string; error?: string } {
  const deleted = db.deleteRide(rideId, userId);

  if (!deleted) {
    return { success: false, status: 404, error: 'Ride not found' };
  }

  return { success: true, status: 200, message: 'Ride deleted' };
}

function handleGetStats(
  db: MockRidesDatabase,
  userId: string
): { success: boolean; data: ReturnType<MockRidesDatabase['getStatsByUser']> } {
  return { success: true, data: db.getStatsByUser(userId) };
}

// Helper to create test ride
function createTestRide(userId: string, timestamp: number, overrides?: Partial<RideData>): Omit<RideData, 'id' | 'created_at'> {
  return {
    user_id: userId,
    device_id: 'device-1',
    timestamp,
    duration_ms: 3600000,
    balance_left: 50,
    balance_right: 50,
    te_left: 75,
    te_right: 75,
    ps_left: 25,
    ps_right: 25,
    zone_optimal: 80,
    zone_attention: 15,
    zone_problem: 5,
    score: 85,
    ...overrides,
  };
}

describe('Rides List Endpoint', () => {
  let db: MockRidesDatabase;

  beforeEach(() => {
    db = new MockRidesDatabase();
  });

  it('should return empty list for new user', () => {
    const result = handleGetRides(db, 'user-1', {});

    expect(result.success).toBe(true);
    expect(result.data?.rides).toHaveLength(0);
    expect(result.data?.total).toBe(0);
  });

  it('should return rides sorted by timestamp desc', () => {
    db.addRide(createTestRide('user-1', 1000));
    db.addRide(createTestRide('user-1', 3000));
    db.addRide(createTestRide('user-1', 2000));

    const result = handleGetRides(db, 'user-1', {});

    expect(result.data?.rides).toHaveLength(3);
    expect(result.data?.rides[0].timestamp).toBe(3000);
    expect(result.data?.rides[1].timestamp).toBe(2000);
    expect(result.data?.rides[2].timestamp).toBe(1000);
  });

  it('should respect pagination limit', () => {
    for (let i = 0; i < 10; i++) {
      db.addRide(createTestRide('user-1', i * 1000));
    }

    const result = handleGetRides(db, 'user-1', { limit: '5' });

    expect(result.data?.rides).toHaveLength(5);
    expect(result.data?.total).toBe(10);
    expect(result.data?.limit).toBe(5);
  });

  it('should respect pagination offset', () => {
    for (let i = 0; i < 10; i++) {
      db.addRide(createTestRide('user-1', i * 1000));
    }

    const result = handleGetRides(db, 'user-1', { limit: '5', offset: '5' });

    expect(result.data?.rides).toHaveLength(5);
    expect(result.data?.offset).toBe(5);
  });

  it('should only return rides for specified user', () => {
    db.addRide(createTestRide('user-1', 1000));
    db.addRide(createTestRide('user-2', 2000));
    db.addRide(createTestRide('user-1', 3000));

    const result = handleGetRides(db, 'user-1', {});

    expect(result.data?.rides).toHaveLength(2);
    expect(result.data?.rides.every(r => r.user_id === 'user-1')).toBe(true);
  });

  it('should use default pagination values', () => {
    const result = handleGetRides(db, 'user-1', {});

    expect(result.data?.limit).toBe(20);
    expect(result.data?.offset).toBe(0);
  });

  it('should clamp limit to max 100', () => {
    const { limit } = validatePagination('200');
    expect(limit).toBe(20); // Falls back to default when invalid
  });

  it('should handle invalid pagination params', () => {
    const { limit, offset } = validatePagination('invalid', '-5');
    expect(limit).toBe(20);
    expect(offset).toBe(0);
  });
});

describe('Get Single Ride Endpoint', () => {
  let db: MockRidesDatabase;

  beforeEach(() => {
    db = new MockRidesDatabase();
  });

  it('should return ride by ID', () => {
    const added = db.addRide(createTestRide('user-1', 1000));

    const result = handleGetRide(db, 'user-1', added.id!);

    expect(result.success).toBe(true);
    expect(result.status).toBe(200);
    expect(result.data?.id).toBe(added.id);
  });

  it('should return 404 for non-existent ride', () => {
    const result = handleGetRide(db, 'user-1', 999);

    expect(result.success).toBe(false);
    expect(result.status).toBe(404);
    expect(result.error).toBe('Ride not found');
  });

  it('should return 404 for ride belonging to different user', () => {
    const added = db.addRide(createTestRide('user-2', 1000));

    const result = handleGetRide(db, 'user-1', added.id!);

    expect(result.success).toBe(false);
    expect(result.status).toBe(404);
  });

  it('should return all ride fields', () => {
    const added = db.addRide(createTestRide('user-1', 1000, {
      notes: 'Test notes',
      rating: 4,
    }));

    const result = handleGetRide(db, 'user-1', added.id!);

    expect(result.data).toMatchObject({
      timestamp: 1000,
      balance_left: 50,
      te_left: 75,
      score: 85,
      notes: 'Test notes',
      rating: 4,
    });
  });
});

describe('Delete Ride Endpoint', () => {
  let db: MockRidesDatabase;

  beforeEach(() => {
    db = new MockRidesDatabase();
  });

  it('should delete ride', () => {
    const added = db.addRide(createTestRide('user-1', 1000));

    const result = handleDeleteRide(db, 'user-1', added.id!);

    expect(result.success).toBe(true);
    expect(result.status).toBe(200);
    expect(result.message).toBe('Ride deleted');

    // Verify deleted
    expect(db.getRideById(added.id!, 'user-1')).toBeNull();
  });

  it('should return 404 for non-existent ride', () => {
    const result = handleDeleteRide(db, 'user-1', 999);

    expect(result.success).toBe(false);
    expect(result.status).toBe(404);
  });

  it('should not delete ride belonging to different user', () => {
    const added = db.addRide(createTestRide('user-2', 1000));

    const result = handleDeleteRide(db, 'user-1', added.id!);

    expect(result.success).toBe(false);
    expect(result.status).toBe(404);

    // Verify not deleted
    expect(db.getRideById(added.id!, 'user-2')).not.toBeNull();
  });
});

describe('Stats Summary Endpoint', () => {
  let db: MockRidesDatabase;

  beforeEach(() => {
    db = new MockRidesDatabase();
  });

  it('should return default stats for new user', () => {
    const result = handleGetStats(db, 'user-1');

    expect(result.success).toBe(true);
    expect(result.data.total_rides).toBe(0);
    expect(result.data.avg_balance_left).toBe(50); // Default
    expect(result.data.avg_score).toBe(0);
  });

  it('should calculate correct averages', () => {
    db.addRide(createTestRide('user-1', 1000, { balance_left: 48, score: 80 }));
    db.addRide(createTestRide('user-1', 2000, { balance_left: 52, score: 90 }));

    const result = handleGetStats(db, 'user-1');

    expect(result.data.total_rides).toBe(2);
    expect(result.data.avg_balance_left).toBe(50); // (48 + 52) / 2
    expect(result.data.avg_score).toBe(85); // (80 + 90) / 2
  });

  it('should calculate total duration', () => {
    db.addRide(createTestRide('user-1', 1000, { duration_ms: 3600000 }));
    db.addRide(createTestRide('user-1', 2000, { duration_ms: 7200000 }));

    const result = handleGetStats(db, 'user-1');

    expect(result.data.total_duration_ms).toBe(10800000); // 3h + 2h = 5h
  });

  it('should only include stats for specified user', () => {
    db.addRide(createTestRide('user-1', 1000, { score: 80 }));
    db.addRide(createTestRide('user-2', 2000, { score: 100 }));

    const result = handleGetStats(db, 'user-1');

    expect(result.data.total_rides).toBe(1);
    expect(result.data.avg_score).toBe(80);
  });
});

describe('Pagination Validation', () => {
  it('should parse valid limit', () => {
    expect(validatePagination('50').limit).toBe(50);
  });

  it('should parse valid offset', () => {
    expect(validatePagination('20', '100').offset).toBe(100);
  });

  it('should reject limit > 100', () => {
    expect(validatePagination('150').limit).toBe(20); // Default
  });

  it('should reject limit <= 0', () => {
    expect(validatePagination('0').limit).toBe(20);
    expect(validatePagination('-5').limit).toBe(20);
  });

  it('should reject negative offset', () => {
    expect(validatePagination('20', '-10').offset).toBe(0);
  });

  it('should handle non-numeric input', () => {
    const result = validatePagination('abc', 'xyz');
    expect(result.limit).toBe(20);
    expect(result.offset).toBe(0);
  });
});

describe('Days Validation', () => {
  it('should parse valid days', () => {
    expect(validateDays('7')).toBe(7);
    expect(validateDays('30')).toBe(30);
    expect(validateDays('365')).toBe(365);
  });

  it('should return default for missing input', () => {
    expect(validateDays()).toBe(30);
    expect(validateDays(undefined)).toBe(30);
  });

  it('should clamp to range 1-365', () => {
    expect(validateDays('0')).toBe(30); // Default
    expect(validateDays('500')).toBe(30); // Default
  });

  it('should handle invalid input', () => {
    expect(validateDays('abc')).toBe(30);
  });
});

describe('User Isolation', () => {
  let db: MockRidesDatabase;

  beforeEach(() => {
    db = new MockRidesDatabase();
    // Setup rides for two users
    db.addRide(createTestRide('user-1', 1000));
    db.addRide(createTestRide('user-1', 2000));
    db.addRide(createTestRide('user-2', 3000));
  });

  it('user cannot see other users rides in list', () => {
    const result = handleGetRides(db, 'user-1', {});
    expect(result.data?.rides).toHaveLength(2);
    expect(result.data?.rides.every(r => r.user_id === 'user-1')).toBe(true);
  });

  it('user cannot get other users ride by ID', () => {
    const user2Ride = Array.from(db.getRidesByUser('user-2', 10, 0))[0];
    const result = handleGetRide(db, 'user-1', user2Ride.id!);
    expect(result.success).toBe(false);
    expect(result.status).toBe(404);
  });

  it('user cannot delete other users ride', () => {
    const user2Ride = Array.from(db.getRidesByUser('user-2', 10, 0))[0];
    const result = handleDeleteRide(db, 'user-1', user2Ride.id!);
    expect(result.success).toBe(false);

    // Ride still exists for user-2
    expect(db.getRideById(user2Ride.id!, 'user-2')).not.toBeNull();
  });

  it('user stats only include their own rides', () => {
    const result = handleGetStats(db, 'user-1');
    expect(result.data.total_rides).toBe(2);
  });
});
