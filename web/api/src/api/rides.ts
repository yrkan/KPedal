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
                 zone_problem, score, created_at
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
 * GET /api/rides/:id
 * Get single ride by ID
 */
rides.get('/:id', async (c) => {
  const user = c.get('user');
  const id = c.req.param('id');

  try {
    const ride = await c.env.DB
      .prepare(`
        SELECT id, user_id, device_id, timestamp, duration_ms,
               balance_left, balance_right, te_left, te_right,
               ps_left, ps_right, zone_optimal, zone_attention,
               zone_problem, score, notes, rating, created_at
        FROM rides WHERE id = ? AND user_id = ?
      `)
      .bind(id, user.sub)
      .first<RideData>();

    if (!ride) {
      return c.json<ApiResponse>({ success: false, error: 'Ride not found' }, 404);
    }

    return c.json<ApiResponse>({ success: true, data: ride });
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

/**
 * GET /api/rides/stats/summary
 * Get aggregated statistics
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
          COALESCE(MIN(ABS(balance_left - 50)), 50) as best_balance_diff
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
          COALESCE(SUM(duration_ms), 0) as total_duration_ms
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
          COALESCE(SUM(duration_ms), 0) as total_duration_ms
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

    return c.json<ApiResponse>({
      success: true,
      data: {
        thisWeek,
        lastWeek,
        changes: {
          score: scoreChange,
          zone_optimal: zoneChange,
          rides_count: ridesChange,
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

export { rides as ridesRoutes };
