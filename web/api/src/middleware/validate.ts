/**
 * Input validation utilities
 */

/**
 * Validate and sanitize pagination parameters
 */
export function validatePagination(limit?: string, offset?: string): {
  limit: number;
  offset: number;
} {
  const parsedLimit = parseInt(limit || '50', 10);
  const parsedOffset = parseInt(offset || '0', 10);

  return {
    // Limit: 1-100, default 50
    limit: Math.min(Math.max(isNaN(parsedLimit) ? 50 : parsedLimit, 1), 100),
    // Offset: 0+, default 0
    offset: Math.max(isNaN(parsedOffset) ? 0 : parsedOffset, 0),
  };
}

/**
 * Validate days parameter for trends
 */
export function validateDays(days?: string): number {
  const parsed = parseInt(days || '30', 10);
  // Days: 1-365, default 30
  return Math.min(Math.max(isNaN(parsed) ? 30 : parsed, 1), 365);
}

/**
 * Validate ride data from sync
 */
export function validateRideData(ride: unknown): {
  valid: boolean;
  errors: string[];
} {
  const errors: string[] = [];

  if (!ride || typeof ride !== 'object') {
    return { valid: false, errors: ['Invalid ride data'] };
  }

  const r = ride as Record<string, unknown>;

  // Required fields
  if (typeof r.timestamp !== 'number' || r.timestamp <= 0) {
    errors.push('Invalid timestamp');
  }
  if (typeof r.duration_ms !== 'number' || r.duration_ms < 0) {
    errors.push('Invalid duration_ms');
  }

  // Balance fields (0-100)
  if (!isValidPercentage(r.balance_left)) errors.push('Invalid balance_left');
  if (!isValidPercentage(r.balance_right)) errors.push('Invalid balance_right');

  // TE/PS fields (0-100)
  if (!isValidPercentage(r.te_left)) errors.push('Invalid te_left');
  if (!isValidPercentage(r.te_right)) errors.push('Invalid te_right');
  if (!isValidPercentage(r.ps_left)) errors.push('Invalid ps_left');
  if (!isValidPercentage(r.ps_right)) errors.push('Invalid ps_right');

  // Zone fields (0-100, should sum to ~100)
  if (!isValidPercentage(r.zone_optimal)) errors.push('Invalid zone_optimal');
  if (!isValidPercentage(r.zone_attention)) errors.push('Invalid zone_attention');
  if (!isValidPercentage(r.zone_problem)) errors.push('Invalid zone_problem');

  // Score (0-100)
  if (!isValidScore(r.score)) errors.push('Invalid score');

  return {
    valid: errors.length === 0,
    errors,
  };
}

function isValidPercentage(value: unknown): boolean {
  return typeof value === 'number' && value >= 0 && value <= 100;
}

function isValidScore(value: unknown): boolean {
  return typeof value === 'number' &&
         Number.isInteger(value) &&
         value >= 0 &&
         value <= 100;
}

/**
 * Sanitize string input (prevent XSS)
 */
export function sanitizeString(input: string, maxLength: number = 1000): string {
  if (typeof input !== 'string') return '';
  return input
    .slice(0, maxLength)
    .replace(/[<>]/g, '') // Remove angle brackets
    .trim();
}

/**
 * Validate device ID format
 */
export function isValidDeviceId(deviceId: unknown): boolean {
  if (typeof deviceId !== 'string') return false;
  // Allow alphanumeric, hyphens, underscores, 1-100 chars
  return /^[a-zA-Z0-9_-]{1,100}$/.test(deviceId);
}
