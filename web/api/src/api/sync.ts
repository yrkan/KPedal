import { Hono } from 'hono';
import { Env, ApiResponse, RideData } from '../types/env';
import { validateRideData, isValidDeviceId, sanitizeString } from '../middleware/validate';

const sync = new Hono<{ Bindings: Env }>();

/**
 * POST /api/sync/rides
 * Sync rides from device (batch upload)
 */
sync.post('/rides', async (c) => {
  const user = c.get('user');
  const deviceId = c.req.header('X-Device-ID');

  if (!deviceId || !isValidDeviceId(deviceId)) {
    return c.json<ApiResponse>({ success: false, error: 'Missing or invalid X-Device-ID header' }, 400);
  }

  try {
    const body = await c.req.json<{ rides: RideData[] }>();
    const { rides } = body;

    if (!rides || !Array.isArray(rides) || rides.length === 0) {
      return c.json<ApiResponse>({ success: false, error: 'No rides provided' }, 400);
    }

    // Limit batch size
    if (rides.length > 100) {
      return c.json<ApiResponse>({ success: false, error: 'Max 100 rides per batch' }, 400);
    }

    let inserted = 0;
    let duplicates = 0;
    let invalid = 0;

    for (const ride of rides) {
      // Validate ride data
      const validation = validateRideData(ride);
      if (!validation.valid) {
        invalid++;
        console.warn('Invalid ride data:', validation.errors);
        continue;
      }

      // Check for duplicate (same timestamp + device)
      const existing = await c.env.DB
        .prepare('SELECT id FROM rides WHERE user_id = ? AND device_id = ? AND timestamp = ?')
        .bind(user.sub, deviceId, ride.timestamp)
        .first();

      if (existing) {
        duplicates++;
        continue;
      }

      // Insert new ride (validated)
      await c.env.DB
        .prepare(`
          INSERT INTO rides (
            user_id, device_id, timestamp, duration_ms,
            balance_left, balance_right,
            te_left, te_right,
            ps_left, ps_right,
            zone_optimal, zone_attention, zone_problem,
            score, created_at
          ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'))
        `)
        .bind(
          user.sub, deviceId, ride.timestamp, ride.duration_ms,
          ride.balance_left, ride.balance_right,
          ride.te_left, ride.te_right,
          ride.ps_left, ride.ps_right,
          ride.zone_optimal, ride.zone_attention, ride.zone_problem,
          ride.score
        )
        .run();

      inserted++;
    }

    return c.json<ApiResponse>({
      success: true,
      data: {
        inserted,
        duplicates,
        invalid,
        total: rides.length,
      },
      message: `Synced ${inserted} rides (${duplicates} duplicates, ${invalid} invalid skipped)`,
    });

  } catch (err) {
    console.error('Error syncing rides:', err);
    return c.json<ApiResponse>({ success: false, error: 'Sync failed' }, 500);
  }
});

/**
 * GET /api/sync/status
 * Get sync status (last sync, count)
 */
sync.get('/status', async (c) => {
  const user = c.get('user');
  const deviceId = c.req.header('X-Device-ID');

  try {
    // Get last ride timestamp for this device
    const lastRide = await c.env.DB
      .prepare(`
        SELECT timestamp FROM rides
        WHERE user_id = ? AND device_id = ?
        ORDER BY timestamp DESC
        LIMIT 1
      `)
      .bind(user.sub, deviceId || '')
      .first<{ timestamp: number }>();

    // Get total count
    const count = await c.env.DB
      .prepare('SELECT COUNT(*) as count FROM rides WHERE user_id = ?')
      .bind(user.sub)
      .first<{ count: number }>();

    return c.json<ApiResponse>({
      success: true,
      data: {
        lastSyncTimestamp: lastRide?.timestamp || null,
        totalRides: count?.count || 0,
        deviceId: deviceId || null,
      },
    });

  } catch (err) {
    console.error('Error getting sync status:', err);
    return c.json<ApiResponse>({ success: false, error: 'Failed to get status' }, 500);
  }
});

/**
 * POST /api/sync/device
 * Register/update device
 */
sync.post('/device', async (c) => {
  const user = c.get('user');

  try {
    const body = await c.req.json<{
      device_id: string;
      device_name?: string;
      device_type?: string;
    }>();

    const { device_id, device_name, device_type } = body;

    if (!device_id || !isValidDeviceId(device_id)) {
      return c.json<ApiResponse>({ success: false, error: 'Missing or invalid device_id' }, 400);
    }

    // Upsert device
    await c.env.DB
      .prepare(`
        INSERT INTO devices (id, user_id, name, type, last_sync, created_at)
        VALUES (?, ?, ?, ?, datetime('now'), datetime('now'))
        ON CONFLICT(id) DO UPDATE SET
          name = excluded.name,
          type = excluded.type,
          last_sync = datetime('now')
      `)
      .bind(
        device_id,
        user.sub,
        sanitizeString(device_name || 'Karoo', 100),
        sanitizeString(device_type || 'karoo', 50)
      )
      .run();

    return c.json<ApiResponse>({
      success: true,
      message: 'Device registered',
      data: { device_id },
    });

  } catch (err) {
    console.error('Error registering device:', err);
    return c.json<ApiResponse>({ success: false, error: 'Failed to register device' }, 500);
  }
});

export { sync as syncRoutes };
