import { Hono } from 'hono';
import { Env, ApiResponse } from '../types/env';
import { validatePagination } from '../middleware/validate';

const drills = new Hono<{ Bindings: Env }>();

// Types
interface DrillResult {
  id?: number;
  user_id?: string;
  device_id?: string;
  drill_id: string;
  drill_name: string;
  timestamp: number;
  duration_ms: number;
  score: number;
  time_in_target_ms?: number;
  time_in_target_percent?: number;
  completed: boolean;
  phase_scores_json?: string;
  created_at?: string;
}

/**
 * GET /api/drills
 * Get all drill results for authenticated user
 */
drills.get('/', async (c) => {
  const user = c.get('user');
  const { limit, offset } = validatePagination(
    c.req.query('limit'),
    c.req.query('offset')
  );
  const drillId = c.req.query('drill_id');

  try {
    let query = `
      SELECT id, user_id, device_id, drill_id, drill_name, timestamp,
             duration_ms, score, time_in_target_ms, time_in_target_percent,
             completed, phase_scores_json, created_at
      FROM drill_results
      WHERE user_id = ?
    `;
    const bindings: (string | number)[] = [user.sub];

    if (drillId) {
      query += ' AND drill_id = ?';
      bindings.push(drillId);
    }

    query += ' ORDER BY timestamp DESC LIMIT ? OFFSET ?';
    bindings.push(limit, offset);

    const [resultsResult, countResult] = await c.env.DB.batch([
      c.env.DB.prepare(query).bind(...bindings),
      c.env.DB.prepare(
        drillId
          ? 'SELECT COUNT(*) as count FROM drill_results WHERE user_id = ? AND drill_id = ?'
          : 'SELECT COUNT(*) as count FROM drill_results WHERE user_id = ?'
      ).bind(...(drillId ? [user.sub, drillId] : [user.sub])),
    ]);

    const results = resultsResult.results as DrillResult[];
    const total = (countResult.results[0] as { count: number })?.count || 0;

    return c.json<ApiResponse>({
      success: true,
      data: { drills: results, total, limit, offset },
    });
  } catch (err) {
    console.error('Error fetching drills:', err);
    return c.json<ApiResponse>({ success: false, error: 'Failed to fetch drills' }, 500);
  }
});

/**
 * GET /api/drills/dashboard
 * Combined endpoint - drills list + stats in ONE request
 */
drills.get('/dashboard', async (c) => {
  const user = c.get('user');
  const { limit, offset } = validatePagination(
    c.req.query('limit'),
    c.req.query('offset')
  );

  try {
    // Run ALL queries in a single batch - one D1 roundtrip
    const [drillsResult, countResult, summaryResult, perDrillResult] = await c.env.DB.batch([
      // 1. Recent drills
      c.env.DB.prepare(`
        SELECT id, drill_id, drill_name, timestamp, duration_ms, score,
               time_in_target_ms, time_in_target_percent, completed
        FROM drill_results WHERE user_id = ?
        ORDER BY timestamp DESC LIMIT ? OFFSET ?
      `).bind(user.sub, limit, offset),

      // 2. Total count
      c.env.DB.prepare('SELECT COUNT(*) as count FROM drill_results WHERE user_id = ?')
        .bind(user.sub),

      // 3. Summary stats
      c.env.DB.prepare(`
        SELECT
          COUNT(*) as total_drills,
          SUM(CASE WHEN completed = 1 THEN 1 ELSE 0 END) as completed_drills,
          COALESCE(AVG(score), 0) as avg_score,
          COALESCE(MAX(score), 0) as best_score,
          COALESCE(SUM(duration_ms), 0) as total_duration_ms,
          COUNT(DISTINCT drill_id) as drill_types_tried
        FROM drill_results WHERE user_id = ?
      `).bind(user.sub),

      // 4. Per-drill stats
      c.env.DB.prepare(`
        SELECT drill_id, drill_name, COUNT(*) as attempts,
               SUM(CASE WHEN completed = 1 THEN 1 ELSE 0 END) as completions,
               COALESCE(AVG(score), 0) as avg_score,
               COALESCE(MAX(score), 0) as best_score,
               MAX(timestamp) as last_attempt
        FROM drill_results WHERE user_id = ?
        GROUP BY drill_id ORDER BY last_attempt DESC
      `).bind(user.sub),
    ]);

    return c.json<ApiResponse>({
      success: true,
      data: {
        drills: drillsResult.results,
        total: (countResult.results[0] as { count: number })?.count || 0,
        limit,
        offset,
        stats: {
          summary: summaryResult.results[0],
          by_drill: perDrillResult.results,
        },
      },
    });
  } catch (err) {
    console.error('Error fetching drills dashboard:', err);
    return c.json<ApiResponse>({ success: false, error: 'Failed to fetch drills' }, 500);
  }
});

/**
 * GET /api/drills/stats
 * Get drill statistics
 */
drills.get('/stats', async (c) => {
  const user = c.get('user');

  try {
    const stats = await c.env.DB
      .prepare(`
        SELECT
          COUNT(*) as total_drills,
          SUM(CASE WHEN completed = 1 THEN 1 ELSE 0 END) as completed_drills,
          COALESCE(AVG(score), 0) as avg_score,
          COALESCE(MAX(score), 0) as best_score,
          COALESCE(SUM(duration_ms), 0) as total_duration_ms,
          COUNT(DISTINCT drill_id) as drill_types_tried
        FROM drill_results
        WHERE user_id = ?
      `)
      .bind(user.sub)
      .first();

    // Get stats per drill type
    const perDrillStats = await c.env.DB
      .prepare(`
        SELECT
          drill_id,
          drill_name,
          COUNT(*) as attempts,
          SUM(CASE WHEN completed = 1 THEN 1 ELSE 0 END) as completions,
          COALESCE(AVG(score), 0) as avg_score,
          COALESCE(MAX(score), 0) as best_score,
          MAX(timestamp) as last_attempt
        FROM drill_results
        WHERE user_id = ?
        GROUP BY drill_id
        ORDER BY last_attempt DESC
      `)
      .bind(user.sub)
      .all();

    return c.json<ApiResponse>({
      success: true,
      data: {
        summary: stats,
        by_drill: perDrillStats.results,
      },
    });
  } catch (err) {
    console.error('Error fetching drill stats:', err);
    return c.json<ApiResponse>({ success: false, error: 'Failed to fetch stats' }, 500);
  }
});

export { drills as drillRoutes };
