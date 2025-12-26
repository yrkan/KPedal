import { Hono } from 'hono';
import { Env, ApiResponse, RideData } from '../types/env';
import { validatePagination, validateDays } from '../middleware/validate';

const rides = new Hono<{ Bindings: Env }>();

/**
 * GET /api/rides
 * Get all rides for authenticated user
 */
rides.get('/', async (c) => {
  const user = c.get('user');
  const { limit, offset } = validatePagination(
    c.req.query('limit'),
    c.req.query('offset')
  );
  const sortParam = c.req.query('sort') || 'date';

  // Map sort param to ORDER BY clause (prevent SQL injection)
  const sortMap: Record<string, string> = {
    'date': 'timestamp DESC',
    'score': 'score DESC',
    'optimal': 'zone_optimal DESC'
  };
  const orderBy = sortMap[sortParam] || 'timestamp DESC';

  try {
    // Run both queries in a single batch request for efficiency
    const [ridesResult, countResult] = await c.env.DB.batch([
      c.env.DB
        .prepare(`
          SELECT id, user_id, device_id, timestamp, duration_ms,
                 balance_left, balance_right, te_left, te_right,
                 ps_left, ps_right, zone_optimal, zone_attention,
                 zone_problem, score,
                 power_avg, power_max, cadence_avg, hr_avg, hr_max, speed_avg, distance_km,
                 elevation_gain, elevation_loss, grade_avg, grade_max,
                 normalized_power, energy_kj,
                 created_at
          FROM rides
          WHERE user_id = ?
          ORDER BY ${orderBy}
          LIMIT ? OFFSET ?
        `)
        .bind(user.sub, limit, offset),
      c.env.DB
        .prepare('SELECT COUNT(*) as count FROM rides WHERE user_id = ?')
        .bind(user.sub),
    ]);

    const rides = ridesResult.results as RideData[];
    const total = (countResult.results[0] as { count: number })?.count || 0;

    return c.json<ApiResponse>({
      success: true,
      data: {
        rides,
        total,
        limit,
        offset,
      },
    });
  } catch (err) {
    console.error('Error fetching rides:', err);
    return c.json<ApiResponse>({ success: false, error: 'Failed to fetch rides' }, 500);
  }
});

/**
 * GET /api/rides/stats/summary
 * Get aggregated statistics
 * NOTE: Must be defined BEFORE /:id route to avoid matching "stats" as id
 */
rides.get('/stats/summary', async (c) => {
  const user = c.get('user');

  try {
    const stats = await c.env.DB
      .prepare(`
        SELECT
          COUNT(*) as total_rides,
          COALESCE(AVG(balance_left), 50) as avg_balance_left,
          COALESCE(AVG(balance_right), 50) as avg_balance_right,
          COALESCE(AVG(te_left), 0) as avg_te_left,
          COALESCE(AVG(te_right), 0) as avg_te_right,
          COALESCE(AVG(ps_left), 0) as avg_ps_left,
          COALESCE(AVG(ps_right), 0) as avg_ps_right,
          COALESCE(AVG(score), 0) as avg_score,
          COALESCE(SUM(duration_ms), 0) as total_duration_ms,
          COALESCE(AVG(zone_optimal), 0) as avg_zone_optimal,
          COALESCE(AVG(zone_attention), 0) as avg_zone_attention,
          COALESCE(AVG(zone_problem), 0) as avg_zone_problem,
          COALESCE(MAX(score), 0) as best_score,
          COALESCE(MIN(ABS(balance_left - 50)), 50) as best_balance_diff,
          COALESCE(AVG(power_avg), 0) as avg_power,
          COALESCE(MAX(power_max), 0) as max_power,
          COALESCE(AVG(cadence_avg), 0) as avg_cadence,
          COALESCE(AVG(hr_avg), 0) as avg_hr,
          COALESCE(MAX(hr_max), 0) as max_hr,
          COALESCE(AVG(speed_avg), 0) as avg_speed,
          COALESCE(SUM(distance_km), 0) as total_distance_km,
          COALESCE(SUM(elevation_gain), 0) as total_elevation_gain,
          COALESCE(SUM(elevation_loss), 0) as total_elevation_loss,
          COALESCE(AVG(grade_avg), 0) as avg_grade,
          COALESCE(MAX(grade_max), 0) as max_grade,
          COALESCE(AVG(normalized_power), 0) as avg_normalized_power,
          COALESCE(SUM(energy_kj), 0) as total_energy_kj
        FROM rides
        WHERE user_id = ?
      `)
      .bind(user.sub)
      .first();

    return c.json<ApiResponse>({ success: true, data: stats });
  } catch (err) {
    console.error('Error fetching stats:', err);
    return c.json<ApiResponse>({ success: false, error: 'Failed to fetch stats' }, 500);
  }
});

/**
 * GET /api/rides/stats/weekly
 * Get this week vs last week comparison
 */
rides.get('/stats/weekly', async (c) => {
  const user = c.get('user');

  try {
    // Get this week and last week stats
    const now = Date.now();
    const oneWeekMs = 7 * 24 * 60 * 60 * 1000;
    const thisWeekStart = now - oneWeekMs;
    const lastWeekStart = thisWeekStart - oneWeekMs;

    const [thisWeekResult, lastWeekResult] = await c.env.DB.batch([
      c.env.DB.prepare(`
        SELECT
          COUNT(*) as rides_count,
          COALESCE(AVG(score), 0) as avg_score,
          COALESCE(AVG(balance_left), 50) as avg_balance_left,
          COALESCE(AVG(te_left + te_right) / 2, 0) as avg_te,
          COALESCE(AVG(ps_left + ps_right) / 2, 0) as avg_ps,
          COALESCE(AVG(zone_optimal), 0) as avg_zone_optimal,
          COALESCE(SUM(duration_ms), 0) as total_duration_ms,
          COALESCE(AVG(power_avg), 0) as avg_power,
          COALESCE(SUM(distance_km), 0) as total_distance_km,
          COALESCE(SUM(elevation_gain), 0) as total_elevation,
          COALESCE(AVG(normalized_power), 0) as avg_normalized_power,
          COALESCE(SUM(energy_kj), 0) as total_energy_kj
        FROM rides
        WHERE user_id = ? AND timestamp >= ?
      `).bind(user.sub, thisWeekStart),
      c.env.DB.prepare(`
        SELECT
          COUNT(*) as rides_count,
          COALESCE(AVG(score), 0) as avg_score,
          COALESCE(AVG(balance_left), 50) as avg_balance_left,
          COALESCE(AVG(te_left + te_right) / 2, 0) as avg_te,
          COALESCE(AVG(ps_left + ps_right) / 2, 0) as avg_ps,
          COALESCE(AVG(zone_optimal), 0) as avg_zone_optimal,
          COALESCE(SUM(duration_ms), 0) as total_duration_ms,
          COALESCE(AVG(power_avg), 0) as avg_power,
          COALESCE(SUM(distance_km), 0) as total_distance_km,
          COALESCE(SUM(elevation_gain), 0) as total_elevation,
          COALESCE(AVG(normalized_power), 0) as avg_normalized_power,
          COALESCE(SUM(energy_kj), 0) as total_energy_kj
        FROM rides
        WHERE user_id = ? AND timestamp >= ? AND timestamp < ?
      `).bind(user.sub, lastWeekStart, thisWeekStart),
    ]);

    const thisWeek = thisWeekResult.results[0] as any;
    const lastWeek = lastWeekResult.results[0] as any;

    // Calculate changes
    const scoreChange = thisWeek.avg_score - lastWeek.avg_score;
    const zoneChange = thisWeek.avg_zone_optimal - lastWeek.avg_zone_optimal;
    const ridesChange = thisWeek.rides_count - lastWeek.rides_count;
    const powerChange = thisWeek.avg_power - lastWeek.avg_power;
    const distanceChange = thisWeek.total_distance_km - lastWeek.total_distance_km;
    const durationChange = thisWeek.total_duration_ms - lastWeek.total_duration_ms;
    const elevationChange = thisWeek.total_elevation - lastWeek.total_elevation;
    const teChange = thisWeek.avg_te - lastWeek.avg_te;
    const psChange = thisWeek.avg_ps - lastWeek.avg_ps;
    const balanceChange = Math.abs(thisWeek.avg_balance_left - 50) - Math.abs(lastWeek.avg_balance_left - 50);
    const energyChange = thisWeek.total_energy_kj - lastWeek.total_energy_kj;

    return c.json<ApiResponse>({
      success: true,
      data: {
        thisWeek,
        lastWeek,
        changes: {
          score: scoreChange,
          zone_optimal: zoneChange,
          rides_count: ridesChange,
          power: powerChange,
          distance: distanceChange,
          duration: durationChange,
          elevation: elevationChange,
          te: teChange,
          ps: psChange,
          balance: balanceChange, // negative is better (closer to 50%)
          energy: energyChange,
        },
      },
    });
  } catch (err) {
    console.error('Error fetching weekly stats:', err);
    return c.json<ApiResponse>({ success: false, error: 'Failed to fetch weekly stats' }, 500);
  }
});

/**
 * GET /api/rides/stats/trends
 * Get trends over time (last 30 days)
 */
rides.get('/stats/trends', async (c) => {
  const user = c.get('user');
  const days = validateDays(c.req.query('days'));

  try {
    const trends = await c.env.DB
      .prepare(`
        SELECT
          date(timestamp / 1000, 'unixepoch') as date,
          COUNT(*) as rides_count,
          AVG(balance_left) as avg_balance_left,
          AVG(te_left + te_right) / 2 as avg_te,
          AVG(ps_left + ps_right) / 2 as avg_ps,
          AVG(score) as avg_score,
          AVG(zone_optimal) as avg_zone_optimal
        FROM rides
        WHERE user_id = ?
          AND timestamp > (strftime('%s', 'now') - ? * 86400) * 1000
        GROUP BY date(timestamp / 1000, 'unixepoch')
        ORDER BY date DESC
      `)
      .bind(user.sub, days)
      .all();

    return c.json<ApiResponse>({ success: true, data: trends.results });
  } catch (err) {
    console.error('Error fetching trends:', err);
    return c.json<ApiResponse>({ success: false, error: 'Failed to fetch trends' }, 500);
  }
});

/**
 * GET /api/rides/dashboard
 * Combined endpoint for dashboard - all data in ONE D1 batch request
 * Reduces 6 API calls to 1, saving ~1500ms of latency
 * NOTE: Must be defined BEFORE /:id route
 */
rides.get('/dashboard', async (c) => {
  const user = c.get('user');

  try {
    const now = Date.now();
    const oneWeekMs = 7 * 24 * 60 * 60 * 1000;
    const thisWeekStart = now - oneWeekMs;
    const lastWeekStart = thisWeekStart - oneWeekMs;

    // Run ALL queries in a single batch - one D1 roundtrip
    const [
      statsResult,
      recentRidesResult,
      thisWeekResult,
      lastWeekResult,
      trendsResult,
      lastRideSnapshotsResult,
    ] = await c.env.DB.batch([
      // 1. Summary stats
      c.env.DB.prepare(`
        SELECT
          COUNT(*) as total_rides,
          COALESCE(AVG(balance_left), 50) as avg_balance_left,
          COALESCE(AVG(balance_right), 50) as avg_balance_right,
          COALESCE(AVG(te_left), 0) as avg_te_left,
          COALESCE(AVG(te_right), 0) as avg_te_right,
          COALESCE(AVG(ps_left), 0) as avg_ps_left,
          COALESCE(AVG(ps_right), 0) as avg_ps_right,
          COALESCE(AVG(score), 0) as avg_score,
          COALESCE(SUM(duration_ms), 0) as total_duration_ms,
          COALESCE(AVG(zone_optimal), 0) as avg_zone_optimal,
          COALESCE(AVG(zone_attention), 0) as avg_zone_attention,
          COALESCE(AVG(zone_problem), 0) as avg_zone_problem,
          COALESCE(MAX(score), 0) as best_score,
          COALESCE(MIN(ABS(balance_left - 50)), 50) as best_balance_diff,
          COALESCE(AVG(power_avg), 0) as avg_power,
          COALESCE(MAX(power_max), 0) as max_power,
          COALESCE(AVG(cadence_avg), 0) as avg_cadence,
          COALESCE(AVG(hr_avg), 0) as avg_hr,
          COALESCE(SUM(distance_km), 0) as total_distance_km
        FROM rides WHERE user_id = ?
      `).bind(user.sub),

      // 2. Recent rides (for list + fatigue analysis)
      c.env.DB.prepare(`
        SELECT id, timestamp, duration_ms, balance_left, balance_right,
               te_left, te_right, ps_left, ps_right,
               zone_optimal, zone_attention, zone_problem, score,
               power_avg, cadence_avg, hr_avg, distance_km
        FROM rides WHERE user_id = ?
        ORDER BY timestamp DESC LIMIT 10
      `).bind(user.sub),

      // 3. This week stats
      c.env.DB.prepare(`
        SELECT
          COUNT(*) as rides_count,
          COALESCE(AVG(score), 0) as avg_score,
          COALESCE(AVG(balance_left), 50) as avg_balance_left,
          COALESCE(AVG(te_left + te_right) / 2, 0) as avg_te,
          COALESCE(AVG(ps_left + ps_right) / 2, 0) as avg_ps,
          COALESCE(AVG(zone_optimal), 0) as avg_zone_optimal,
          COALESCE(SUM(duration_ms), 0) as total_duration_ms,
          COALESCE(AVG(power_avg), 0) as avg_power,
          COALESCE(SUM(distance_km), 0) as total_distance_km,
          COALESCE(SUM(elevation_gain), 0) as total_elevation,
          COALESCE(SUM(energy_kj), 0) as total_energy_kj
        FROM rides WHERE user_id = ? AND timestamp >= ?
      `).bind(user.sub, thisWeekStart),

      // 4. Last week stats
      c.env.DB.prepare(`
        SELECT
          COUNT(*) as rides_count,
          COALESCE(AVG(score), 0) as avg_score,
          COALESCE(AVG(balance_left), 50) as avg_balance_left,
          COALESCE(AVG(te_left + te_right) / 2, 0) as avg_te,
          COALESCE(AVG(ps_left + ps_right) / 2, 0) as avg_ps,
          COALESCE(AVG(zone_optimal), 0) as avg_zone_optimal,
          COALESCE(SUM(duration_ms), 0) as total_duration_ms,
          COALESCE(AVG(power_avg), 0) as avg_power,
          COALESCE(SUM(distance_km), 0) as total_distance_km,
          COALESCE(SUM(elevation_gain), 0) as total_elevation,
          COALESCE(SUM(energy_kj), 0) as total_energy_kj
        FROM rides WHERE user_id = ? AND timestamp >= ? AND timestamp < ?
      `).bind(user.sub, lastWeekStart, thisWeekStart),

      // 5. Trends (last 30 days)
      c.env.DB.prepare(`
        SELECT
          date(timestamp / 1000, 'unixepoch') as date,
          COUNT(*) as rides_count,
          AVG(balance_left) as avg_balance_left,
          AVG(te_left + te_right) / 2 as avg_te,
          AVG(ps_left + ps_right) / 2 as avg_ps,
          AVG(score) as avg_score,
          AVG(zone_optimal) as avg_zone_optimal
        FROM rides
        WHERE user_id = ? AND timestamp > (strftime('%s', 'now') - 30 * 86400) * 1000
        GROUP BY date(timestamp / 1000, 'unixepoch')
        ORDER BY date DESC
      `).bind(user.sub),

      // 6. Snapshots for the latest ride (for fatigue analysis)
      c.env.DB.prepare(`
        SELECT minute_index, balance_left, balance_right,
               te_left, te_right, ps_left, ps_right, power_avg
        FROM ride_snapshots
        WHERE ride_id = (SELECT id FROM rides WHERE user_id = ? ORDER BY timestamp DESC LIMIT 1)
        ORDER BY minute_index ASC
      `).bind(user.sub),
    ]);

    const stats = statsResult.results[0];
    const recentRides = recentRidesResult.results;
    const thisWeek = thisWeekResult.results[0] as any;
    const lastWeek = lastWeekResult.results[0] as any;
    const trends = trendsResult.results;
    const lastRideSnapshots = lastRideSnapshotsResult.results;

    // Calculate weekly changes
    const thisWeekAsymmetry = Math.abs((thisWeek?.avg_balance_left || 50) - 50);
    const lastWeekAsymmetry = Math.abs((lastWeek?.avg_balance_left || 50) - 50);
    const changes = {
      score: (thisWeek?.avg_score || 0) - (lastWeek?.avg_score || 0),
      zone_optimal: (thisWeek?.avg_zone_optimal || 0) - (lastWeek?.avg_zone_optimal || 0),
      rides_count: (thisWeek?.rides_count || 0) - (lastWeek?.rides_count || 0),
      power: (thisWeek?.avg_power || 0) - (lastWeek?.avg_power || 0),
      distance: (thisWeek?.total_distance_km || 0) - (lastWeek?.total_distance_km || 0),
      duration: (thisWeek?.total_duration_ms || 0) - (lastWeek?.total_duration_ms || 0),
      te: (thisWeek?.avg_te || 0) - (lastWeek?.avg_te || 0),
      ps: (thisWeek?.avg_ps || 0) - (lastWeek?.avg_ps || 0),
      balance: thisWeekAsymmetry - lastWeekAsymmetry, // Positive = worse asymmetry
    };

    return c.json<ApiResponse>({
      success: true,
      data: {
        stats,
        recentRides,
        weeklyComparison: { thisWeek, lastWeek, changes },
        trends,
        lastRideSnapshots, // For fatigue analysis
      },
    });
  } catch (err) {
    console.error('Error fetching dashboard:', err);
    return c.json<ApiResponse>({ success: false, error: 'Failed to fetch dashboard' }, 500);
  }
});

/**
 * GET /api/rides/:id
 * Get single ride by ID (with snapshots)
 * NOTE: Must be defined AFTER /stats/* and /dashboard routes
 */
rides.get('/:id', async (c) => {
  const user = c.get('user');
  const id = c.req.param('id');

  try {
    // Run both queries in parallel
    const [rideResult, snapshotsResult] = await c.env.DB.batch([
      c.env.DB
        .prepare(`
          SELECT id, user_id, device_id, timestamp, duration_ms,
                 balance_left, balance_right, te_left, te_right,
                 ps_left, ps_right, zone_optimal, zone_attention,
                 zone_problem, score,
                 power_avg, power_max, cadence_avg, hr_avg, hr_max, speed_avg, distance_km,
                 elevation_gain, elevation_loss, grade_avg, grade_max,
                 normalized_power, energy_kj,
                 notes, rating, created_at
          FROM rides WHERE id = ? AND user_id = ?
        `)
        .bind(id, user.sub),
      c.env.DB
        .prepare(`
          SELECT minute_index, timestamp, balance_left, balance_right,
                 te_left, te_right, ps_left, ps_right,
                 power_avg, cadence_avg, hr_avg, zone_status
          FROM ride_snapshots
          WHERE ride_id = ?
          ORDER BY minute_index ASC
        `)
        .bind(id),
    ]);

    const ride = rideResult.results[0] as RideData | undefined;

    if (!ride) {
      return c.json<ApiResponse>({ success: false, error: 'Ride not found' }, 404);
    }

    const snapshots = snapshotsResult.results;

    return c.json<ApiResponse>({
      success: true,
      data: {
        ...ride,
        snapshots,
      },
    });
  } catch (err) {
    console.error('Error fetching ride:', err);
    return c.json<ApiResponse>({ success: false, error: 'Failed to fetch ride' }, 500);
  }
});

/**
 * DELETE /api/rides/:id
 * Delete a ride
 */
rides.delete('/:id', async (c) => {
  const user = c.get('user');
  const id = c.req.param('id');

  try {
    const result = await c.env.DB
      .prepare('DELETE FROM rides WHERE id = ? AND user_id = ?')
      .bind(id, user.sub)
      .run();

    if (result.meta.changes === 0) {
      return c.json<ApiResponse>({ success: false, error: 'Ride not found' }, 404);
    }

    return c.json<ApiResponse>({ success: true, message: 'Ride deleted' });
  } catch (err) {
    console.error('Error deleting ride:', err);
    return c.json<ApiResponse>({ success: false, error: 'Failed to delete ride' }, 500);
  }
});

export { rides as ridesRoutes };
