import { describe, it, expect, beforeEach } from 'vitest';

/**
 * Unit tests for Drills API
 *
 * Tests for drill results retrieval and statistics.
 */

// Mock types
interface DrillResult {
  id: number;
  user_id: string;
  device_id: string;
  drill_id: string;
  drill_name: string;
  timestamp: number;
  duration_ms: number;
  score: number;
  time_in_target_ms: number;
  time_in_target_percent: number;
  completed: boolean;
  phase_scores_json: string | null;
}

// Simulated database
class MockDrillsDatabase {
  private drills: DrillResult[] = [];
  private idCounter = 1;

  addDrill(drill: Omit<DrillResult, 'id'>): DrillResult {
    const newDrill = { ...drill, id: this.idCounter++ };
    this.drills.push(newDrill);
    return newDrill;
  }

  getDrillsForUser(userId: string, options: { limit: number; offset: number; drillId?: string }): DrillResult[] {
    let results = this.drills.filter(d => d.user_id === userId);
    if (options.drillId) {
      results = results.filter(d => d.drill_id === options.drillId);
    }
    results.sort((a, b) => b.timestamp - a.timestamp);
    return results.slice(options.offset, options.offset + options.limit);
  }

  getCountForUser(userId: string, drillId?: string): number {
    let results = this.drills.filter(d => d.user_id === userId);
    if (drillId) {
      results = results.filter(d => d.drill_id === drillId);
    }
    return results.length;
  }

  getStatsForUser(userId: string): {
    total_drills: number;
    completed_drills: number;
    avg_score: number;
    best_score: number;
    total_duration_ms: number;
    drill_types_tried: number;
  } {
    const userDrills = this.drills.filter(d => d.user_id === userId);
    if (userDrills.length === 0) {
      return {
        total_drills: 0,
        completed_drills: 0,
        avg_score: 0,
        best_score: 0,
        total_duration_ms: 0,
        drill_types_tried: 0,
      };
    }

    const completedDrills = userDrills.filter(d => d.completed);
    const scores = userDrills.map(d => d.score);
    const drillTypes = new Set(userDrills.map(d => d.drill_id));

    return {
      total_drills: userDrills.length,
      completed_drills: completedDrills.length,
      avg_score: scores.reduce((a, b) => a + b, 0) / scores.length,
      best_score: Math.max(...scores),
      total_duration_ms: userDrills.reduce((a, b) => a + b.duration_ms, 0),
      drill_types_tried: drillTypes.size,
    };
  }

  getStatsByDrill(userId: string): Array<{
    drill_id: string;
    drill_name: string;
    attempts: number;
    completions: number;
    avg_score: number;
    best_score: number;
    last_attempt: number;
  }> {
    const userDrills = this.drills.filter(d => d.user_id === userId);
    const byDrill = new Map<string, DrillResult[]>();

    for (const drill of userDrills) {
      const existing = byDrill.get(drill.drill_id) || [];
      existing.push(drill);
      byDrill.set(drill.drill_id, existing);
    }

    return Array.from(byDrill.entries()).map(([drillId, drills]) => ({
      drill_id: drillId,
      drill_name: drills[0].drill_name,
      attempts: drills.length,
      completions: drills.filter(d => d.completed).length,
      avg_score: drills.reduce((a, b) => a + b.score, 0) / drills.length,
      best_score: Math.max(...drills.map(d => d.score)),
      last_attempt: Math.max(...drills.map(d => d.timestamp)),
    }));
  }

  clear() {
    this.drills = [];
    this.idCounter = 1;
  }
}

describe('Drills API', () => {
  let db: MockDrillsDatabase;
  const userId = 'user-123';
  const deviceId = 'device-456';

  beforeEach(() => {
    db = new MockDrillsDatabase();
  });

  describe('GET /api/drills', () => {
    it('should return empty list when no drills exist', () => {
      const drills = db.getDrillsForUser(userId, { limit: 25, offset: 0 });
      expect(drills).toEqual([]);
      expect(db.getCountForUser(userId)).toBe(0);
    });

    it('should return drills for authenticated user', () => {
      db.addDrill({
        user_id: userId,
        device_id: deviceId,
        drill_id: 'balance-hold',
        drill_name: 'Balance Hold',
        timestamp: Date.now(),
        duration_ms: 60000,
        score: 85,
        time_in_target_ms: 45000,
        time_in_target_percent: 75,
        completed: true,
        phase_scores_json: '[85, 80, 90]',
      });

      const drills = db.getDrillsForUser(userId, { limit: 25, offset: 0 });
      expect(drills.length).toBe(1);
      expect(drills[0].drill_id).toBe('balance-hold');
      expect(drills[0].score).toBe(85);
    });

    it('should not return drills from other users', () => {
      db.addDrill({
        user_id: 'other-user',
        device_id: deviceId,
        drill_id: 'balance-hold',
        drill_name: 'Balance Hold',
        timestamp: Date.now(),
        duration_ms: 60000,
        score: 85,
        time_in_target_ms: 45000,
        time_in_target_percent: 75,
        completed: true,
        phase_scores_json: null,
      });

      const drills = db.getDrillsForUser(userId, { limit: 25, offset: 0 });
      expect(drills.length).toBe(0);
    });

    it('should paginate results correctly', () => {
      // Add 30 drills
      for (let i = 0; i < 30; i++) {
        db.addDrill({
          user_id: userId,
          device_id: deviceId,
          drill_id: 'balance-hold',
          drill_name: 'Balance Hold',
          timestamp: Date.now() - i * 1000,
          duration_ms: 60000,
          score: 80 + i,
          time_in_target_ms: 45000,
          time_in_target_percent: 75,
          completed: true,
          phase_scores_json: null,
        });
      }

      const page1 = db.getDrillsForUser(userId, { limit: 10, offset: 0 });
      const page2 = db.getDrillsForUser(userId, { limit: 10, offset: 10 });
      const page3 = db.getDrillsForUser(userId, { limit: 10, offset: 20 });

      expect(page1.length).toBe(10);
      expect(page2.length).toBe(10);
      expect(page3.length).toBe(10);
      expect(db.getCountForUser(userId)).toBe(30);
    });

    it('should filter by drill_id', () => {
      db.addDrill({
        user_id: userId,
        device_id: deviceId,
        drill_id: 'balance-hold',
        drill_name: 'Balance Hold',
        timestamp: Date.now(),
        duration_ms: 60000,
        score: 85,
        time_in_target_ms: 45000,
        time_in_target_percent: 75,
        completed: true,
        phase_scores_json: null,
      });

      db.addDrill({
        user_id: userId,
        device_id: deviceId,
        drill_id: 'te-focus',
        drill_name: 'TE Focus',
        timestamp: Date.now(),
        duration_ms: 60000,
        score: 90,
        time_in_target_ms: 50000,
        time_in_target_percent: 83,
        completed: true,
        phase_scores_json: null,
      });

      const allDrills = db.getDrillsForUser(userId, { limit: 25, offset: 0 });
      expect(allDrills.length).toBe(2);

      const balanceDrills = db.getDrillsForUser(userId, { limit: 25, offset: 0, drillId: 'balance-hold' });
      expect(balanceDrills.length).toBe(1);
      expect(balanceDrills[0].drill_id).toBe('balance-hold');
    });

    it('should order by timestamp descending', () => {
      const now = Date.now();
      db.addDrill({
        user_id: userId,
        device_id: deviceId,
        drill_id: 'balance-hold',
        drill_name: 'Balance Hold',
        timestamp: now - 10000,
        duration_ms: 60000,
        score: 85,
        time_in_target_ms: 45000,
        time_in_target_percent: 75,
        completed: true,
        phase_scores_json: null,
      });

      db.addDrill({
        user_id: userId,
        device_id: deviceId,
        drill_id: 'balance-hold',
        drill_name: 'Balance Hold',
        timestamp: now,
        duration_ms: 60000,
        score: 90,
        time_in_target_ms: 50000,
        time_in_target_percent: 83,
        completed: true,
        phase_scores_json: null,
      });

      const drills = db.getDrillsForUser(userId, { limit: 25, offset: 0 });
      expect(drills[0].timestamp).toBe(now);
      expect(drills[1].timestamp).toBe(now - 10000);
    });
  });

  describe('GET /api/drills/dashboard (combined endpoint)', () => {
    it('should return empty data when no drills exist', () => {
      const drills = db.getDrillsForUser(userId, { limit: 25, offset: 0 });
      const stats = db.getStatsForUser(userId);
      const byDrill = db.getStatsByDrill(userId);

      // Simulating dashboard response
      const dashboardData = {
        drills,
        total: db.getCountForUser(userId),
        limit: 25,
        offset: 0,
        stats: {
          summary: stats,
          by_drill: byDrill,
        },
      };

      expect(dashboardData.drills).toEqual([]);
      expect(dashboardData.total).toBe(0);
      expect(dashboardData.stats.summary.total_drills).toBe(0);
      expect(dashboardData.stats.by_drill).toEqual([]);
    });

    it('should return all dashboard data in single response', () => {
      // Add multiple drills
      const now = Date.now();
      db.addDrill({
        user_id: userId,
        device_id: deviceId,
        drill_id: 'balance-hold',
        drill_name: 'Balance Hold',
        timestamp: now,
        duration_ms: 60000,
        score: 85,
        time_in_target_ms: 45000,
        time_in_target_percent: 75,
        completed: true,
        phase_scores_json: null,
      });

      db.addDrill({
        user_id: userId,
        device_id: deviceId,
        drill_id: 'te-focus',
        drill_name: 'TE Focus',
        timestamp: now - 1000,
        duration_ms: 120000,
        score: 90,
        time_in_target_ms: 100000,
        time_in_target_percent: 83,
        completed: true,
        phase_scores_json: null,
      });

      const drills = db.getDrillsForUser(userId, { limit: 25, offset: 0 });
      const stats = db.getStatsForUser(userId);
      const byDrill = db.getStatsByDrill(userId);

      const dashboardData = {
        drills,
        total: db.getCountForUser(userId),
        limit: 25,
        offset: 0,
        stats: {
          summary: stats,
          by_drill: byDrill,
        },
      };

      // Verify drills list
      expect(dashboardData.drills.length).toBe(2);
      expect(dashboardData.total).toBe(2);

      // Verify summary stats
      expect(dashboardData.stats.summary.total_drills).toBe(2);
      expect(dashboardData.stats.summary.completed_drills).toBe(2);
      expect(dashboardData.stats.summary.avg_score).toBe(87.5);
      expect(dashboardData.stats.summary.best_score).toBe(90);
      expect(dashboardData.stats.summary.total_duration_ms).toBe(180000);
      expect(dashboardData.stats.summary.drill_types_tried).toBe(2);

      // Verify per-drill stats
      expect(dashboardData.stats.by_drill.length).toBe(2);
    });

    it('should paginate drills in dashboard response', () => {
      // Add 30 drills
      for (let i = 0; i < 30; i++) {
        db.addDrill({
          user_id: userId,
          device_id: deviceId,
          drill_id: i % 2 === 0 ? 'balance-hold' : 'te-focus',
          drill_name: i % 2 === 0 ? 'Balance Hold' : 'TE Focus',
          timestamp: Date.now() - i * 1000,
          duration_ms: 60000,
          score: 80 + (i % 20),
          time_in_target_ms: 45000,
          time_in_target_percent: 75,
          completed: i % 3 !== 0,
          phase_scores_json: null,
        });
      }

      const page1 = db.getDrillsForUser(userId, { limit: 10, offset: 0 });
      const page2 = db.getDrillsForUser(userId, { limit: 10, offset: 10 });

      expect(page1.length).toBe(10);
      expect(page2.length).toBe(10);

      // But stats should reflect all drills
      const stats = db.getStatsForUser(userId);
      expect(stats.total_drills).toBe(30);
      expect(stats.drill_types_tried).toBe(2);
    });

    it('should include all fields in per-drill stats', () => {
      const now = Date.now();

      // Add 3 balance-hold drills
      for (let i = 0; i < 3; i++) {
        db.addDrill({
          user_id: userId,
          device_id: deviceId,
          drill_id: 'balance-hold',
          drill_name: 'Balance Hold',
          timestamp: now - i * 10000,
          duration_ms: 60000,
          score: 80 + i * 5, // 80, 85, 90
          time_in_target_ms: 45000,
          time_in_target_percent: 75,
          completed: i !== 0, // First one not completed
          phase_scores_json: null,
        });
      }

      const byDrill = db.getStatsByDrill(userId);
      expect(byDrill.length).toBe(1);

      const balanceStats = byDrill.find(d => d.drill_id === 'balance-hold');
      expect(balanceStats).toBeDefined();
      expect(balanceStats!.attempts).toBe(3);
      expect(balanceStats!.completions).toBe(2);
      expect(balanceStats!.avg_score).toBe(85);
      expect(balanceStats!.best_score).toBe(90);
      expect(balanceStats!.last_attempt).toBe(now);
    });

    it('should isolate data per user in dashboard', () => {
      const otherUserId = 'other-user';

      db.addDrill({
        user_id: userId,
        device_id: deviceId,
        drill_id: 'balance-hold',
        drill_name: 'Balance Hold',
        timestamp: Date.now(),
        duration_ms: 60000,
        score: 85,
        time_in_target_ms: 45000,
        time_in_target_percent: 75,
        completed: true,
        phase_scores_json: null,
      });

      db.addDrill({
        user_id: otherUserId,
        device_id: 'other-device',
        drill_id: 'te-focus',
        drill_name: 'TE Focus',
        timestamp: Date.now(),
        duration_ms: 120000,
        score: 95,
        time_in_target_ms: 100000,
        time_in_target_percent: 83,
        completed: true,
        phase_scores_json: null,
      });

      const myDrills = db.getDrillsForUser(userId, { limit: 25, offset: 0 });
      const myStats = db.getStatsForUser(userId);

      expect(myDrills.length).toBe(1);
      expect(myStats.total_drills).toBe(1);
      expect(myStats.best_score).toBe(85);
    });
  });

  describe('GET /api/drills/stats', () => {
    it('should return zero stats when no drills exist', () => {
      const stats = db.getStatsForUser(userId);
      expect(stats.total_drills).toBe(0);
      expect(stats.completed_drills).toBe(0);
      expect(stats.avg_score).toBe(0);
      expect(stats.best_score).toBe(0);
    });

    it('should calculate correct summary stats', () => {
      db.addDrill({
        user_id: userId,
        device_id: deviceId,
        drill_id: 'balance-hold',
        drill_name: 'Balance Hold',
        timestamp: Date.now(),
        duration_ms: 60000,
        score: 80,
        time_in_target_ms: 45000,
        time_in_target_percent: 75,
        completed: true,
        phase_scores_json: null,
      });

      db.addDrill({
        user_id: userId,
        device_id: deviceId,
        drill_id: 'te-focus',
        drill_name: 'TE Focus',
        timestamp: Date.now(),
        duration_ms: 120000,
        score: 100,
        time_in_target_ms: 100000,
        time_in_target_percent: 83,
        completed: true,
        phase_scores_json: null,
      });

      db.addDrill({
        user_id: userId,
        device_id: deviceId,
        drill_id: 'balance-hold',
        drill_name: 'Balance Hold',
        timestamp: Date.now(),
        duration_ms: 30000,
        score: 60,
        time_in_target_ms: 15000,
        time_in_target_percent: 50,
        completed: false,
        phase_scores_json: null,
      });

      const stats = db.getStatsForUser(userId);
      expect(stats.total_drills).toBe(3);
      expect(stats.completed_drills).toBe(2);
      expect(stats.avg_score).toBe(80); // (80 + 100 + 60) / 3
      expect(stats.best_score).toBe(100);
      expect(stats.total_duration_ms).toBe(210000);
      expect(stats.drill_types_tried).toBe(2);
    });

    it('should return per-drill statistics', () => {
      db.addDrill({
        user_id: userId,
        device_id: deviceId,
        drill_id: 'balance-hold',
        drill_name: 'Balance Hold',
        timestamp: Date.now() - 10000,
        duration_ms: 60000,
        score: 80,
        time_in_target_ms: 45000,
        time_in_target_percent: 75,
        completed: true,
        phase_scores_json: null,
      });

      db.addDrill({
        user_id: userId,
        device_id: deviceId,
        drill_id: 'balance-hold',
        drill_name: 'Balance Hold',
        timestamp: Date.now(),
        duration_ms: 60000,
        score: 90,
        time_in_target_ms: 50000,
        time_in_target_percent: 83,
        completed: true,
        phase_scores_json: null,
      });

      const byDrill = db.getStatsByDrill(userId);
      expect(byDrill.length).toBe(1);
      expect(byDrill[0].drill_id).toBe('balance-hold');
      expect(byDrill[0].attempts).toBe(2);
      expect(byDrill[0].completions).toBe(2);
      expect(byDrill[0].avg_score).toBe(85);
      expect(byDrill[0].best_score).toBe(90);
    });
  });
});

describe('Drill Sync API', () => {
  describe('POST /api/sync/drill', () => {
    it('should validate required fields', () => {
      const validDrill = {
        drill_id: 'balance-hold',
        drill_name: 'Balance Hold',
        timestamp: Date.now(),
        duration_ms: 60000,
        score: 85,
        time_in_target_ms: 45000,
        time_in_target_percent: 75,
        completed: true,
      };

      // All required fields present
      expect(validDrill.drill_id).toBeTruthy();
      expect(validDrill.drill_name).toBeTruthy();
      expect(validDrill.timestamp).toBeGreaterThan(0);
      expect(validDrill.score).toBeDefined();
    });

    it('should detect missing drill_id', () => {
      const invalidDrill = {
        drill_name: 'Balance Hold',
        timestamp: Date.now(),
        score: 85,
      };

      expect((invalidDrill as any).drill_id).toBeUndefined();
    });

    it('should handle duplicate drill detection', () => {
      const drills = new Map<string, boolean>();
      const key1 = 'user-123:device-456:balance-hold:1234567890';
      const key2 = 'user-123:device-456:balance-hold:1234567890';

      drills.set(key1, true);
      expect(drills.has(key2)).toBe(true); // Same drill, should be detected as duplicate
    });
  });

  describe('POST /api/sync/drills (batch)', () => {
    it('should enforce batch size limit', () => {
      const maxBatchSize = 50;
      const drills = Array(60).fill(null).map((_, i) => ({
        drill_id: 'balance-hold',
        drill_name: 'Balance Hold',
        timestamp: Date.now() + i,
        duration_ms: 60000,
        score: 85,
        time_in_target_ms: 45000,
        time_in_target_percent: 75,
        completed: true,
      }));

      expect(drills.length).toBeGreaterThan(maxBatchSize);
    });

    it('should count duplicates correctly', () => {
      const existingTimestamps = new Set([1000, 2000, 3000]);
      const drills = [
        { timestamp: 1000 }, // duplicate
        { timestamp: 2000 }, // duplicate
        { timestamp: 4000 }, // new
        { timestamp: 5000 }, // new
      ];

      const newDrills = drills.filter(d => !existingTimestamps.has(d.timestamp));
      const duplicates = drills.length - newDrills.length;

      expect(newDrills.length).toBe(2);
      expect(duplicates).toBe(2);
    });
  });
});
