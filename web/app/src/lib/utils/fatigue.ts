/**
 * Fatigue Analysis Utilities
 * Analyzes ride snapshots to detect technique degradation over time
 */

import type { FatigueAnalysis, RideSnapshot } from '$lib/types/dashboard';

/**
 * Calculate fatigue analysis from ride snapshots
 * Compares first third vs last third of ride to detect degradation
 */
export function calculateFatigueAnalysis(snapshots: RideSnapshot[]): FatigueAnalysis {
  const thirdLen = Math.floor(snapshots.length / 3);
  const firstThird = snapshots.slice(0, thirdLen);
  const lastThird = snapshots.slice(-thirdLen);

  const avg = (arr: RideSnapshot[], key: keyof RideSnapshot) =>
    arr.reduce((s, x) => s + (x[key] || 0), 0) / arr.length;

  const first = {
    balance: avg(firstThird, 'balance_left'),
    te: (avg(firstThird, 'te_left') + avg(firstThird, 'te_right')) / 2,
    ps: (avg(firstThird, 'ps_left') + avg(firstThird, 'ps_right')) / 2,
    power: avg(firstThird, 'power_avg'),
  };

  const last = {
    balance: avg(lastThird, 'balance_left'),
    te: (avg(lastThird, 'te_left') + avg(lastThird, 'te_right')) / 2,
    ps: (avg(lastThird, 'ps_left') + avg(lastThird, 'ps_right')) / 2,
    power: avg(lastThird, 'power_avg'),
  };

  return {
    hasData: true,
    firstThird: first,
    lastThird: last,
    degradation: {
      balance: Math.abs(last.balance - 50) - Math.abs(first.balance - 50), // positive = worse
      te: first.te - last.te, // positive = degraded
      ps: first.ps - last.ps, // positive = degraded
    },
  };
}
