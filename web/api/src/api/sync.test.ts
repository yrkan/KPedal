import { describe, it, expect, beforeEach } from 'vitest';

/**
 * Unit tests for Sync API device verification
 *
 * Tests that sync endpoints properly verify device authorization
 * and return DEVICE_REVOKED when device is removed.
 */

// Mock types
interface Device {
  id: string;
  user_id: string;
  name: string;
}

interface RideData {
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
  // Extended metrics
  power_avg?: number;
  power_max?: number;
  cadence_avg?: number;
  hr_avg?: number;
  hr_max?: number;
  speed_avg?: number;
  distance_km?: number;
  // Pro cyclist metrics
  elevation_gain?: number;
  elevation_loss?: number;
  grade_avg?: number;
  grade_max?: number;
  normalized_power?: number;
  energy_kj?: number;
}

interface SyncRequest {
  userId: string;
  deviceId: string;
  rides?: RideData[];
}

// Simulated database
class MockDatabase {
  private devices: Map<string, Device> = new Map();

  addDevice(device: Device) {
    this.devices.set(device.id, device);
  }

  deleteDevice(deviceId: string, userId: string): boolean {
    const device = this.devices.get(deviceId);
    if (device && device.user_id === userId) {
      this.devices.delete(deviceId);
      return true;
    }
    return false;
  }

  findDevice(deviceId: string, userId: string): Device | null {
    const device = this.devices.get(deviceId);
    if (device && device.user_id === userId) {
      return device;
    }
    return null;
  }

  clear() {
    this.devices.clear();
  }
}

// Device verification result
interface VerifyResult {
  success: boolean;
  status: number;
  code?: string;
  error?: string;
}

// Verify device helper (matches API implementation)
function verifyDevice(db: MockDatabase, userId: string, deviceId: string): VerifyResult {
  if (!deviceId || deviceId.length < 8) {
    return {
      success: false,
      status: 400,
      error: 'Missing or invalid X-Device-ID header'
    };
  }

  const device = db.findDevice(deviceId, userId);
  if (!device) {
    return {
      success: false,
      status: 403,
      code: 'DEVICE_REVOKED',
      error: 'Device not found or access revoked'
    };
  }

  return { success: true, status: 200 };
}

// Simulated sync ride endpoint
function handleSyncRide(
  db: MockDatabase,
  request: SyncRequest
): VerifyResult & { data?: { ride_id: string } } {
  const verification = verifyDevice(db, request.userId, request.deviceId);
  if (!verification.success) {
    return verification;
  }

  // Simulate successful sync
  return {
    success: true,
    status: 200,
    data: { ride_id: 'new-ride-123' }
  };
}

// Simulated sync rides endpoint (batch)
function handleSyncRides(
  db: MockDatabase,
  request: SyncRequest
): VerifyResult & { data?: { inserted: number; duplicates: number } } {
  const verification = verifyDevice(db, request.userId, request.deviceId);
  if (!verification.success) {
    return verification;
  }

  const rideCount = request.rides?.length || 0;
  return {
    success: true,
    status: 200,
    data: { inserted: rideCount, duplicates: 0 }
  };
}

// Simulated check sync request endpoint
function handleCheckSyncRequest(
  db: MockDatabase,
  kv: Map<string, string>,
  request: { userId: string; deviceId: string }
): VerifyResult & { data?: { syncRequested: boolean } } {
  const verification = verifyDevice(db, request.userId, request.deviceId);
  if (!verification.success) {
    return verification;
  }

  const syncRequest = kv.get(`sync_request:${request.deviceId}`);
  return {
    success: true,
    status: 200,
    data: { syncRequested: !!syncRequest }
  };
}

describe('Sync Ride Endpoint', () => {
  let db: MockDatabase;

  beforeEach(() => {
    db = new MockDatabase();
  });

  it('should sync ride with valid device', () => {
    db.addDevice({ id: 'device-123', user_id: 'user-456', name: 'Karoo' });

    const result = handleSyncRide(db, {
      userId: 'user-456',
      deviceId: 'device-123'
    });

    expect(result.success).toBe(true);
    expect(result.status).toBe(200);
    expect(result.data?.ride_id).toBeDefined();
  });

  it('should fail with DEVICE_REVOKED for deleted device', () => {
    db.addDevice({ id: 'device-123', user_id: 'user-456', name: 'Karoo' });
    db.deleteDevice('device-123', 'user-456');

    const result = handleSyncRide(db, {
      userId: 'user-456',
      deviceId: 'device-123'
    });

    expect(result.success).toBe(false);
    expect(result.status).toBe(403);
    expect(result.code).toBe('DEVICE_REVOKED');
  });

  it('should fail with 400 for missing device ID', () => {
    const result = handleSyncRide(db, {
      userId: 'user-456',
      deviceId: ''
    });

    expect(result.success).toBe(false);
    expect(result.status).toBe(400);
  });

  it('should fail for device belonging to different user', () => {
    db.addDevice({ id: 'device-123', user_id: 'user-456', name: 'Karoo' });

    const result = handleSyncRide(db, {
      userId: 'user-999', // Different user
      deviceId: 'device-123'
    });

    expect(result.success).toBe(false);
    expect(result.status).toBe(403);
    expect(result.code).toBe('DEVICE_REVOKED');
  });
});

describe('Sync Rides Batch Endpoint', () => {
  let db: MockDatabase;

  beforeEach(() => {
    db = new MockDatabase();
  });

  it('should sync batch with valid device', () => {
    db.addDevice({ id: 'device-123', user_id: 'user-456', name: 'Karoo' });

    const result = handleSyncRides(db, {
      userId: 'user-456',
      deviceId: 'device-123',
      rides: [
        { timestamp: 1, duration_ms: 1000, balance_left: 50, balance_right: 50, te_left: 70, te_right: 70, ps_left: 20, ps_right: 20, zone_optimal: 80, zone_attention: 15, zone_problem: 5, score: 85 },
        { timestamp: 2, duration_ms: 2000, balance_left: 48, balance_right: 52, te_left: 72, te_right: 68, ps_left: 22, ps_right: 18, zone_optimal: 75, zone_attention: 20, zone_problem: 5, score: 80 }
      ]
    });

    expect(result.success).toBe(true);
    expect(result.data?.inserted).toBe(2);
  });

  it('should fail with DEVICE_REVOKED for deleted device', () => {
    db.addDevice({ id: 'device-123', user_id: 'user-456', name: 'Karoo' });
    db.deleteDevice('device-123', 'user-456');

    const result = handleSyncRides(db, {
      userId: 'user-456',
      deviceId: 'device-123',
      rides: []
    });

    expect(result.success).toBe(false);
    expect(result.code).toBe('DEVICE_REVOKED');
  });
});

describe('Check Sync Request Endpoint', () => {
  let db: MockDatabase;
  let kv: Map<string, string>;

  beforeEach(() => {
    db = new MockDatabase();
    kv = new Map();
  });

  it('should check sync request with valid device', () => {
    db.addDevice({ id: 'device-123', user_id: 'user-456', name: 'Karoo' });

    const result = handleCheckSyncRequest(db, kv, {
      userId: 'user-456',
      deviceId: 'device-123'
    });

    expect(result.success).toBe(true);
    expect(result.data?.syncRequested).toBe(false);
  });

  it('should return syncRequested true when request exists', () => {
    db.addDevice({ id: 'device-123', user_id: 'user-456', name: 'Karoo' });
    kv.set('sync_request:device-123', JSON.stringify({ requestedAt: Date.now() }));

    const result = handleCheckSyncRequest(db, kv, {
      userId: 'user-456',
      deviceId: 'device-123'
    });

    expect(result.success).toBe(true);
    expect(result.data?.syncRequested).toBe(true);
  });

  it('should fail with DEVICE_REVOKED for deleted device', () => {
    db.addDevice({ id: 'device-123', user_id: 'user-456', name: 'Karoo' });
    kv.set('sync_request:device-123', JSON.stringify({ requestedAt: Date.now() }));

    // Delete device
    db.deleteDevice('device-123', 'user-456');

    const result = handleCheckSyncRequest(db, kv, {
      userId: 'user-456',
      deviceId: 'device-123'
    });

    // Should fail even though sync request exists
    expect(result.success).toBe(false);
    expect(result.code).toBe('DEVICE_REVOKED');
  });
});

describe('Device ID Validation', () => {
  let db: MockDatabase;

  beforeEach(() => {
    db = new MockDatabase();
  });

  it('should reject empty device ID', () => {
    const result = verifyDevice(db, 'user-456', '');
    expect(result.success).toBe(false);
    expect(result.status).toBe(400);
  });

  it('should reject short device ID', () => {
    const result = verifyDevice(db, 'user-456', 'abc');
    expect(result.success).toBe(false);
    expect(result.status).toBe(400);
  });

  it('should accept valid UUID device ID', () => {
    const uuid = 'f47ac10b-58cc-4372-a567-0e02b2c3d479';
    db.addDevice({ id: uuid, user_id: 'user-456', name: 'Karoo' });

    const result = verifyDevice(db, 'user-456', uuid);
    expect(result.success).toBe(true);
  });
});

describe('Sync Flow with Device Lifecycle', () => {
  let db: MockDatabase;
  let kv: Map<string, string>;

  beforeEach(() => {
    db = new MockDatabase();
    kv = new Map();
  });

  it('should handle complete device lifecycle', () => {
    const userId = 'user-456';
    const deviceId = 'device-f47ac10b';

    // 1. Device linked
    db.addDevice({ id: deviceId, user_id: userId, name: 'Karoo' });

    // 2. Syncs work
    expect(handleSyncRide(db, { userId, deviceId }).success).toBe(true);
    expect(handleSyncRides(db, { userId, deviceId, rides: [] }).success).toBe(true);
    expect(handleCheckSyncRequest(db, kv, { userId, deviceId }).success).toBe(true);

    // 3. User requests sync from web
    kv.set(`sync_request:${deviceId}`, JSON.stringify({ requestedAt: Date.now() }));
    expect(handleCheckSyncRequest(db, kv, { userId, deviceId }).data?.syncRequested).toBe(true);

    // 4. User removes device from web
    db.deleteDevice(deviceId, userId);

    // 5. All endpoints fail with DEVICE_REVOKED
    expect(handleSyncRide(db, { userId, deviceId }).code).toBe('DEVICE_REVOKED');
    expect(handleSyncRides(db, { userId, deviceId, rides: [] }).code).toBe('DEVICE_REVOKED');
    expect(handleCheckSyncRequest(db, kv, { userId, deviceId }).code).toBe('DEVICE_REVOKED');
  });

  it('should allow re-linking same device', () => {
    const userId = 'user-456';
    const deviceId = 'device-f47ac10b';

    // 1. Device linked
    db.addDevice({ id: deviceId, user_id: userId, name: 'Karoo' });
    expect(handleSyncRide(db, { userId, deviceId }).success).toBe(true);

    // 2. Device removed
    db.deleteDevice(deviceId, userId);
    expect(handleSyncRide(db, { userId, deviceId }).code).toBe('DEVICE_REVOKED');

    // 3. Device re-linked
    db.addDevice({ id: deviceId, user_id: userId, name: 'Karoo (Re-linked)' });
    expect(handleSyncRide(db, { userId, deviceId }).success).toBe(true);
  });
});

/**
 * Tests for Batch Sync Logic (N+1 optimization)
 * These tests verify the batch duplicate detection and insert logic
 */

// Mock rides database
class MockRidesDatabase {
  private rides: Map<string, RideData & { user_id: string; device_id: string }> = new Map();

  // Key format: user_id:device_id:timestamp
  private makeKey(userId: string, deviceId: string, timestamp: number): string {
    return `${userId}:${deviceId}:${timestamp}`;
  }

  addRide(userId: string, deviceId: string, ride: RideData) {
    const key = this.makeKey(userId, deviceId, ride.timestamp);
    this.rides.set(key, { ...ride, user_id: userId, device_id: deviceId });
  }

  findExistingTimestamps(userId: string, deviceId: string, timestamps: number[]): number[] {
    const existing: number[] = [];
    for (const ts of timestamps) {
      const key = this.makeKey(userId, deviceId, ts);
      if (this.rides.has(key)) {
        existing.push(ts);
      }
    }
    return existing;
  }

  getRideCount(): number {
    return this.rides.size;
  }

  clear() {
    this.rides.clear();
  }
}

// Validation function matching API
function validateRideData(ride: RideData): { valid: boolean; errors: string[] } {
  const errors: string[] = [];

  if (!ride.timestamp || ride.timestamp <= 0) {
    errors.push('Invalid timestamp');
  }
  if (!ride.duration_ms || ride.duration_ms < 0) {
    errors.push('Invalid duration');
  }
  if (ride.balance_left < 0 || ride.balance_left > 100) {
    errors.push('Invalid balance_left');
  }

  return { valid: errors.length === 0, errors };
}

// Batch sync handler (matches optimized API implementation)
function handleBatchSync(
  ridesDb: MockRidesDatabase,
  userId: string,
  deviceId: string,
  rides: RideData[]
): { inserted: number; duplicates: number; invalid: number; total: number } {
  // 1. Validate all rides first
  const validRides: RideData[] = [];
  let invalid = 0;

  for (const ride of rides) {
    const validation = validateRideData(ride);
    if (!validation.valid) {
      invalid++;
      continue;
    }
    validRides.push(ride);
  }

  if (validRides.length === 0) {
    return { inserted: 0, duplicates: 0, invalid, total: rides.length };
  }

  // 2. Batch check for duplicates (ONE query instead of N)
  const timestamps = validRides.map(r => r.timestamp);
  const existingTimestamps = new Set(
    ridesDb.findExistingTimestamps(userId, deviceId, timestamps)
  );

  // 3. Filter out duplicates
  const newRides = validRides.filter(r => !existingTimestamps.has(r.timestamp));
  const duplicates = validRides.length - newRides.length;

  // 4. Batch insert
  for (const ride of newRides) {
    ridesDb.addRide(userId, deviceId, ride);
  }

  return {
    inserted: newRides.length,
    duplicates,
    invalid,
    total: rides.length
  };
}

describe('Batch Sync Duplicate Detection', () => {
  let ridesDb: MockRidesDatabase;

  beforeEach(() => {
    ridesDb = new MockRidesDatabase();
  });

  const createRide = (timestamp: number): RideData => ({
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
    // Extended metrics
    power_avg: 185,
    power_max: 420,
    cadence_avg: 88,
    hr_avg: 145,
    hr_max: 172,
    speed_avg: 28.5,
    distance_km: 42.3,
    // Pro cyclist metrics
    elevation_gain: 650,
    elevation_loss: 620,
    grade_avg: 3.2,
    grade_max: 12.5,
    normalized_power: 205,
    energy_kj: 1850
  });

  it('should insert all new rides', () => {
    const rides = [createRide(1000), createRide(2000), createRide(3000)];

    const result = handleBatchSync(ridesDb, 'user-1', 'device-1', rides);

    expect(result.inserted).toBe(3);
    expect(result.duplicates).toBe(0);
    expect(result.invalid).toBe(0);
    expect(ridesDb.getRideCount()).toBe(3);
  });

  it('should detect all duplicates', () => {
    // Pre-existing rides
    ridesDb.addRide('user-1', 'device-1', createRide(1000));
    ridesDb.addRide('user-1', 'device-1', createRide(2000));

    // Try to sync same rides again
    const rides = [createRide(1000), createRide(2000)];

    const result = handleBatchSync(ridesDb, 'user-1', 'device-1', rides);

    expect(result.inserted).toBe(0);
    expect(result.duplicates).toBe(2);
    expect(ridesDb.getRideCount()).toBe(2); // No new rides added
  });

  it('should handle mixed new and duplicate rides', () => {
    // Pre-existing ride
    ridesDb.addRide('user-1', 'device-1', createRide(1000));

    // Mix of new and duplicate
    const rides = [createRide(1000), createRide(2000), createRide(3000)];

    const result = handleBatchSync(ridesDb, 'user-1', 'device-1', rides);

    expect(result.inserted).toBe(2); // 2000 and 3000
    expect(result.duplicates).toBe(1); // 1000
    expect(ridesDb.getRideCount()).toBe(3);
  });

  it('should skip invalid rides', () => {
    const validRide = createRide(1000);
    const invalidRide = { ...createRide(2000), timestamp: -1 }; // Invalid timestamp

    const result = handleBatchSync(ridesDb, 'user-1', 'device-1', [validRide, invalidRide]);

    expect(result.inserted).toBe(1);
    expect(result.invalid).toBe(1);
    expect(ridesDb.getRideCount()).toBe(1);
  });

  it('should handle all invalid rides', () => {
    const invalid1 = { ...createRide(1000), timestamp: 0 };
    const invalid2 = { ...createRide(2000), balance_left: 150 };

    const result = handleBatchSync(ridesDb, 'user-1', 'device-1', [invalid1, invalid2]);

    expect(result.inserted).toBe(0);
    expect(result.invalid).toBe(2);
    expect(ridesDb.getRideCount()).toBe(0);
  });

  it('should not affect other devices rides', () => {
    // Device 1 has a ride
    ridesDb.addRide('user-1', 'device-1', createRide(1000));

    // Device 2 syncs same timestamp (not a duplicate!)
    const result = handleBatchSync(ridesDb, 'user-1', 'device-2', [createRide(1000)]);

    expect(result.inserted).toBe(1);
    expect(result.duplicates).toBe(0);
    expect(ridesDb.getRideCount()).toBe(2);
  });

  it('should handle empty batch', () => {
    const result = handleBatchSync(ridesDb, 'user-1', 'device-1', []);

    expect(result.inserted).toBe(0);
    expect(result.duplicates).toBe(0);
    expect(result.invalid).toBe(0);
    expect(result.total).toBe(0);
  });

  it('should correctly count totals', () => {
    ridesDb.addRide('user-1', 'device-1', createRide(1000));

    const rides = [
      createRide(1000),  // duplicate
      createRide(2000),  // new
      { ...createRide(3000), timestamp: -1 },  // invalid
      createRide(4000),  // new
    ];

    const result = handleBatchSync(ridesDb, 'user-1', 'device-1', rides);

    expect(result.total).toBe(4);
    expect(result.inserted).toBe(2);
    expect(result.duplicates).toBe(1);
    expect(result.invalid).toBe(1);
    expect(result.inserted + result.duplicates + result.invalid).toBe(result.total);
  });
});
