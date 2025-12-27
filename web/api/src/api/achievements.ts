import { Hono } from 'hono';
import { Env, ApiResponse, isDemoUser } from '../types/env';
import { getDemoCache, setDemoCache } from '../utils/demoCache';
import { roundObjectValues, roundArrayValues } from '../utils/format';

const achievements = new Hono<{ Bindings: Env }>();

// Types
interface AchievementData {
  id?: number;
  user_id?: string;
  achievement_id: string;
  unlocked_at: number;
  created_at?: string;
}

/**
 * GET /api/achievements
 * Get all achievements for authenticated user
 */
achievements.get('/', async (c) => {
  const user = c.get('user');

  try {
    const results = await c.env.DB
      .prepare(`
        SELECT id, user_id, achievement_id, unlocked_at, created_at
        FROM achievements
        WHERE user_id = ?
        ORDER BY unlocked_at DESC
      `)
      .bind(user.sub)
      .all<AchievementData>();

    return c.json<ApiResponse>({
      success: true,
      data: {
        achievements: results.results,
        total: results.results.length,
      },
    });
  } catch (err) {
    console.error('Error fetching achievements:', err);
    return c.json<ApiResponse>({ success: false, error: 'Failed to fetch achievements' }, 500);
  }
});

/**
 * GET /api/achievements/dashboard
 * Combined endpoint - achievements list + stats in ONE request
 * For demo users: KV cached for instant response
 */
achievements.get('/dashboard', async (c) => {
  const user = c.get('user');
  const cacheKey = 'achievements:dashboard';

  try {
    // Check KV cache for demo users (instant response)
    if (isDemoUser(user.sub)) {
      const cached = await getDemoCache<unknown>(c.env.SESSIONS, user.sub, cacheKey);
      if (cached) {
        c.header('X-Cache', 'HIT');
        return c.json<ApiResponse>({ success: true, data: cached });
      }
    }

    // Cache-Control for authenticated users (1 minute)
    c.header('Cache-Control', 'private, max-age=60');

    // Run ALL queries in a single batch - one D1 roundtrip
    const [achievementsResult, statsResult, recentResult] = await c.env.DB.batch([
      // 1. All achievements
      c.env.DB.prepare(`
        SELECT id, achievement_id, unlocked_at, created_at
        FROM achievements WHERE user_id = ?
        ORDER BY unlocked_at DESC
      `).bind(user.sub),

      // 2. Summary stats
      c.env.DB.prepare(`
        SELECT
          COUNT(*) as unlocked_count,
          MIN(unlocked_at) as first_unlocked_at,
          MAX(unlocked_at) as last_unlocked_at
        FROM achievements WHERE user_id = ?
      `).bind(user.sub),

      // 3. Recent (for quick display)
      c.env.DB.prepare(`
        SELECT achievement_id, unlocked_at
        FROM achievements WHERE user_id = ?
        ORDER BY unlocked_at DESC LIMIT 5
      `).bind(user.sub),
    ]);

    const responseData = {
      achievements: roundArrayValues(achievementsResult.results as object[]),
      total: achievementsResult.results.length,
      stats: {
        summary: roundObjectValues(statsResult.results[0] as object),
        recent: roundArrayValues(recentResult.results as object[]),
      },
    };

    // Cache response for demo users
    if (isDemoUser(user.sub)) {
      c.executionCtx.waitUntil(
        setDemoCache(c.env.SESSIONS, user.sub, cacheKey, responseData).catch(() => {})
      );
    }

    return c.json<ApiResponse>({ success: true, data: responseData });
  } catch (err) {
    console.error('Error fetching achievements dashboard:', err);
    return c.json<ApiResponse>({ success: false, error: 'Failed to fetch achievements' }, 500);
  }
});

/**
 * GET /api/achievements/stats
 * Get achievement statistics
 */
achievements.get('/stats', async (c) => {
  const user = c.get('user');

  try {
    // Get count of unlocked achievements
    const stats = await c.env.DB
      .prepare(`
        SELECT
          COUNT(*) as unlocked_count,
          MIN(unlocked_at) as first_unlocked_at,
          MAX(unlocked_at) as last_unlocked_at
        FROM achievements
        WHERE user_id = ?
      `)
      .bind(user.sub)
      .first();

    // Get recently unlocked (last 5)
    const recent = await c.env.DB
      .prepare(`
        SELECT achievement_id, unlocked_at
        FROM achievements
        WHERE user_id = ?
        ORDER BY unlocked_at DESC
        LIMIT 5
      `)
      .bind(user.sub)
      .all();

    return c.json<ApiResponse>({
      success: true,
      data: {
        summary: stats,
        recent: recent.results,
      },
    });
  } catch (err) {
    console.error('Error fetching achievement stats:', err);
    return c.json<ApiResponse>({ success: false, error: 'Failed to fetch stats' }, 500);
  }
});

export { achievements as achievementRoutes };
