import { isDemoUser } from '../types/env';

/**
 * Demo data timestamp adjustment utility
 *
 * Demo rides are stored with fixed timestamps. This utility adjusts them
 * so they always appear as recent rides (within last 2 weeks) for fresh UX.
 *
 * Strategy:
 * - The most recent demo ride should appear as "today at midnight"
 * - All other rides keep their relative spacing
 * - We calculate offset = today_midnight - max_demo_timestamp
 * - Apply offset to all timestamps
 */

// Hardcoded max timestamp from demo data (Dec 26, 2025 18:34:46)
// This eliminates a database query on every demo request
const DEMO_MAX_TIMESTAMP = 1766766886000;

// Cache for demo offset - recalculate daily since offset depends on current date
let cachedDemoOffset: { offset: number; dateKey: string } | null = null;

/**
 * Calculate time offset to make demo rides appear recent
 * Uses hardcoded max timestamp - no database query needed
 * @returns Offset in milliseconds to add to all demo timestamps
 */
export function calculateDemoOffset(): number {
  // Use date key for cache (offset changes daily)
  const today = new Date();
  today.setUTCHours(0, 0, 0, 0);
  const dateKey = today.toISOString().split('T')[0];

  // Return cached if same day
  if (cachedDemoOffset && cachedDemoOffset.dateKey === dateKey) {
    return cachedDemoOffset.offset;
  }

  // Calculate offset: today midnight - newest ride timestamp
  // This makes the newest ride appear as "today"
  const offset = today.getTime() - DEMO_MAX_TIMESTAMP;

  // Cache it
  cachedDemoOffset = { offset, dateKey };

  return offset;
}

/**
 * Adjust timestamp for demo user
 * Makes demo data appear recent by shifting timestamps
 */
export function adjustDemoTimestamp(timestamp: number, offset: number): number {
  return timestamp + offset;
}

/**
 * Adjust ride data for demo user
 * Returns new object with adjusted timestamps
 */
export function adjustDemoRide<T extends { timestamp: number }>(ride: T, offset: number): T {
  return {
    ...ride,
    timestamp: adjustDemoTimestamp(ride.timestamp, offset),
  };
}

/**
 * Adjust snapshot data for demo user
 */
export function adjustDemoSnapshot<T extends { timestamp?: number }>(snapshot: T, offset: number): T {
  if (snapshot.timestamp) {
    return {
      ...snapshot,
      timestamp: adjustDemoTimestamp(snapshot.timestamp, offset),
    };
  }
  return snapshot;
}

/**
 * Adjust date string for demo user (e.g., "2025-12-25" -> adjusted date)
 */
export function adjustDemoDateString(dateStr: string, offset: number): string {
  const date = new Date(dateStr);
  date.setTime(date.getTime() + offset);
  return date.toISOString().split('T')[0];
}

/**
 * Get demo offset - no database query needed
 * Uses hardcoded max timestamp for instant calculation
 */
export function getDemoOffset(userId: string): number {
  if (!isDemoUser(userId)) {
    return 0;
  }
  return calculateDemoOffset();
}
