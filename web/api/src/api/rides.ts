import { Hono } from 'hono';
import { Env, ApiResponse, RideData } from '../types/env';

const rides = new Hono<{ Bindings: Env }>();

/**
 * GET /api/rides
 * Get all rides for authenticated user
 */
rides.get('/', async (c) => {
  const user = c.get('user');
  const limit = parseInt(c.req.query('limit') || '50');
  const offset = parseInt(c.req.query('offset') || '0');

  try {
    const result = await c.env.DB
      .prepare(`
        SELECT * FROM rides
        WHERE user_id = ?
        ORDER BY timestamp DESC
        LIMIT ? OFFSET ?
      `)
      .bind(user.sub, limit, offset)
      .all<RideData>();

    const count = await c.env.DB
      .prepare('SELECT COUNT(*) as count FROM rides WHERE user_id = ?')
      .bind(user.sub)
      .first<{ count: number }>();

    return c.json<ApiResponse>({
      success: true,
      data: {
        rides: result.results,
        total: count?.count || 0,
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
      .prepare('SELECT * FROM rides WHERE id = ? AND user_id = ?')
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
          COALESCE(AVG(zone_optimal), 0) as avg_zone_optimal
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
 * GET /api/rides/stats/trends
 * Get trends over time (last 30 days)
 */
rides.get('/stats/trends', async (c) => {
  const user = c.get('user');
  const days = parseInt(c.req.query('days') || '30');

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
