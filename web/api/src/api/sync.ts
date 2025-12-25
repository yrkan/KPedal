import { Hono } from 'hono';
import { Env, ApiResponse, RideData } from '../types/env';
import { validateRideData, isValidDeviceId, sanitizeString } from '../middleware/validate';

const sync = new Hono<{ Bindings: Env }>();

/**
 * Helper to verify device exists and belongs to user.
 * Returns null if valid, or error response if device was revoked/not found.
 */
async function verifyDevice(env: Env, userId: string, deviceId: string): Promise<Response | null> {
  const device = await env.DB
    .prepare('SELECT id FROM devices WHERE id = ? AND user_id = ?')
    .bind(deviceId, userId)
    .first();

  if (!device) {
    // Device was removed/revoked - return special error code
    return new Response(JSON.stringify({
      success: false,
      error: 'Device not found or access revoked',
      code: 'DEVICE_REVOKED'
    } as ApiResponse), {
      status: 403,
      headers: { 'Content-Type': 'application/json' }
    });
  }
  return null;
}

/**
 * POST /api/sync/ride
 * Sync single ride from device (used by Android app)
 */
sync.post('/ride', async (c) => {
  const user = c.get('user');
  const deviceId = c.req.header('X-Device-ID');

  if (!deviceId || !isValidDeviceId(deviceId)) {
    return c.json<ApiResponse>({ success: false, error: 'Missing or invalid X-Device-ID header' }, 400);
  }

  // Verify device still exists and belongs to user
  const revokedResponse = await verifyDevice(c.env, user.sub, deviceId);
  if (revokedResponse) return revokedResponse;

  try {
    const body = await c.req.json<{
      timestamp: number;
      duration: number;
      balance_left_avg: number;
      balance_right_avg: number;
      te_left_avg: number;
      te_right_avg: number;
      ps_left_avg: number;
      ps_right_avg: number;
      optimal_pct: number;
      attention_pct: number;
      problem_pct: number;
    }>();

    // Convert from client format to server format
    const ride: RideData = {
      timestamp: body.timestamp,
      duration_ms: body.duration,
      balance_left: body.balance_left_avg,
      balance_right: body.balance_right_avg,
      te_left: body.te_left_avg,
      te_right: body.te_right_avg,
      ps_left: body.ps_left_avg,
      ps_right: body.ps_right_avg,
      zone_optimal: body.optimal_pct,
      zone_attention: body.attention_pct,
      zone_problem: body.problem_pct,
      score: Math.round((body.optimal_pct * 100 + body.attention_pct * 50) / 100),
    };

    // Validate ride data
    const validation = validateRideData(ride);
    if (!validation.valid) {
      return c.json<ApiResponse>({
        success: false,
        error: `Invalid ride data: ${validation.errors.join(', ')}`
      }, 400);
    }

    // Check for duplicate
    const existing = await c.env.DB
      .prepare('SELECT id FROM rides WHERE user_id = ? AND device_id = ? AND timestamp = ?')
      .bind(user.sub, deviceId, ride.timestamp)
      .first();

    if (existing) {
      return c.json<ApiResponse>({
        success: true,
        data: { ride_id: String(existing.id), synced_at: new Date().toISOString() },
        message: 'Ride already synced',
      });
    }

    // Insert ride
    const result = await c.env.DB
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

    // Update device last_sync
    await c.env.DB
      .prepare(`UPDATE devices SET last_sync = datetime('now') WHERE id = ? AND user_id = ?`)
      .bind(deviceId, user.sub)
      .run();

    return c.json<ApiResponse>({
      success: true,
      data: {
        ride_id: String(result.meta?.last_row_id || 0),
        synced_at: new Date().toISOString(),
      },
      message: 'Ride synced successfully',
    });

  } catch (err) {
    console.error('Error syncing ride:', err);
    return c.json<ApiResponse>({ success: false, error: 'Sync failed' }, 500);
  }
});

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

  // Verify device still exists and belongs to user
  const revokedResponse = await verifyDevice(c.env, user.sub, deviceId);
  if (revokedResponse) return revokedResponse;

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

    // 1. Validate all rides first and filter out invalid ones
    const validRides: RideData[] = [];
    let invalid = 0;

    for (const ride of rides) {
      const validation = validateRideData(ride);
      if (!validation.valid) {
        invalid++;
        console.warn('Invalid ride data:', validation.errors);
        continue;
      }
      validRides.push(ride);
    }

    if (validRides.length === 0) {
      return c.json<ApiResponse>({
        success: true,
        data: { inserted: 0, duplicates: 0, invalid, total: rides.length },
        message: `No valid rides to sync (${invalid} invalid)`,
      });
    }

    // 2. Batch check for duplicates - ONE query instead of N queries
    const timestamps = validRides.map(r => r.timestamp);
    const placeholders = timestamps.map(() => '?').join(',');
    const existingResult = await c.env.DB
      .prepare(`SELECT timestamp FROM rides WHERE user_id = ? AND device_id = ? AND timestamp IN (${placeholders})`)
      .bind(user.sub, deviceId, ...timestamps)
      .all<{ timestamp: number }>();

    const existingTimestamps = new Set(existingResult.results.map(r => r.timestamp));

    // 3. Filter out duplicates
    const newRides = validRides.filter(r => !existingTimestamps.has(r.timestamp));
    const duplicates = validRides.length - newRides.length;

    // 4. Batch insert new rides using D1 batch API
    if (newRides.length > 0) {
      const insertStatements = newRides.map(ride =>
        c.env.DB.prepare(`
          INSERT INTO rides (
            user_id, device_id, timestamp, duration_ms,
            balance_left, balance_right,
            te_left, te_right,
            ps_left, ps_right,
            zone_optimal, zone_attention, zone_problem,
            score, created_at
          ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'))
        `).bind(
          user.sub, deviceId, ride.timestamp, ride.duration_ms,
          ride.balance_left, ride.balance_right,
          ride.te_left, ride.te_right,
          ride.ps_left, ride.ps_right,
          ride.zone_optimal, ride.zone_attention, ride.zone_problem,
          ride.score
        )
      );

      await c.env.DB.batch(insertStatements);
    }

    const inserted = newRides.length;

    // Update device last_sync timestamp if any rides were synced
    if (inserted > 0 || duplicates > 0) {
      await c.env.DB
        .prepare(`UPDATE devices SET last_sync = datetime('now') WHERE id = ? AND user_id = ?`)
        .bind(deviceId, user.sub)
        .run();
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

/**
 * GET /api/sync/check-request
 * Check if there's a pending sync request for this device (set by web "Request Sync" button)
 * Also acts as a heartbeat - updates device last_sync to show it's online
 */
sync.get('/check-request', async (c) => {
  const user = c.get('user');
  const deviceId = c.req.header('X-Device-ID');

  if (!deviceId || !isValidDeviceId(deviceId)) {
    return c.json<ApiResponse>({ success: false, error: 'Missing or invalid X-Device-ID header' }, 400);
  }

  // Verify device still exists and belongs to user
  const revokedResponse = await verifyDevice(c.env, user.sub, deviceId);
  if (revokedResponse) return revokedResponse;

  try {
    // Update device last_sync as heartbeat (device is online)
    await c.env.DB
      .prepare(`UPDATE devices SET last_sync = datetime('now') WHERE id = ? AND user_id = ?`)
      .bind(deviceId, user.sub)
      .run();

    // Check KV for sync request
    const syncRequest = await c.env.SESSIONS.get(`sync_request:${deviceId}`);

    if (syncRequest) {
      const request = JSON.parse(syncRequest) as { requestedAt: number; userId: string };

      // Verify request is for this user
      if (request.userId === user.sub) {
        // Delete the request so it's only processed once
        await c.env.SESSIONS.delete(`sync_request:${deviceId}`);

        return c.json<ApiResponse>({
          success: true,
          data: {
            syncRequested: true,
            requestedAt: request.requestedAt,
          },
        });
      }
    }

    return c.json<ApiResponse>({
      success: true,
      data: {
        syncRequested: false,
      },
    });
  } catch (err) {
    console.error('Error checking sync request:', err);
    return c.json<ApiResponse>({ success: false, error: 'Failed to check sync request' }, 500);
  }
});

export { sync as syncRoutes };
