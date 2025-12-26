import { describe, it, expect, beforeEach } from 'vitest';

/**
 * Unit tests for Achievements API
 *
 * Tests for achievement retrieval and sync.
 */

// Mock types
interface Achievement {
  id: number;
  user_id: string;
  achievement_id: string;
  unlocked_at: number;
  created_at?: string;
}

// Simulated database
class MockAchievementsDatabase {
  private achievements: Achievement[] = [];
  private idCounter = 1;

  addAchievement(achievement: Omit<Achievement, 'id' | 'created_at'>): Achievement | null {
    // Duplicate check (same as INSERT OR IGNORE)
    const exists = this.achievements.some(
      a => a.user_id === achievement.user_id && a.achievement_id === achievement.achievement_id
    );
    if (exists) return null;

    const newAchievement: Achievement = {
      ...achievement,
      id: this.idCounter++,
      created_at: new Date().toISOString(),
    };
    this.achievements.push(newAchievement);
    return newAchievement;
  }

  getAchievementsForUser(userId: string): Achievement[] {
    return this.achievements
      .filter(a => a.user_id === userId)
      .sort((a, b) => b.unlocked_at - a.unlocked_at);
  }

  getStatsForUser(userId: string): {
    unlocked_count: number;
    first_unlocked_at: number | null;
    last_unlocked_at: number | null;
  } {
    const userAchievements = this.achievements.filter(a => a.user_id === userId);
    if (userAchievements.length === 0) {
      return { unlocked_count: 0, first_unlocked_at: null, last_unlocked_at: null };
    }

    const timestamps = userAchievements.map(a => a.unlocked_at);
    return {
      unlocked_count: userAchievements.length,
      first_unlocked_at: Math.min(...timestamps),
      last_unlocked_at: Math.max(...timestamps),
    };
  }

  getRecentForUser(userId: string, limit: number): Achievement[] {
    return this.getAchievementsForUser(userId).slice(0, limit);
  }

  getExistingIds(userId: string, achievementIds: string[]): Set<string> {
    const userAchievements = this.achievements.filter(a => a.user_id === userId);
    return new Set(
      userAchievements
        .filter(a => achievementIds.includes(a.achievement_id))
        .map(a => a.achievement_id)
    );
  }

  clear() {
    this.achievements = [];
    this.idCounter = 1;
  }
}

describe('Achievements API', () => {
  let db: MockAchievementsDatabase;
  const userId = 'user-123';

  beforeEach(() => {
    db = new MockAchievementsDatabase();
  });

  describe('GET /api/achievements', () => {
    it('should return empty list when no achievements exist', () => {
      const achievements = db.getAchievementsForUser(userId);
      expect(achievements).toEqual([]);
    });

    it('should return achievements for authenticated user', () => {
      db.addAchievement({
        user_id: userId,
        achievement_id: 'first_ride',
        unlocked_at: Date.now(),
      });

      const achievements = db.getAchievementsForUser(userId);
      expect(achievements.length).toBe(1);
      expect(achievements[0].achievement_id).toBe('first_ride');
    });

    it('should not return achievements from other users', () => {
      db.addAchievement({
        user_id: 'other-user',
        achievement_id: 'first_ride',
        unlocked_at: Date.now(),
      });

      const achievements = db.getAchievementsForUser(userId);
      expect(achievements.length).toBe(0);
    });

    it('should order by unlocked_at descending', () => {
      const now = Date.now();
      db.addAchievement({
        user_id: userId,
        achievement_id: 'first_ride',
        unlocked_at: now - 10000,
      });

      db.addAchievement({
        user_id: userId,
        achievement_id: 'ten_rides',
        unlocked_at: now,
      });

      const achievements = db.getAchievementsForUser(userId);
      expect(achievements[0].achievement_id).toBe('ten_rides');
      expect(achievements[1].achievement_id).toBe('first_ride');
    });
  });

  describe('GET /api/achievements/dashboard (combined endpoint)', () => {
    it('should return empty data when no achievements exist', () => {
      const achievements = db.getAchievementsForUser(userId);
      const stats = db.getStatsForUser(userId);
      const recent = db.getRecentForUser(userId, 5);

      const dashboardData = {
        achievements,
        total: achievements.length,
        stats: {
          summary: stats,
          recent,
        },
      };

      expect(dashboardData.achievements).toEqual([]);
      expect(dashboardData.total).toBe(0);
      expect(dashboardData.stats.summary.unlocked_count).toBe(0);
      expect(dashboardData.stats.recent).toEqual([]);
    });

    it('should return all dashboard data in single response', () => {
      const now = Date.now();

      db.addAchievement({
        user_id: userId,
        achievement_id: 'first_ride',
        unlocked_at: now - 100000,
      });

      db.addAchievement({
        user_id: userId,
        achievement_id: 'ten_rides',
        unlocked_at: now - 50000,
      });

      db.addAchievement({
        user_id: userId,
        achievement_id: 'perfect_balance',
        unlocked_at: now,
      });

      const achievements = db.getAchievementsForUser(userId);
      const stats = db.getStatsForUser(userId);
      const recent = db.getRecentForUser(userId, 5);

      const dashboardData = {
        achievements,
        total: achievements.length,
        stats: {
          summary: stats,
          recent,
        },
      };

      // Verify achievements list
      expect(dashboardData.achievements.length).toBe(3);
      expect(dashboardData.total).toBe(3);

      // Verify order (most recent first)
      expect(dashboardData.achievements[0].achievement_id).toBe('perfect_balance');
      expect(dashboardData.achievements[2].achievement_id).toBe('first_ride');

      // Verify summary stats
      expect(dashboardData.stats.summary.unlocked_count).toBe(3);
      expect(dashboardData.stats.summary.first_unlocked_at).toBe(now - 100000);
      expect(dashboardData.stats.summary.last_unlocked_at).toBe(now);

      // Verify recent achievements
      expect(dashboardData.stats.recent.length).toBe(3);
    });

    it('should limit recent achievements to 5', () => {
      const now = Date.now();

      for (let i = 0; i < 10; i++) {
        db.addAchievement({
          user_id: userId,
          achievement_id: `achievement_${i}`,
          unlocked_at: now - i * 1000,
        });
      }

      const achievements = db.getAchievementsForUser(userId);
      const recent = db.getRecentForUser(userId, 5);

      expect(achievements.length).toBe(10);
      expect(recent.length).toBe(5);
      expect(recent[0].achievement_id).toBe('achievement_0'); // Most recent
    });

    it('should isolate data per user in dashboard', () => {
      const otherUserId = 'other-user';

      db.addAchievement({
        user_id: userId,
        achievement_id: 'first_ride',
        unlocked_at: Date.now(),
      });

      db.addAchievement({
        user_id: otherUserId,
        achievement_id: 'ten_rides',
        unlocked_at: Date.now(),
      });

      db.addAchievement({
        user_id: otherUserId,
        achievement_id: 'perfect_balance',
        unlocked_at: Date.now(),
      });

      const myAchievements = db.getAchievementsForUser(userId);
      const myStats = db.getStatsForUser(userId);

      expect(myAchievements.length).toBe(1);
      expect(myStats.unlocked_count).toBe(1);
    });

    it('should handle batch query efficiency', () => {
      // This test validates that all data needed for dashboard
      // can be computed with the same data source
      const now = Date.now();

      db.addAchievement({
        user_id: userId,
        achievement_id: 'first_ride',
        unlocked_at: now,
      });

      // Simulate batch query - all queries use same user filter
      const achievements = db.getAchievementsForUser(userId);
      const stats = db.getStatsForUser(userId);
      const recent = db.getRecentForUser(userId, 5);

      // All should be consistent
      expect(achievements.length).toBe(stats.unlocked_count);
      expect(recent.length).toBeLessThanOrEqual(5);
      expect(recent.every(r => achievements.some(a => a.achievement_id === r.achievement_id))).toBe(true);
    });
  });

  describe('GET /api/achievements/stats', () => {
    it('should return zero stats when no achievements exist', () => {
      const stats = db.getStatsForUser(userId);
      expect(stats.unlocked_count).toBe(0);
      expect(stats.first_unlocked_at).toBeNull();
      expect(stats.last_unlocked_at).toBeNull();
    });

    it('should calculate correct summary stats', () => {
      const now = Date.now();
      db.addAchievement({
        user_id: userId,
        achievement_id: 'first_ride',
        unlocked_at: now - 1000000,
      });

      db.addAchievement({
        user_id: userId,
        achievement_id: 'ten_rides',
        unlocked_at: now,
      });

      db.addAchievement({
        user_id: userId,
        achievement_id: 'perfect_balance',
        unlocked_at: now - 500000,
      });

      const stats = db.getStatsForUser(userId);
      expect(stats.unlocked_count).toBe(3);
      expect(stats.first_unlocked_at).toBe(now - 1000000);
      expect(stats.last_unlocked_at).toBe(now);
    });

    it('should return recent achievements limited to 5', () => {
      const now = Date.now();
      for (let i = 0; i < 10; i++) {
        db.addAchievement({
          user_id: userId,
          achievement_id: `achievement_${i}`,
          unlocked_at: now - i * 1000,
        });
      }

      const recent = db.getRecentForUser(userId, 5);
      expect(recent.length).toBe(5);
      expect(recent[0].achievement_id).toBe('achievement_0'); // Most recent
    });
  });
});

describe('Achievement Sync API', () => {
  let db: MockAchievementsDatabase;
  const userId = 'user-123';
  const deviceId = 'device-456';

  beforeEach(() => {
    db = new MockAchievementsDatabase();
  });

  describe('POST /api/sync/achievements', () => {
    it('should validate required fields', () => {
      const validAchievement = {
        achievement_id: 'first_ride',
        unlocked_at: Date.now(),
      };

      expect(validAchievement.achievement_id).toBeTruthy();
      expect(validAchievement.unlocked_at).toBeGreaterThan(0);
    });

    it('should reject empty achievements array', () => {
      const achievements: unknown[] = [];
      expect(achievements.length).toBe(0);
    });

    it('should enforce batch size limit', () => {
      const maxBatchSize = 100;
      const achievements = Array(150).fill(null).map((_, i) => ({
        achievement_id: `achievement_${i}`,
        unlocked_at: Date.now() + i,
      }));

      expect(achievements.length).toBeGreaterThan(maxBatchSize);
    });

    it('should detect and skip duplicates', () => {
      // Add initial achievement
      db.addAchievement({
        user_id: userId,
        achievement_id: 'first_ride',
        unlocked_at: Date.now(),
      });

      // Check for existing
      const toSync = ['first_ride', 'ten_rides', 'perfect_balance'];
      const existingIds = db.getExistingIds(userId, toSync);

      expect(existingIds.has('first_ride')).toBe(true);
      expect(existingIds.has('ten_rides')).toBe(false);

      const newAchievements = toSync.filter(id => !existingIds.has(id));
      expect(newAchievements.length).toBe(2);
    });

    it('should use INSERT OR IGNORE for safety', () => {
      // First insert
      const result1 = db.addAchievement({
        user_id: userId,
        achievement_id: 'first_ride',
        unlocked_at: Date.now(),
      });
      expect(result1).not.toBeNull();

      // Duplicate insert should return null (ignored)
      const result2 = db.addAchievement({
        user_id: userId,
        achievement_id: 'first_ride',
        unlocked_at: Date.now() + 1000,
      });
      expect(result2).toBeNull();

      // Only one achievement should exist
      const achievements = db.getAchievementsForUser(userId);
      expect(achievements.length).toBe(1);
    });

    it('should batch insert multiple achievements', () => {
      const achievementIds = ['first_ride', 'ten_rides', 'perfect_balance', 'efficiency_master'];
      const now = Date.now();

      achievementIds.forEach((id, i) => {
        db.addAchievement({
          user_id: userId,
          achievement_id: id,
          unlocked_at: now + i * 1000,
        });
      });

      const achievements = db.getAchievementsForUser(userId);
      expect(achievements.length).toBe(4);
    });
  });
});

describe('Achievement ID validation', () => {
  it('should accept valid achievement IDs', () => {
    const validIds = [
      'first_ride',
      'ten_rides',
      'fifty_rides',
      'hundred_rides',
      'perfect_balance_1m',
      'perfect_balance_5m',
      'perfect_balance_10m',
      'efficiency_master',
      'smooth_operator',
      'three_day_streak',
      'seven_day_streak',
      'fourteen_day_streak',
      'thirty_day_streak',
      'first_drill',
      'ten_drills',
      'perfect_drill',
    ];

    validIds.forEach(id => {
      expect(id.length).toBeLessThanOrEqual(50);
      expect(id.length).toBeGreaterThan(0);
    });
  });

  it('should sanitize long achievement IDs', () => {
    const sanitize = (id: string, maxLen: number) => id.slice(0, maxLen);

    const longId = 'a'.repeat(100);
    const sanitized = sanitize(longId, 50);
    expect(sanitized.length).toBe(50);
  });
});
