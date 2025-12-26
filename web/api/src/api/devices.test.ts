import { describe, it, expect, beforeEach } from 'vitest';

/**
 * Unit tests for Devices API
 *
 * Tests device listing, status calculation, sync requests, and device deletion
 */

// Types
interface Device {
  id: string;
  user_id: string;
  name: string;
  type: string;
  last_sync: string | null;
  created_at: string;
}

interface DeviceWithStatus extends Device {
  status: 'connected' | 'idle' | 'offline';
  last_sync_relative: string | null;
}

// Mock database
class MockDeviceDatabase {
  private devices: Device[] = [];

  addDevice(device: Device): void {
    this.devices.push(device);
  }

  getDevicesForUser(userId: string): Device[] {
    return this.devices
      .filter(d => d.user_id === userId)
      .sort((a, b) => {
        // Sort by last_sync DESC NULLS LAST
        if (!a.last_sync && !b.last_sync) return 0;
        if (!a.last_sync) return 1;
        if (!b.last_sync) return -1;
        return new Date(b.last_sync).getTime() - new Date(a.last_sync).getTime();
      });
  }

  getDevice(deviceId: string, userId: string): Device | null {
    return this.devices.find(d => d.id === deviceId && d.user_id === userId) || null;
  }

  deleteDevice(deviceId: string, userId: string): boolean {
    const index = this.devices.findIndex(d => d.id === deviceId && d.user_id === userId);
    if (index === -1) return false;
    this.devices.splice(index, 1);
    return true;
  }

  clear(): void {
    this.devices = [];
  }
}

// Mock KV store for sync requests
class MockKV {
  private store: Map<string, string> = new Map();

  async get(key: string): Promise<string | null> {
    return this.store.get(key) || null;
  }

  async put(key: string, value: string, options?: { expirationTtl?: number }): Promise<void> {
    this.store.set(key, value);
  }

  async delete(key: string): Promise<void> {
    this.store.delete(key);
  }

  async list(options?: { prefix?: string }): Promise<{ keys: { name: string }[] }> {
    const keys: { name: string }[] = [];
    for (const key of this.store.keys()) {
      if (!options?.prefix || key.startsWith(options.prefix)) {
        keys.push({ name: key });
      }
    }
    return { keys };
  }

  clear(): void {
    this.store.clear();
  }
}

// Helper to calculate device status (mirrors devices.ts logic)
function calculateDeviceStatus(
  lastSync: string | null,
  createdAt: string,
  now: number = Date.now()
): { status: 'connected' | 'idle' | 'offline'; lastSyncRelative: string | null } {
  if (!lastSync) {
    return { status: 'idle', lastSyncRelative: 'Just linked' };
  }

  const lastSyncDate = new Date(lastSync.replace(' ', 'T') + 'Z');
  const diffMs = now - lastSyncDate.getTime();
  const diffMinutes = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);

  let status: 'connected' | 'idle' | 'offline';
  if (diffMinutes < 30) {
    status = 'connected';
  } else if (diffHours < 24) {
    status = 'idle';
  } else {
    status = 'offline';
  }

  let lastSyncRelative: string;
  if (diffMinutes < 1) {
    lastSyncRelative = 'Just now';
  } else if (diffMinutes < 60) {
    lastSyncRelative = `${diffMinutes} min ago`;
  } else if (diffHours < 24) {
    lastSyncRelative = `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
  } else if (diffDays < 7) {
    lastSyncRelative = `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
  } else {
    lastSyncRelative = lastSyncDate.toLocaleDateString();
  }

  return { status, lastSyncRelative };
}

// API handlers
function handleGetDevices(
  db: MockDeviceDatabase,
  userId: string,
  now: number = Date.now()
): { success: boolean; data: { devices: DeviceWithStatus[]; count: number } } {
  const devices = db.getDevicesForUser(userId);

  const devicesWithStatus = devices.map(device => {
    const { status, lastSyncRelative } = calculateDeviceStatus(
      device.last_sync,
      device.created_at,
      now
    );
    return {
      ...device,
      status,
      last_sync_relative: lastSyncRelative,
    };
  });

  return {
    success: true,
    data: {
      devices: devicesWithStatus,
      count: devicesWithStatus.length,
    },
  };
}

async function handleRequestSync(
  db: MockDeviceDatabase,
  kv: MockKV,
  userId: string,
  deviceId: string
): Promise<{ success: boolean; status: number; message?: string; error?: string }> {
  const device = db.getDevice(deviceId, userId);

  if (!device) {
    return { success: false, status: 404, error: 'Device not found' };
  }

  await kv.put(
    `sync_request:${deviceId}`,
    JSON.stringify({ requestedAt: Date.now(), userId }),
    { expirationTtl: 300 }
  );

  return {
    success: true,
    status: 200,
    message: 'Sync requested. Open KPedal on your device to sync.',
  };
}

async function handleDeleteDevice(
  db: MockDeviceDatabase,
  kv: MockKV,
  userId: string,
  deviceId: string
): Promise<{ success: boolean; status: number; message?: string; error?: string }> {
  const device = db.getDevice(deviceId, userId);

  if (!device) {
    return { success: false, status: 404, error: 'Device not found' };
  }

  // Revoke refresh tokens (simplified - just delete from KV)
  const tokens = await kv.list({ prefix: `refresh:${userId}:` });
  for (const key of tokens.keys) {
    const tokenData = await kv.get(key.name);
    if (tokenData) {
      try {
        const parsed = JSON.parse(tokenData);
        if (parsed.deviceId === deviceId) {
          await kv.delete(key.name);
        }
      } catch {
        // Skip malformed tokens
      }
    }
  }

  db.deleteDevice(deviceId, userId);

  return {
    success: true,
    status: 200,
    message: 'Device removed',
  };
}

describe('Get Devices Endpoint', () => {
  let db: MockDeviceDatabase;

  beforeEach(() => {
    db = new MockDeviceDatabase();
  });

  it('should return empty list for user with no devices', () => {
    const result = handleGetDevices(db, 'user-1');

    expect(result.success).toBe(true);
    expect(result.data.devices).toHaveLength(0);
    expect(result.data.count).toBe(0);
  });

  it('should return devices for user', () => {
    db.addDevice({
      id: 'device-1',
      user_id: 'user-1',
      name: 'My Karoo',
      type: 'karoo',
      last_sync: '2024-01-15 10:00:00',
      created_at: '2024-01-01 00:00:00',
    });

    const result = handleGetDevices(db, 'user-1');

    expect(result.data.devices).toHaveLength(1);
    expect(result.data.devices[0].name).toBe('My Karoo');
  });

  it('should not return devices for other users', () => {
    db.addDevice({
      id: 'device-1',
      user_id: 'user-1',
      name: 'User 1 Karoo',
      type: 'karoo',
      last_sync: null,
      created_at: '2024-01-01 00:00:00',
    });
    db.addDevice({
      id: 'device-2',
      user_id: 'user-2',
      name: 'User 2 Karoo',
      type: 'karoo',
      last_sync: null,
      created_at: '2024-01-01 00:00:00',
    });

    const result = handleGetDevices(db, 'user-1');

    expect(result.data.devices).toHaveLength(1);
    expect(result.data.devices[0].id).toBe('device-1');
  });

  it('should sort devices by last_sync DESC', () => {
    db.addDevice({
      id: 'device-old',
      user_id: 'user-1',
      name: 'Old Device',
      type: 'karoo',
      last_sync: '2024-01-01 00:00:00',
      created_at: '2024-01-01 00:00:00',
    });
    db.addDevice({
      id: 'device-new',
      user_id: 'user-1',
      name: 'New Device',
      type: 'karoo',
      last_sync: '2024-01-15 00:00:00',
      created_at: '2024-01-01 00:00:00',
    });

    const result = handleGetDevices(db, 'user-1');

    expect(result.data.devices[0].id).toBe('device-new');
    expect(result.data.devices[1].id).toBe('device-old');
  });

  it('should put devices with null last_sync at the end', () => {
    db.addDevice({
      id: 'device-never',
      user_id: 'user-1',
      name: 'Never Synced',
      type: 'karoo',
      last_sync: null,
      created_at: '2024-01-01 00:00:00',
    });
    db.addDevice({
      id: 'device-synced',
      user_id: 'user-1',
      name: 'Synced Device',
      type: 'karoo',
      last_sync: '2024-01-01 00:00:00',
      created_at: '2024-01-01 00:00:00',
    });

    const result = handleGetDevices(db, 'user-1');

    expect(result.data.devices[0].id).toBe('device-synced');
    expect(result.data.devices[1].id).toBe('device-never');
  });
});

describe('Device Status Calculation', () => {
  const NOW = new Date('2024-01-15T12:00:00Z').getTime();

  it('should return "connected" for activity within 30 minutes', () => {
    const result = calculateDeviceStatus(
      '2024-01-15 11:45:00',
      '2024-01-01 00:00:00',
      NOW
    );
    expect(result.status).toBe('connected');
  });

  it('should return "idle" for activity between 30 min and 24 hours', () => {
    const result = calculateDeviceStatus(
      '2024-01-15 10:00:00', // 2 hours ago
      '2024-01-01 00:00:00',
      NOW
    );
    expect(result.status).toBe('idle');
  });

  it('should return "offline" for activity over 24 hours ago', () => {
    const result = calculateDeviceStatus(
      '2024-01-13 12:00:00', // 2 days ago
      '2024-01-01 00:00:00',
      NOW
    );
    expect(result.status).toBe('offline');
  });

  it('should return "idle" and "Just linked" for null last_sync', () => {
    const result = calculateDeviceStatus(
      null,
      '2024-01-15 11:00:00',
      NOW
    );
    expect(result.status).toBe('idle');
    expect(result.lastSyncRelative).toBe('Just linked');
  });
});

describe('Device Status - Relative Time', () => {
  const NOW = new Date('2024-01-15T12:00:00Z').getTime();

  it('should show "Just now" for < 1 minute', () => {
    const result = calculateDeviceStatus(
      '2024-01-15 11:59:30', // 30 seconds ago
      '2024-01-01 00:00:00',
      NOW
    );
    expect(result.lastSyncRelative).toBe('Just now');
  });

  it('should show "X min ago" for < 60 minutes', () => {
    const result = calculateDeviceStatus(
      '2024-01-15 11:45:00', // 15 min ago
      '2024-01-01 00:00:00',
      NOW
    );
    expect(result.lastSyncRelative).toBe('15 min ago');
  });

  it('should show "1 hour ago" (singular)', () => {
    const result = calculateDeviceStatus(
      '2024-01-15 11:00:00', // 1 hour ago
      '2024-01-01 00:00:00',
      NOW
    );
    expect(result.lastSyncRelative).toBe('1 hour ago');
  });

  it('should show "X hours ago" (plural)', () => {
    const result = calculateDeviceStatus(
      '2024-01-15 06:00:00', // 6 hours ago
      '2024-01-01 00:00:00',
      NOW
    );
    expect(result.lastSyncRelative).toBe('6 hours ago');
  });

  it('should show "1 day ago" (singular)', () => {
    const result = calculateDeviceStatus(
      '2024-01-14 12:00:00', // 1 day ago
      '2024-01-01 00:00:00',
      NOW
    );
    expect(result.lastSyncRelative).toBe('1 day ago');
  });

  it('should show "X days ago" (plural)', () => {
    const result = calculateDeviceStatus(
      '2024-01-12 12:00:00', // 3 days ago
      '2024-01-01 00:00:00',
      NOW
    );
    expect(result.lastSyncRelative).toBe('3 days ago');
  });

  it('should show date for > 7 days', () => {
    const result = calculateDeviceStatus(
      '2024-01-01 12:00:00', // 14 days ago
      '2024-01-01 00:00:00',
      NOW
    );
    // This will be a localized date string
    expect(result.lastSyncRelative).toMatch(/\d/);
    expect(result.lastSyncRelative).not.toContain('ago');
  });
});

describe('Request Sync Endpoint', () => {
  let db: MockDeviceDatabase;
  let kv: MockKV;

  beforeEach(() => {
    db = new MockDeviceDatabase();
    kv = new MockKV();
  });

  it('should create sync request for valid device', async () => {
    db.addDevice({
      id: 'device-1',
      user_id: 'user-1',
      name: 'My Karoo',
      type: 'karoo',
      last_sync: null,
      created_at: '2024-01-01 00:00:00',
    });

    const result = await handleRequestSync(db, kv, 'user-1', 'device-1');

    expect(result.success).toBe(true);
    expect(result.status).toBe(200);
    expect(result.message).toContain('Sync requested');
  });

  it('should store sync request in KV', async () => {
    db.addDevice({
      id: 'device-1',
      user_id: 'user-1',
      name: 'My Karoo',
      type: 'karoo',
      last_sync: null,
      created_at: '2024-01-01 00:00:00',
    });

    await handleRequestSync(db, kv, 'user-1', 'device-1');

    const stored = await kv.get('sync_request:device-1');
    expect(stored).not.toBeNull();

    const parsed = JSON.parse(stored!);
    expect(parsed.userId).toBe('user-1');
    expect(parsed.requestedAt).toBeDefined();
  });

  it('should return 404 for non-existent device', async () => {
    const result = await handleRequestSync(db, kv, 'user-1', 'nonexistent');

    expect(result.success).toBe(false);
    expect(result.status).toBe(404);
    expect(result.error).toBe('Device not found');
  });

  it('should return 404 for device belonging to another user', async () => {
    db.addDevice({
      id: 'device-1',
      user_id: 'user-2', // Different user
      name: 'Other Karoo',
      type: 'karoo',
      last_sync: null,
      created_at: '2024-01-01 00:00:00',
    });

    const result = await handleRequestSync(db, kv, 'user-1', 'device-1');

    expect(result.success).toBe(false);
    expect(result.status).toBe(404);
  });
});

describe('Delete Device Endpoint', () => {
  let db: MockDeviceDatabase;
  let kv: MockKV;

  beforeEach(() => {
    db = new MockDeviceDatabase();
    kv = new MockKV();
  });

  it('should delete device successfully', async () => {
    db.addDevice({
      id: 'device-1',
      user_id: 'user-1',
      name: 'My Karoo',
      type: 'karoo',
      last_sync: null,
      created_at: '2024-01-01 00:00:00',
    });

    const result = await handleDeleteDevice(db, kv, 'user-1', 'device-1');

    expect(result.success).toBe(true);
    expect(result.status).toBe(200);
    expect(result.message).toBe('Device removed');

    // Verify device is deleted
    expect(db.getDevice('device-1', 'user-1')).toBeNull();
  });

  it('should return 404 for non-existent device', async () => {
    const result = await handleDeleteDevice(db, kv, 'user-1', 'nonexistent');

    expect(result.success).toBe(false);
    expect(result.status).toBe(404);
    expect(result.error).toBe('Device not found');
  });

  it('should return 404 for device belonging to another user', async () => {
    db.addDevice({
      id: 'device-1',
      user_id: 'user-2',
      name: 'Other Karoo',
      type: 'karoo',
      last_sync: null,
      created_at: '2024-01-01 00:00:00',
    });

    const result = await handleDeleteDevice(db, kv, 'user-1', 'device-1');

    expect(result.success).toBe(false);
    expect(result.status).toBe(404);
  });

  it('should revoke refresh tokens for deleted device', async () => {
    db.addDevice({
      id: 'device-1',
      user_id: 'user-1',
      name: 'My Karoo',
      type: 'karoo',
      last_sync: null,
      created_at: '2024-01-01 00:00:00',
    });

    // Add refresh tokens
    await kv.put('refresh:user-1:token1', JSON.stringify({ deviceId: 'device-1' }));
    await kv.put('refresh:user-1:token2', JSON.stringify({ deviceId: 'device-1' }));
    await kv.put('refresh:user-1:token3', JSON.stringify({ deviceId: 'other-device' }));

    await handleDeleteDevice(db, kv, 'user-1', 'device-1');

    // Tokens for device-1 should be deleted
    expect(await kv.get('refresh:user-1:token1')).toBeNull();
    expect(await kv.get('refresh:user-1:token2')).toBeNull();
    // Token for other device should remain
    expect(await kv.get('refresh:user-1:token3')).not.toBeNull();
  });

  it('should not delete other users devices', async () => {
    db.addDevice({
      id: 'device-1',
      user_id: 'user-1',
      name: 'User 1 Karoo',
      type: 'karoo',
      last_sync: null,
      created_at: '2024-01-01 00:00:00',
    });
    db.addDevice({
      id: 'device-2',
      user_id: 'user-2',
      name: 'User 2 Karoo',
      type: 'karoo',
      last_sync: null,
      created_at: '2024-01-01 00:00:00',
    });

    await handleDeleteDevice(db, kv, 'user-1', 'device-1');

    // User 2's device should still exist
    expect(db.getDevice('device-2', 'user-2')).not.toBeNull();
  });
});

describe('Device Types', () => {
  let db: MockDeviceDatabase;

  beforeEach(() => {
    db = new MockDeviceDatabase();
  });

  it('should support karoo device type', () => {
    db.addDevice({
      id: 'device-1',
      user_id: 'user-1',
      name: 'Karoo 3',
      type: 'karoo',
      last_sync: null,
      created_at: '2024-01-01 00:00:00',
    });

    const result = handleGetDevices(db, 'user-1');
    expect(result.data.devices[0].type).toBe('karoo');
  });

  it('should handle multiple devices per user', () => {
    db.addDevice({
      id: 'device-1',
      user_id: 'user-1',
      name: 'Karoo 3',
      type: 'karoo',
      last_sync: '2024-01-15 00:00:00',
      created_at: '2024-01-01 00:00:00',
    });
    db.addDevice({
      id: 'device-2',
      user_id: 'user-1',
      name: 'Karoo 2',
      type: 'karoo',
      last_sync: '2024-01-10 00:00:00',
      created_at: '2024-01-01 00:00:00',
    });

    const result = handleGetDevices(db, 'user-1');

    expect(result.data.devices).toHaveLength(2);
    expect(result.data.count).toBe(2);
  });
});

describe('Edge Cases', () => {
  let db: MockDeviceDatabase;
  let kv: MockKV;

  beforeEach(() => {
    db = new MockDeviceDatabase();
    kv = new MockKV();
  });

  it('should handle device with special characters in name', () => {
    db.addDevice({
      id: 'device-1',
      user_id: 'user-1',
      name: "John's Karoo (Main)",
      type: 'karoo',
      last_sync: null,
      created_at: '2024-01-01 00:00:00',
    });

    const result = handleGetDevices(db, 'user-1');
    expect(result.data.devices[0].name).toBe("John's Karoo (Main)");
  });

  it('should handle very long device IDs', () => {
    const longId = 'a'.repeat(100);
    db.addDevice({
      id: longId,
      user_id: 'user-1',
      name: 'Long ID Device',
      type: 'karoo',
      last_sync: null,
      created_at: '2024-01-01 00:00:00',
    });

    const result = handleGetDevices(db, 'user-1');
    expect(result.data.devices[0].id).toBe(longId);
  });

  it('should handle malformed token data during delete', async () => {
    db.addDevice({
      id: 'device-1',
      user_id: 'user-1',
      name: 'My Karoo',
      type: 'karoo',
      last_sync: null,
      created_at: '2024-01-01 00:00:00',
    });

    // Add malformed token
    await kv.put('refresh:user-1:bad-token', 'not-json');

    // Should not throw
    const result = await handleDeleteDevice(db, kv, 'user-1', 'device-1');
    expect(result.success).toBe(true);
  });

  it('should handle concurrent sync requests', async () => {
    db.addDevice({
      id: 'device-1',
      user_id: 'user-1',
      name: 'My Karoo',
      type: 'karoo',
      last_sync: null,
      created_at: '2024-01-01 00:00:00',
    });

    // Make multiple concurrent sync requests
    const results = await Promise.all([
      handleRequestSync(db, kv, 'user-1', 'device-1'),
      handleRequestSync(db, kv, 'user-1', 'device-1'),
      handleRequestSync(db, kv, 'user-1', 'device-1'),
    ]);

    // All should succeed
    expect(results.every(r => r.success)).toBe(true);
  });
});
