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
      // Extended metrics (optional for backwards compatibility)
      power_avg?: number;
      power_max?: number;
      cadence_avg?: number;
      hr_avg?: number;
      hr_max?: number;
      speed_avg?: number;
      distance_km?: number;
      // Pro cyclist metrics (optional)
      elevation_gain?: number;
      elevation_loss?: number;
      grade_avg?: number;
      grade_max?: number;
      normalized_power?: number;
      energy_kj?: number;
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
      // Extended metrics (default to 0 if not provided)
      power_avg: body.power_avg ?? 0,
      power_max: body.power_max ?? 0,
      cadence_avg: body.cadence_avg ?? 0,
      hr_avg: body.hr_avg ?? 0,
      hr_max: body.hr_max ?? 0,
      speed_avg: body.speed_avg ?? 0,
      distance_km: body.distance_km ?? 0,
      // Pro cyclist metrics
      elevation_gain: body.elevation_gain ?? 0,
      elevation_loss: body.elevation_loss ?? 0,
      grade_avg: body.grade_avg ?? 0,
      grade_max: body.grade_max ?? 0,
      normalized_power: body.normalized_power ?? 0,
      energy_kj: body.energy_kj ?? 0,
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
          score,
          power_avg, power_max, cadence_avg, hr_avg, hr_max, speed_avg, distance_km,
          elevation_gain, elevation_loss, grade_avg, grade_max,
          normalized_power, energy_kj,
          created_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'))
      `)
      .bind(
        user.sub, deviceId, ride.timestamp, ride.duration_ms,
        ride.balance_left, ride.balance_right,
        ride.te_left, ride.te_right,
        ride.ps_left, ride.ps_right,
        ride.zone_optimal, ride.zone_attention, ride.zone_problem,
        ride.score,
        ride.power_avg, ride.power_max, ride.cadence_avg, ride.hr_avg, ride.hr_max, ride.speed_avg, ride.distance_km,
        ride.elevation_gain, ride.elevation_loss, ride.grade_avg, ride.grade_max,
        ride.normalized_power, ride.energy_kj
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
 * POST /api/sync/ride-full
 * Sync single ride with per-minute snapshots from device
 */
sync.post('/ride-full', async (c) => {
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
      ride: {
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
        power_avg?: number;
        power_max?: number;
        cadence_avg?: number;
        hr_avg?: number;
        hr_max?: number;
        speed_avg?: number;
        distance_km?: number;
        // Pro cyclist metrics (optional)
        elevation_gain?: number;
        elevation_loss?: number;
        grade_avg?: number;
        grade_max?: number;
        normalized_power?: number;
        energy_kj?: number;
      };
      snapshots: Array<{
        minute_index: number;
        timestamp: number;
        balance_left: number;
        balance_right: number;
        te_left: number;
        te_right: number;
        ps_left: number;
        ps_right: number;
        power_avg: number;
        cadence_avg: number;
        hr_avg: number;
        zone_status: string;
      }>;
    }>();

    const { ride, snapshots } = body;

    // Convert ride from client format to server format
    const rideData: RideData = {
      timestamp: ride.timestamp,
      duration_ms: ride.duration,
      balance_left: ride.balance_left_avg,
      balance_right: ride.balance_right_avg,
      te_left: ride.te_left_avg,
      te_right: ride.te_right_avg,
      ps_left: ride.ps_left_avg,
      ps_right: ride.ps_right_avg,
      zone_optimal: ride.optimal_pct,
      zone_attention: ride.attention_pct,
      zone_problem: ride.problem_pct,
      score: Math.round((ride.optimal_pct * 100 + ride.attention_pct * 50) / 100),
      power_avg: ride.power_avg ?? 0,
      power_max: ride.power_max ?? 0,
      cadence_avg: ride.cadence_avg ?? 0,
      hr_avg: ride.hr_avg ?? 0,
      hr_max: ride.hr_max ?? 0,
      speed_avg: ride.speed_avg ?? 0,
      distance_km: ride.distance_km ?? 0,
      // Pro cyclist metrics
      elevation_gain: ride.elevation_gain ?? 0,
      elevation_loss: ride.elevation_loss ?? 0,
      grade_avg: ride.grade_avg ?? 0,
      grade_max: ride.grade_max ?? 0,
      normalized_power: ride.normalized_power ?? 0,
      energy_kj: ride.energy_kj ?? 0,
    };

    // Validate ride data
    const validation = validateRideData(rideData);
    if (!validation.valid) {
      return c.json<ApiResponse>({
        success: false,
        error: `Invalid ride data: ${validation.errors.join(', ')}`
      }, 400);
    }

    // Check for duplicate ride
    const existing = await c.env.DB
      .prepare('SELECT id FROM rides WHERE user_id = ? AND device_id = ? AND timestamp = ?')
      .bind(user.sub, deviceId, rideData.timestamp)
      .first<{ id: number }>();

    if (existing) {
      return c.json<ApiResponse>({
        success: true,
        data: { ride_id: String(existing.id), synced_at: new Date().toISOString() },
        message: 'Ride already synced',
      });
    }

    // Insert ride
    const rideResult = await c.env.DB
      .prepare(`
        INSERT INTO rides (
          user_id, device_id, timestamp, duration_ms,
          balance_left, balance_right,
          te_left, te_right,
          ps_left, ps_right,
          zone_optimal, zone_attention, zone_problem,
          score,
          power_avg, power_max, cadence_avg, hr_avg, hr_max, speed_avg, distance_km,
          elevation_gain, elevation_loss, grade_avg, grade_max,
          normalized_power, energy_kj,
          created_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'))
      `)
      .bind(
        user.sub, deviceId, rideData.timestamp, rideData.duration_ms,
        rideData.balance_left, rideData.balance_right,
        rideData.te_left, rideData.te_right,
        rideData.ps_left, rideData.ps_right,
        rideData.zone_optimal, rideData.zone_attention, rideData.zone_problem,
        rideData.score,
        rideData.power_avg, rideData.power_max, rideData.cadence_avg, rideData.hr_avg, rideData.hr_max, rideData.speed_avg, rideData.distance_km,
        rideData.elevation_gain, rideData.elevation_loss, rideData.grade_avg, rideData.grade_max,
        rideData.normalized_power, rideData.energy_kj
      )
      .run();

    const rideId = rideResult.meta?.last_row_id;

    // Insert snapshots if any
    if (snapshots && snapshots.length > 0 && rideId) {
      const snapshotStatements = snapshots.map(s =>
        c.env.DB.prepare(`
          INSERT INTO ride_snapshots (
            ride_id, minute_index, timestamp,
            balance_left, balance_right,
            te_left, te_right,
            ps_left, ps_right,
            power_avg, cadence_avg, hr_avg,
            zone_status
          ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        `).bind(
          rideId, s.minute_index, s.timestamp,
          s.balance_left, s.balance_right,
          s.te_left, s.te_right,
          s.ps_left, s.ps_right,
          s.power_avg, s.cadence_avg, s.hr_avg,
          s.zone_status
        )
      );

      await c.env.DB.batch(snapshotStatements);
    }

    // Update device last_sync
    await c.env.DB
      .prepare(`UPDATE devices SET last_sync = datetime('now') WHERE id = ? AND user_id = ?`)
      .bind(deviceId, user.sub)
      .run();

    return c.json<ApiResponse>({
      success: true,
      data: {
        ride_id: String(rideId || 0),
        snapshots_count: snapshots?.length || 0,
        synced_at: new Date().toISOString(),
      },
      message: `Ride synced with ${snapshots?.length || 0} snapshots`,
    });

  } catch (err) {
    console.error('Error syncing ride with snapshots:', err);
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
            score,
            power_avg, power_max, cadence_avg, hr_avg, hr_max, speed_avg, distance_km,
            elevation_gain, elevation_loss, grade_avg, grade_max,
            normalized_power, energy_kj,
            created_at
          ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'))
        `).bind(
          user.sub, deviceId, ride.timestamp, ride.duration_ms,
          ride.balance_left, ride.balance_right,
          ride.te_left, ride.te_right,
          ride.ps_left, ride.ps_right,
          ride.zone_optimal, ride.zone_attention, ride.zone_problem,
          ride.score,
          ride.power_avg ?? 0, ride.power_max ?? 0, ride.cadence_avg ?? 0,
          ride.hr_avg ?? 0, ride.hr_max ?? 0, ride.speed_avg ?? 0, ride.distance_km ?? 0,
          ride.elevation_gain ?? 0, ride.elevation_loss ?? 0, ride.grade_avg ?? 0, ride.grade_max ?? 0,
          ride.normalized_power ?? 0, ride.energy_kj ?? 0
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

/**
 * POST /api/sync/drill
 * Sync single drill result from device
 */
sync.post('/drill', async (c) => {
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
      drill_id: string;
      drill_name: string;
      timestamp: number;
      duration_ms: number;
      score: number;
      time_in_target_ms: number;
      time_in_target_percent: number;
      completed: boolean;
      phase_scores_json?: string;
    }>();

    // Validate required fields
    if (!body.drill_id || !body.drill_name || !body.timestamp || body.score === undefined) {
      return c.json<ApiResponse>({ success: false, error: 'Missing required drill fields' }, 400);
    }

    // Check for duplicate
    const existing = await c.env.DB
      .prepare('SELECT id FROM drill_results WHERE user_id = ? AND device_id = ? AND drill_id = ? AND timestamp = ?')
      .bind(user.sub, deviceId, body.drill_id, body.timestamp)
      .first<{ id: number }>();

    if (existing) {
      return c.json<ApiResponse>({
        success: true,
        data: { drill_id: String(existing.id) },
        message: 'Drill already synced',
      });
    }

    // Insert drill result
    const result = await c.env.DB
      .prepare(`
        INSERT INTO drill_results (
          user_id, device_id, drill_id, drill_name, timestamp, duration_ms,
          score, time_in_target_ms, time_in_target_percent, completed, phase_scores_json, created_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'))
      `)
      .bind(
        user.sub, deviceId,
        sanitizeString(body.drill_id, 50),
        sanitizeString(body.drill_name, 100),
        body.timestamp,
        body.duration_ms,
        Math.round(body.score),
        body.time_in_target_ms || 0,
        body.time_in_target_percent || 0,
        body.completed ? 1 : 0,
        body.phase_scores_json || null
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
        drill_result_id: String(result.meta?.last_row_id || 0),
        synced_at: new Date().toISOString(),
      },
      message: 'Drill synced successfully',
    });

  } catch (err) {
    console.error('Error syncing drill:', err);
    return c.json<ApiResponse>({ success: false, error: 'Drill sync failed' }, 500);
  }
});

/**
 * POST /api/sync/drills
 * Batch sync drill results from device
 */
sync.post('/drills', async (c) => {
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
      drills: Array<{
        drill_id: string;
        drill_name: string;
        timestamp: number;
        duration_ms: number;
        score: number;
        time_in_target_ms: number;
        time_in_target_percent: number;
        completed: boolean;
        phase_scores_json?: string;
      }>;
    }>();

    const { drills } = body;

    if (!drills || !Array.isArray(drills) || drills.length === 0) {
      return c.json<ApiResponse>({ success: false, error: 'No drills provided' }, 400);
    }

    if (drills.length > 50) {
      return c.json<ApiResponse>({ success: false, error: 'Max 50 drills per batch' }, 400);
    }

    // Check for existing drills
    const timestamps = drills.map(d => d.timestamp);
    const placeholders = timestamps.map(() => '?').join(',');
    const existingResult = await c.env.DB
      .prepare(`SELECT timestamp FROM drill_results WHERE user_id = ? AND device_id = ? AND timestamp IN (${placeholders})`)
      .bind(user.sub, deviceId, ...timestamps)
      .all<{ timestamp: number }>();

    const existingTimestamps = new Set(existingResult.results.map(r => r.timestamp));

    // Filter out duplicates
    const newDrills = drills.filter(d => !existingTimestamps.has(d.timestamp));
    const duplicates = drills.length - newDrills.length;

    // Batch insert new drills
    if (newDrills.length > 0) {
      const insertStatements = newDrills.map(drill =>
        c.env.DB.prepare(`
          INSERT INTO drill_results (
            user_id, device_id, drill_id, drill_name, timestamp, duration_ms,
            score, time_in_target_ms, time_in_target_percent, completed, phase_scores_json, created_at
          ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'))
        `).bind(
          user.sub, deviceId,
          sanitizeString(drill.drill_id, 50),
          sanitizeString(drill.drill_name, 100),
          drill.timestamp,
          drill.duration_ms,
          Math.round(drill.score),
          drill.time_in_target_ms || 0,
          drill.time_in_target_percent || 0,
          drill.completed ? 1 : 0,
          drill.phase_scores_json || null
        )
      );

      await c.env.DB.batch(insertStatements);
    }

    // Update device last_sync
    await c.env.DB
      .prepare(`UPDATE devices SET last_sync = datetime('now') WHERE id = ? AND user_id = ?`)
      .bind(deviceId, user.sub)
      .run();

    return c.json<ApiResponse>({
      success: true,
      data: {
        inserted: newDrills.length,
        duplicates,
        total: drills.length,
      },
      message: `Synced ${newDrills.length} drills (${duplicates} duplicates skipped)`,
    });

  } catch (err) {
    console.error('Error syncing drills:', err);
    return c.json<ApiResponse>({ success: false, error: 'Drills sync failed' }, 500);
  }
});

/**
 * POST /api/sync/achievements
 * Batch sync achievements from device
 */
sync.post('/achievements', async (c) => {
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
      achievements: Array<{
        achievement_id: string;
        unlocked_at: number;
      }>;
    }>();

    const { achievements } = body;

    if (!achievements || !Array.isArray(achievements) || achievements.length === 0) {
      return c.json<ApiResponse>({ success: false, error: 'No achievements provided' }, 400);
    }

    if (achievements.length > 100) {
      return c.json<ApiResponse>({ success: false, error: 'Max 100 achievements per batch' }, 400);
    }

    // Check for existing achievements
    const achievementIds = achievements.map(a => a.achievement_id);
    const placeholders = achievementIds.map(() => '?').join(',');
    const existingResult = await c.env.DB
      .prepare(`SELECT achievement_id FROM achievements WHERE user_id = ? AND achievement_id IN (${placeholders})`)
      .bind(user.sub, ...achievementIds)
      .all<{ achievement_id: string }>();

    const existingIds = new Set(existingResult.results.map(r => r.achievement_id));

    // Filter out duplicates
    const newAchievements = achievements.filter(a => !existingIds.has(a.achievement_id));
    const duplicates = achievements.length - newAchievements.length;

    // Batch insert new achievements (using INSERT OR IGNORE for safety)
    if (newAchievements.length > 0) {
      const insertStatements = newAchievements.map(achievement =>
        c.env.DB.prepare(`
          INSERT OR IGNORE INTO achievements (user_id, achievement_id, unlocked_at, created_at)
          VALUES (?, ?, ?, datetime('now'))
        `).bind(
          user.sub,
          sanitizeString(achievement.achievement_id, 50),
          achievement.unlocked_at
        )
      );

      await c.env.DB.batch(insertStatements);
    }

    // Update device last_sync
    await c.env.DB
      .prepare(`UPDATE devices SET last_sync = datetime('now') WHERE id = ? AND user_id = ?`)
      .bind(deviceId, user.sub)
      .run();

    return c.json<ApiResponse>({
      success: true,
      data: {
        inserted: newAchievements.length,
        duplicates,
        total: achievements.length,
      },
      message: `Synced ${newAchievements.length} achievements (${duplicates} already unlocked)`,
    });

  } catch (err) {
    console.error('Error syncing achievements:', err);
    return c.json<ApiResponse>({ success: false, error: 'Achievements sync failed' }, 500);
  }
});

export { sync as syncRoutes };
