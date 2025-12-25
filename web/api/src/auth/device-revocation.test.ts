import { describe, it, expect, vi, beforeEach } from 'vitest';

/**
 * Unit tests for Device Revocation Flow
 *
 * Tests the security flow where:
 * 1. Device is deleted from web settings
 * 2. App's next request returns DEVICE_REVOKED
 * 3. App logs out automatically
 */

// Mock types matching the API
interface Device {
  id: string;
  user_id: string;
  name: string;
  type: string;
  last_sync: string | null;
  created_at: string;
}

interface RefreshTokenData {
  createdAt: number;
  deviceId?: string;
  deviceName?: string;
}

// Simulated database for testing
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

// Simulated KV store for tokens
class MockKVStore {
  private store: Map<string, string> = new Map();

  put(key: string, value: string) {
    this.store.set(key, value);
  }

  get(key: string): string | null {
    return this.store.get(key) || null;
  }

  delete(key: string) {
    this.store.delete(key);
  }

  clear() {
    this.store.clear();
  }
}

// Helper function: verify device exists (matches API implementation)
function verifyDevice(
  db: MockDatabase,
  userId: string,
  deviceId: string
): { valid: boolean; code?: string } {
  const device = db.findDevice(deviceId, userId);
  if (!device) {
    return { valid: false, code: 'DEVICE_REVOKED' };
  }
  return { valid: true };
}

// Helper function: handle refresh with device verification
function handleRefresh(
  db: MockDatabase,
  kv: MockKVStore,
  userId: string,
  refreshTokenKey: string,
  deviceIdHeader?: string
): { success: boolean; code?: string; error?: string } {
  // Check if token exists in KV
  const storedTokenStr = kv.get(refreshTokenKey);
  if (!storedTokenStr) {
    return { success: false, error: 'Token revoked' };
  }

  const storedToken: RefreshTokenData = JSON.parse(storedTokenStr);

  // Use device ID from header or stored token
  const effectiveDeviceId = deviceIdHeader || storedToken.deviceId;

  if (effectiveDeviceId) {
    const verification = verifyDevice(db, userId, effectiveDeviceId);
    if (!verification.valid) {
      // Revoke the token too
      kv.delete(refreshTokenKey);
      return { success: false, code: verification.code, error: 'Device not found or access revoked' };
    }
  }

  return { success: true };
}

describe('Device Verification', () => {
  let db: MockDatabase;

  beforeEach(() => {
    db = new MockDatabase();
  });

  it('should verify existing device belongs to user', () => {
    db.addDevice({
      id: 'device-123',
      user_id: 'user-456',
      name: 'Karoo',
      type: 'karoo',
      last_sync: null,
      created_at: new Date().toISOString()
    });

    const result = verifyDevice(db, 'user-456', 'device-123');
    expect(result.valid).toBe(true);
    expect(result.code).toBeUndefined();
  });

  it('should fail for non-existent device', () => {
    const result = verifyDevice(db, 'user-456', 'non-existent');
    expect(result.valid).toBe(false);
    expect(result.code).toBe('DEVICE_REVOKED');
  });

  it('should fail for device belonging to different user', () => {
    db.addDevice({
      id: 'device-123',
      user_id: 'user-456',
      name: 'Karoo',
      type: 'karoo',
      last_sync: null,
      created_at: new Date().toISOString()
    });

    // Different user trying to access
    const result = verifyDevice(db, 'user-999', 'device-123');
    expect(result.valid).toBe(false);
    expect(result.code).toBe('DEVICE_REVOKED');
  });

  it('should fail after device is deleted', () => {
    db.addDevice({
      id: 'device-123',
      user_id: 'user-456',
      name: 'Karoo',
      type: 'karoo',
      last_sync: null,
      created_at: new Date().toISOString()
    });

    // Verify it works first
    expect(verifyDevice(db, 'user-456', 'device-123').valid).toBe(true);

    // Delete the device
    db.deleteDevice('device-123', 'user-456');

    // Now it should fail
    const result = verifyDevice(db, 'user-456', 'device-123');
    expect(result.valid).toBe(false);
    expect(result.code).toBe('DEVICE_REVOKED');
  });
});

describe('Refresh Token with Device Verification', () => {
  let db: MockDatabase;
  let kv: MockKVStore;

  beforeEach(() => {
    db = new MockDatabase();
    kv = new MockKVStore();
  });

  it('should succeed with valid device', () => {
    db.addDevice({
      id: 'device-123',
      user_id: 'user-456',
      name: 'Karoo',
      type: 'karoo',
      last_sync: null,
      created_at: new Date().toISOString()
    });

    kv.put('refresh:user-456:token123', JSON.stringify({
      createdAt: Date.now(),
      deviceId: 'device-123',
      deviceName: 'Karoo'
    }));

    const result = handleRefresh(db, kv, 'user-456', 'refresh:user-456:token123');
    expect(result.success).toBe(true);
  });

  it('should fail with DEVICE_REVOKED after device deletion', () => {
    db.addDevice({
      id: 'device-123',
      user_id: 'user-456',
      name: 'Karoo',
      type: 'karoo',
      last_sync: null,
      created_at: new Date().toISOString()
    });

    kv.put('refresh:user-456:token123', JSON.stringify({
      createdAt: Date.now(),
      deviceId: 'device-123',
      deviceName: 'Karoo'
    }));

    // Delete device (simulating user removing from web)
    db.deleteDevice('device-123', 'user-456');

    const result = handleRefresh(db, kv, 'user-456', 'refresh:user-456:token123');
    expect(result.success).toBe(false);
    expect(result.code).toBe('DEVICE_REVOKED');
  });

  it('should also revoke refresh token when device is revoked', () => {
    db.addDevice({
      id: 'device-123',
      user_id: 'user-456',
      name: 'Karoo',
      type: 'karoo',
      last_sync: null,
      created_at: new Date().toISOString()
    });

    kv.put('refresh:user-456:token123', JSON.stringify({
      createdAt: Date.now(),
      deviceId: 'device-123',
      deviceName: 'Karoo'
    }));

    // Delete device
    db.deleteDevice('device-123', 'user-456');

    // First refresh fails
    handleRefresh(db, kv, 'user-456', 'refresh:user-456:token123');

    // Token should be deleted from KV
    expect(kv.get('refresh:user-456:token123')).toBeNull();
  });

  it('should use device ID from header over stored', () => {
    // Add two devices
    db.addDevice({
      id: 'device-old',
      user_id: 'user-456',
      name: 'Old Karoo',
      type: 'karoo',
      last_sync: null,
      created_at: new Date().toISOString()
    });
    db.addDevice({
      id: 'device-new',
      user_id: 'user-456',
      name: 'New Karoo',
      type: 'karoo',
      last_sync: null,
      created_at: new Date().toISOString()
    });

    // Token stored with old device
    kv.put('refresh:user-456:token123', JSON.stringify({
      createdAt: Date.now(),
      deviceId: 'device-old',
      deviceName: 'Old Karoo'
    }));

    // Delete old device
    db.deleteDevice('device-old', 'user-456');

    // Refresh with new device in header should still work
    const result = handleRefresh(db, kv, 'user-456', 'refresh:user-456:token123', 'device-new');
    expect(result.success).toBe(true);
  });

  it('should fail for revoked refresh token', () => {
    db.addDevice({
      id: 'device-123',
      user_id: 'user-456',
      name: 'Karoo',
      type: 'karoo',
      last_sync: null,
      created_at: new Date().toISOString()
    });

    // Token not in KV (revoked)
    const result = handleRefresh(db, kv, 'user-456', 'refresh:user-456:token123');
    expect(result.success).toBe(false);
    expect(result.error).toBe('Token revoked');
  });
});

describe('Complete Revocation Flow', () => {
  let db: MockDatabase;
  let kv: MockKVStore;

  beforeEach(() => {
    db = new MockDatabase();
    kv = new MockKVStore();
  });

  it('should handle full revocation scenario', () => {
    // 1. User links device
    db.addDevice({
      id: 'device-123',
      user_id: 'user-456',
      name: 'Karoo',
      type: 'karoo',
      last_sync: null,
      created_at: new Date().toISOString()
    });

    kv.put('refresh:user-456:token123', JSON.stringify({
      createdAt: Date.now(),
      deviceId: 'device-123',
      deviceName: 'Karoo'
    }));

    // 2. Sync works
    expect(verifyDevice(db, 'user-456', 'device-123').valid).toBe(true);
    expect(handleRefresh(db, kv, 'user-456', 'refresh:user-456:token123').success).toBe(true);

    // 3. User removes device from web
    db.deleteDevice('device-123', 'user-456');

    // 4. Next sync fails with DEVICE_REVOKED
    const syncResult = verifyDevice(db, 'user-456', 'device-123');
    expect(syncResult.valid).toBe(false);
    expect(syncResult.code).toBe('DEVICE_REVOKED');

    // 5. Refresh also fails with DEVICE_REVOKED
    const refreshResult = handleRefresh(db, kv, 'user-456', 'refresh:user-456:token123');
    expect(refreshResult.success).toBe(false);
    expect(refreshResult.code).toBe('DEVICE_REVOKED');

    // 6. Token is also revoked
    expect(kv.get('refresh:user-456:token123')).toBeNull();
  });

  it('should not affect other users devices', () => {
    // Two users with devices
    db.addDevice({
      id: 'device-user1',
      user_id: 'user-1',
      name: 'User1 Karoo',
      type: 'karoo',
      last_sync: null,
      created_at: new Date().toISOString()
    });
    db.addDevice({
      id: 'device-user2',
      user_id: 'user-2',
      name: 'User2 Karoo',
      type: 'karoo',
      last_sync: null,
      created_at: new Date().toISOString()
    });

    kv.put('refresh:user-1:token1', JSON.stringify({
      createdAt: Date.now(),
      deviceId: 'device-user1'
    }));
    kv.put('refresh:user-2:token2', JSON.stringify({
      createdAt: Date.now(),
      deviceId: 'device-user2'
    }));

    // User 1 deletes their device
    db.deleteDevice('device-user1', 'user-1');

    // User 1 is revoked
    expect(verifyDevice(db, 'user-1', 'device-user1').valid).toBe(false);
    expect(handleRefresh(db, kv, 'user-1', 'refresh:user-1:token1').success).toBe(false);

    // User 2 is NOT affected
    expect(verifyDevice(db, 'user-2', 'device-user2').valid).toBe(true);
    expect(handleRefresh(db, kv, 'user-2', 'refresh:user-2:token2').success).toBe(true);
  });

  it('should handle device without stored deviceId (legacy tokens)', () => {
    db.addDevice({
      id: 'device-123',
      user_id: 'user-456',
      name: 'Karoo',
      type: 'karoo',
      last_sync: null,
      created_at: new Date().toISOString()
    });

    // Legacy token without deviceId
    kv.put('refresh:user-456:token123', JSON.stringify({
      createdAt: Date.now()
      // No deviceId field
    }));

    // Should succeed (no device verification for legacy tokens)
    const result = handleRefresh(db, kv, 'user-456', 'refresh:user-456:token123');
    expect(result.success).toBe(true);

    // But if header provides deviceId, it should be verified
    db.deleteDevice('device-123', 'user-456');
    const resultWithHeader = handleRefresh(db, kv, 'user-456', 'refresh:user-456:token123', 'device-123');
    expect(resultWithHeader.success).toBe(false);
    expect(resultWithHeader.code).toBe('DEVICE_REVOKED');
  });
});

describe('Error Codes', () => {
  it('DEVICE_REVOKED code should be consistent', () => {
    const db = new MockDatabase();
    const result = verifyDevice(db, 'any-user', 'any-device');
    expect(result.code).toBe('DEVICE_REVOKED');
  });

  it('should distinguish between token revoked and device revoked', () => {
    const db = new MockDatabase();
    const kv = new MockKVStore();

    db.addDevice({
      id: 'device-123',
      user_id: 'user-456',
      name: 'Karoo',
      type: 'karoo',
      last_sync: null,
      created_at: new Date().toISOString()
    });

    // No token in KV - "Token revoked"
    const tokenRevokedResult = handleRefresh(db, kv, 'user-456', 'refresh:user-456:token123');
    expect(tokenRevokedResult.error).toBe('Token revoked');
    expect(tokenRevokedResult.code).toBeUndefined();

    // Token exists but device deleted - "DEVICE_REVOKED"
    kv.put('refresh:user-456:token123', JSON.stringify({
      createdAt: Date.now(),
      deviceId: 'device-123'
    }));
    db.deleteDevice('device-123', 'user-456');

    const deviceRevokedResult = handleRefresh(db, kv, 'user-456', 'refresh:user-456:token123');
    expect(deviceRevokedResult.code).toBe('DEVICE_REVOKED');
  });
});

/**
 * Tests for revokeRefreshTokensForDevice function
 * Simulates the jwt.ts implementation
 */

// Extended MockKVStore with list functionality (matches Cloudflare KV API)
class MockKVStoreWithList {
  private store: Map<string, string> = new Map();

  put(key: string, value: string) {
    this.store.set(key, value);
  }

  get(key: string): string | null {
    return this.store.get(key) || null;
  }

  delete(key: string) {
    this.store.delete(key);
  }

  list(options: { prefix: string }): { keys: { name: string }[] } {
    const keys: { name: string }[] = [];
    for (const key of this.store.keys()) {
      if (key.startsWith(options.prefix)) {
        keys.push({ name: key });
      }
    }
    return { keys };
  }

  clear() {
    this.store.clear();
  }

  size(): number {
    return this.store.size;
  }
}

// Implementation matching jwt.ts revokeRefreshTokensForDevice
async function revokeRefreshTokensForDevice(
  kv: MockKVStoreWithList,
  userId: string,
  deviceId: string
): Promise<number> {
  let deletedCount = 0;

  const list = kv.list({ prefix: `refresh:${userId}:` });

  for (const key of list.keys) {
    try {
      const tokenDataStr = kv.get(key.name);
      if (tokenDataStr) {
        const tokenData = JSON.parse(tokenDataStr) as { deviceId?: string };
        if (tokenData.deviceId === deviceId) {
          kv.delete(key.name);
          deletedCount++;
        }
      }
    } catch (e) {
      // Skip malformed tokens
    }
  }

  return deletedCount;
}

describe('Revoke Refresh Tokens for Device', () => {
  let kv: MockKVStoreWithList;

  beforeEach(() => {
    kv = new MockKVStoreWithList();
  });

  it('should delete all tokens for specified device', async () => {
    // Add tokens for device-123
    kv.put('refresh:user-456:token1', JSON.stringify({ createdAt: Date.now(), deviceId: 'device-123' }));
    kv.put('refresh:user-456:token2', JSON.stringify({ createdAt: Date.now(), deviceId: 'device-123' }));
    // Add token for different device
    kv.put('refresh:user-456:token3', JSON.stringify({ createdAt: Date.now(), deviceId: 'device-other' }));

    const deleted = await revokeRefreshTokensForDevice(kv, 'user-456', 'device-123');

    expect(deleted).toBe(2);
    expect(kv.get('refresh:user-456:token1')).toBeNull();
    expect(kv.get('refresh:user-456:token2')).toBeNull();
    expect(kv.get('refresh:user-456:token3')).not.toBeNull(); // Other device token preserved
  });

  it('should not affect tokens without deviceId (legacy)', async () => {
    kv.put('refresh:user-456:legacy', JSON.stringify({ createdAt: Date.now() })); // No deviceId
    kv.put('refresh:user-456:new', JSON.stringify({ createdAt: Date.now(), deviceId: 'device-123' }));

    const deleted = await revokeRefreshTokensForDevice(kv, 'user-456', 'device-123');

    expect(deleted).toBe(1);
    expect(kv.get('refresh:user-456:legacy')).not.toBeNull(); // Legacy preserved
    expect(kv.get('refresh:user-456:new')).toBeNull();
  });

  it('should not affect other users tokens', async () => {
    kv.put('refresh:user-456:token1', JSON.stringify({ createdAt: Date.now(), deviceId: 'device-123' }));
    kv.put('refresh:user-789:token1', JSON.stringify({ createdAt: Date.now(), deviceId: 'device-123' }));

    const deleted = await revokeRefreshTokensForDevice(kv, 'user-456', 'device-123');

    expect(deleted).toBe(1);
    expect(kv.get('refresh:user-456:token1')).toBeNull();
    expect(kv.get('refresh:user-789:token1')).not.toBeNull(); // Other user preserved
  });

  it('should return 0 when no matching tokens', async () => {
    kv.put('refresh:user-456:token1', JSON.stringify({ createdAt: Date.now(), deviceId: 'device-other' }));

    const deleted = await revokeRefreshTokensForDevice(kv, 'user-456', 'device-123');

    expect(deleted).toBe(0);
    expect(kv.size()).toBe(1); // Token preserved
  });

  it('should handle empty KV store', async () => {
    const deleted = await revokeRefreshTokensForDevice(kv, 'user-456', 'device-123');
    expect(deleted).toBe(0);
  });

  it('should skip malformed token data', async () => {
    kv.put('refresh:user-456:valid', JSON.stringify({ createdAt: Date.now(), deviceId: 'device-123' }));
    kv.put('refresh:user-456:malformed', 'not-json');

    const deleted = await revokeRefreshTokensForDevice(kv, 'user-456', 'device-123');

    expect(deleted).toBe(1);
    // Malformed token should still exist (not deleted, not crashed)
    expect(kv.get('refresh:user-456:malformed')).toBe('not-json');
  });
});

describe('Device Deletion with Token Revocation', () => {
  let db: MockDatabase;
  let kv: MockKVStoreWithList;

  beforeEach(() => {
    db = new MockDatabase();
    kv = new MockKVStoreWithList();
  });

  it('should revoke tokens when device is deleted', async () => {
    // Setup: device with tokens
    db.addDevice({
      id: 'device-123',
      user_id: 'user-456',
      name: 'Karoo',
      type: 'karoo',
      last_sync: null,
      created_at: new Date().toISOString()
    });
    kv.put('refresh:user-456:token1', JSON.stringify({ createdAt: Date.now(), deviceId: 'device-123' }));
    kv.put('refresh:user-456:token2', JSON.stringify({ createdAt: Date.now(), deviceId: 'device-123' }));

    // Action: delete device (simulates DELETE /api/devices/:id)
    const revokedCount = await revokeRefreshTokensForDevice(kv, 'user-456', 'device-123');
    db.deleteDevice('device-123', 'user-456');

    // Verify: tokens are gone
    expect(revokedCount).toBe(2);
    expect(kv.get('refresh:user-456:token1')).toBeNull();
    expect(kv.get('refresh:user-456:token2')).toBeNull();
    expect(db.findDevice('device-123', 'user-456')).toBeNull();
  });

  it('should handle device with no tokens', async () => {
    db.addDevice({
      id: 'device-123',
      user_id: 'user-456',
      name: 'Karoo',
      type: 'karoo',
      last_sync: null,
      created_at: new Date().toISOString()
    });

    const revokedCount = await revokeRefreshTokensForDevice(kv, 'user-456', 'device-123');
    db.deleteDevice('device-123', 'user-456');

    expect(revokedCount).toBe(0);
    expect(db.findDevice('device-123', 'user-456')).toBeNull();
  });
});
