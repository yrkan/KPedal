import { describe, it, expect } from 'vitest';

/**
 * Unit tests for validation utilities
 *
 * Tests pagination, ride data, string sanitization, and device ID validation
 */

// Validation functions (mirrors validate.ts)

function validatePagination(limit?: string, offset?: string): {
  limit: number;
  offset: number;
} {
  const parsedLimit = parseInt(limit || '50', 10);
  const parsedOffset = parseInt(offset || '0', 10);

  return {
    limit: Math.min(Math.max(isNaN(parsedLimit) ? 50 : parsedLimit, 1), 100),
    offset: Math.max(isNaN(parsedOffset) ? 0 : parsedOffset, 0),
  };
}

function validateDays(days?: string): number {
  const parsed = parseInt(days || '30', 10);
  return Math.min(Math.max(isNaN(parsed) ? 30 : parsed, 1), 365);
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

function validateRideData(ride: unknown): {
  valid: boolean;
  errors: string[];
} {
  const errors: string[] = [];

  if (!ride || typeof ride !== 'object') {
    return { valid: false, errors: ['Invalid ride data'] };
  }

  const r = ride as Record<string, unknown>;

  if (typeof r.timestamp !== 'number' || r.timestamp <= 0) {
    errors.push('Invalid timestamp');
  }
  if (typeof r.duration_ms !== 'number' || r.duration_ms < 0) {
    errors.push('Invalid duration_ms');
  }

  if (!isValidPercentage(r.balance_left)) errors.push('Invalid balance_left');
  if (!isValidPercentage(r.balance_right)) errors.push('Invalid balance_right');
  if (!isValidPercentage(r.te_left)) errors.push('Invalid te_left');
  if (!isValidPercentage(r.te_right)) errors.push('Invalid te_right');
  if (!isValidPercentage(r.ps_left)) errors.push('Invalid ps_left');
  if (!isValidPercentage(r.ps_right)) errors.push('Invalid ps_right');
  if (!isValidPercentage(r.zone_optimal)) errors.push('Invalid zone_optimal');
  if (!isValidPercentage(r.zone_attention)) errors.push('Invalid zone_attention');
  if (!isValidPercentage(r.zone_problem)) errors.push('Invalid zone_problem');
  if (!isValidScore(r.score)) errors.push('Invalid score');

  return {
    valid: errors.length === 0,
    errors,
  };
}

function sanitizeString(input: string, maxLength: number = 1000): string {
  if (typeof input !== 'string') return '';
  return input
    .slice(0, maxLength)
    .replace(/[<>]/g, '')
    .trim();
}

function isValidDeviceId(deviceId: unknown): boolean {
  if (typeof deviceId !== 'string') return false;
  return /^[a-zA-Z0-9_-]{1,100}$/.test(deviceId);
}

describe('validatePagination', () => {
  describe('limit parameter', () => {
    it('should use default limit of 50 when not provided', () => {
      const result = validatePagination();
      expect(result.limit).toBe(50);
    });

    it('should parse valid limit string', () => {
      expect(validatePagination('25').limit).toBe(25);
      expect(validatePagination('1').limit).toBe(1);
      expect(validatePagination('100').limit).toBe(100);
    });

    it('should clamp limit to minimum of 1', () => {
      expect(validatePagination('0').limit).toBe(1);
      expect(validatePagination('-10').limit).toBe(1);
    });

    it('should clamp limit to maximum of 100', () => {
      expect(validatePagination('101').limit).toBe(100);
      expect(validatePagination('1000').limit).toBe(100);
    });

    it('should use default 50 for invalid strings', () => {
      expect(validatePagination('abc').limit).toBe(50);
      expect(validatePagination('').limit).toBe(50);
      expect(validatePagination('NaN').limit).toBe(50);
    });

    it('should handle decimal strings', () => {
      expect(validatePagination('25.5').limit).toBe(25);
      expect(validatePagination('99.9').limit).toBe(99);
    });
  });

  describe('offset parameter', () => {
    it('should use default offset of 0 when not provided', () => {
      const result = validatePagination();
      expect(result.offset).toBe(0);
    });

    it('should parse valid offset string', () => {
      expect(validatePagination(undefined, '0').offset).toBe(0);
      expect(validatePagination(undefined, '50').offset).toBe(50);
      expect(validatePagination(undefined, '1000').offset).toBe(1000);
    });

    it('should clamp offset to minimum of 0', () => {
      expect(validatePagination(undefined, '-1').offset).toBe(0);
      expect(validatePagination(undefined, '-100').offset).toBe(0);
    });

    it('should allow any positive offset (no max)', () => {
      expect(validatePagination(undefined, '999999').offset).toBe(999999);
    });

    it('should use default 0 for invalid strings', () => {
      expect(validatePagination(undefined, 'abc').offset).toBe(0);
      expect(validatePagination(undefined, '').offset).toBe(0);
    });
  });

  describe('both parameters', () => {
    it('should handle both parameters correctly', () => {
      const result = validatePagination('25', '100');
      expect(result.limit).toBe(25);
      expect(result.offset).toBe(100);
    });
  });
});

describe('validateDays', () => {
  it('should use default of 30 when not provided', () => {
    expect(validateDays()).toBe(30);
    expect(validateDays(undefined)).toBe(30);
  });

  it('should parse valid days string', () => {
    expect(validateDays('7')).toBe(7);
    expect(validateDays('30')).toBe(30);
    expect(validateDays('90')).toBe(90);
  });

  it('should clamp to minimum of 1', () => {
    expect(validateDays('0')).toBe(1);
    expect(validateDays('-5')).toBe(1);
  });

  it('should clamp to maximum of 365', () => {
    expect(validateDays('366')).toBe(365);
    expect(validateDays('1000')).toBe(365);
  });

  it('should use default 30 for invalid strings', () => {
    expect(validateDays('abc')).toBe(30);
    expect(validateDays('')).toBe(30);
  });
});

describe('isValidPercentage', () => {
  it('should accept valid percentages', () => {
    expect(isValidPercentage(0)).toBe(true);
    expect(isValidPercentage(50)).toBe(true);
    expect(isValidPercentage(100)).toBe(true);
    expect(isValidPercentage(50.5)).toBe(true);
  });

  it('should reject values below 0', () => {
    expect(isValidPercentage(-1)).toBe(false);
    expect(isValidPercentage(-0.1)).toBe(false);
  });

  it('should reject values above 100', () => {
    expect(isValidPercentage(101)).toBe(false);
    expect(isValidPercentage(100.1)).toBe(false);
  });

  it('should reject non-numbers', () => {
    expect(isValidPercentage('50')).toBe(false);
    expect(isValidPercentage(null)).toBe(false);
    expect(isValidPercentage(undefined)).toBe(false);
    expect(isValidPercentage({})).toBe(false);
  });

  it('should reject NaN', () => {
    expect(isValidPercentage(NaN)).toBe(false);
  });
});

describe('isValidScore', () => {
  it('should accept valid integer scores', () => {
    expect(isValidScore(0)).toBe(true);
    expect(isValidScore(50)).toBe(true);
    expect(isValidScore(100)).toBe(true);
  });

  it('should reject non-integers', () => {
    expect(isValidScore(50.5)).toBe(false);
    expect(isValidScore(99.9)).toBe(false);
  });

  it('should reject values below 0', () => {
    expect(isValidScore(-1)).toBe(false);
  });

  it('should reject values above 100', () => {
    expect(isValidScore(101)).toBe(false);
  });

  it('should reject non-numbers', () => {
    expect(isValidScore('50')).toBe(false);
    expect(isValidScore(null)).toBe(false);
    expect(isValidScore(undefined)).toBe(false);
  });
});

describe('validateRideData', () => {
  const validRide = {
    timestamp: 1703894400000,
    duration_ms: 3600000,
    balance_left: 48,
    balance_right: 52,
    te_left: 75,
    te_right: 73,
    ps_left: 22,
    ps_right: 24,
    zone_optimal: 60,
    zone_attention: 30,
    zone_problem: 10,
    score: 85,
  };

  it('should accept valid ride data', () => {
    const result = validateRideData(validRide);
    expect(result.valid).toBe(true);
    expect(result.errors).toHaveLength(0);
  });

  it('should reject null input', () => {
    const result = validateRideData(null);
    expect(result.valid).toBe(false);
    expect(result.errors).toContain('Invalid ride data');
  });

  it('should reject undefined input', () => {
    const result = validateRideData(undefined);
    expect(result.valid).toBe(false);
  });

  it('should reject non-object input', () => {
    expect(validateRideData('string').valid).toBe(false);
    expect(validateRideData(123).valid).toBe(false);
    expect(validateRideData([]).valid).toBe(false);
  });

  describe('timestamp validation', () => {
    it('should reject missing timestamp', () => {
      const { timestamp, ...ride } = validRide;
      const result = validateRideData(ride);
      expect(result.errors).toContain('Invalid timestamp');
    });

    it('should reject zero timestamp', () => {
      const result = validateRideData({ ...validRide, timestamp: 0 });
      expect(result.errors).toContain('Invalid timestamp');
    });

    it('should reject negative timestamp', () => {
      const result = validateRideData({ ...validRide, timestamp: -1 });
      expect(result.errors).toContain('Invalid timestamp');
    });

    it('should reject string timestamp', () => {
      const result = validateRideData({ ...validRide, timestamp: '123' });
      expect(result.errors).toContain('Invalid timestamp');
    });
  });

  describe('duration_ms validation', () => {
    it('should accept zero duration', () => {
      const result = validateRideData({ ...validRide, duration_ms: 0 });
      expect(result.errors).not.toContain('Invalid duration_ms');
    });

    it('should reject negative duration', () => {
      const result = validateRideData({ ...validRide, duration_ms: -1 });
      expect(result.errors).toContain('Invalid duration_ms');
    });

    it('should reject missing duration', () => {
      const { duration_ms, ...ride } = validRide;
      const result = validateRideData(ride);
      expect(result.errors).toContain('Invalid duration_ms');
    });
  });

  describe('balance validation', () => {
    it('should reject invalid balance_left', () => {
      const result = validateRideData({ ...validRide, balance_left: -1 });
      expect(result.errors).toContain('Invalid balance_left');
    });

    it('should reject invalid balance_right', () => {
      const result = validateRideData({ ...validRide, balance_right: 101 });
      expect(result.errors).toContain('Invalid balance_right');
    });

    it('should accept edge values for balance', () => {
      const result = validateRideData({
        ...validRide,
        balance_left: 0,
        balance_right: 100,
      });
      expect(result.errors).not.toContain('Invalid balance_left');
      expect(result.errors).not.toContain('Invalid balance_right');
    });
  });

  describe('TE/PS validation', () => {
    it('should reject invalid te_left', () => {
      const result = validateRideData({ ...validRide, te_left: 'high' });
      expect(result.errors).toContain('Invalid te_left');
    });

    it('should reject invalid ps_right', () => {
      const result = validateRideData({ ...validRide, ps_right: null });
      expect(result.errors).toContain('Invalid ps_right');
    });
  });

  describe('zone validation', () => {
    it('should reject invalid zone_optimal', () => {
      const result = validateRideData({ ...validRide, zone_optimal: -5 });
      expect(result.errors).toContain('Invalid zone_optimal');
    });

    it('should reject invalid zone_attention', () => {
      const result = validateRideData({ ...validRide, zone_attention: 'medium' });
      expect(result.errors).toContain('Invalid zone_attention');
    });
  });

  describe('score validation', () => {
    it('should reject non-integer score', () => {
      const result = validateRideData({ ...validRide, score: 85.5 });
      expect(result.errors).toContain('Invalid score');
    });

    it('should reject out-of-range score', () => {
      const result = validateRideData({ ...validRide, score: 101 });
      expect(result.errors).toContain('Invalid score');
    });

    it('should accept edge value scores', () => {
      expect(validateRideData({ ...validRide, score: 0 }).errors).not.toContain('Invalid score');
      expect(validateRideData({ ...validRide, score: 100 }).errors).not.toContain('Invalid score');
    });
  });

  describe('multiple errors', () => {
    it('should collect all validation errors', () => {
      const result = validateRideData({
        timestamp: 0,
        duration_ms: -1,
        balance_left: -1,
        balance_right: 101,
        te_left: 'invalid',
        te_right: null,
        ps_left: undefined,
        ps_right: {},
        zone_optimal: -1,
        zone_attention: 'high',
        zone_problem: NaN,
        score: 150,
      });

      expect(result.valid).toBe(false);
      expect(result.errors.length).toBeGreaterThan(5);
    });
  });
});

describe('sanitizeString', () => {
  it('should return input unchanged for safe strings', () => {
    expect(sanitizeString('Hello World')).toBe('Hello World');
    expect(sanitizeString('user@example.com')).toBe('user@example.com');
  });

  it('should remove angle brackets (XSS prevention)', () => {
    expect(sanitizeString('<script>alert(1)</script>')).toBe('scriptalert(1)/script');
    expect(sanitizeString('Hello <b>World</b>')).toBe('Hello bWorld/b');
  });

  it('should trim whitespace', () => {
    expect(sanitizeString('  hello  ')).toBe('hello');
    expect(sanitizeString('\n\ttest\n\t')).toBe('test');
  });

  it('should truncate to max length', () => {
    const longString = 'a'.repeat(2000);
    expect(sanitizeString(longString).length).toBe(1000);
    expect(sanitizeString(longString, 50).length).toBe(50);
  });

  it('should handle custom max length', () => {
    expect(sanitizeString('hello world', 5)).toBe('hello');
  });

  it('should return empty string for non-string input', () => {
    expect(sanitizeString(123 as any)).toBe('');
    expect(sanitizeString(null as any)).toBe('');
    expect(sanitizeString(undefined as any)).toBe('');
    expect(sanitizeString({} as any)).toBe('');
  });

  it('should handle empty string', () => {
    expect(sanitizeString('')).toBe('');
  });

  it('should preserve safe special characters', () => {
    expect(sanitizeString('test@example.com')).toBe('test@example.com');
    expect(sanitizeString('100%')).toBe('100%');
    expect(sanitizeString('a & b')).toBe('a & b');
  });
});

describe('isValidDeviceId', () => {
  it('should accept valid device IDs', () => {
    expect(isValidDeviceId('karoo-12345')).toBe(true);
    expect(isValidDeviceId('device_1')).toBe(true);
    expect(isValidDeviceId('ABC123')).toBe(true);
    expect(isValidDeviceId('a')).toBe(true);
  });

  it('should accept alphanumeric with hyphens and underscores', () => {
    expect(isValidDeviceId('my-device-123')).toBe(true);
    expect(isValidDeviceId('my_device_123')).toBe(true);
    expect(isValidDeviceId('My-Device_123')).toBe(true);
  });

  it('should reject empty string', () => {
    expect(isValidDeviceId('')).toBe(false);
  });

  it('should reject strings over 100 characters', () => {
    const longId = 'a'.repeat(101);
    expect(isValidDeviceId(longId)).toBe(false);

    const maxLengthId = 'a'.repeat(100);
    expect(isValidDeviceId(maxLengthId)).toBe(true);
  });

  it('should reject special characters', () => {
    expect(isValidDeviceId('device@123')).toBe(false);
    expect(isValidDeviceId('device#123')).toBe(false);
    expect(isValidDeviceId('device 123')).toBe(false);
    expect(isValidDeviceId('device.123')).toBe(false);
    expect(isValidDeviceId('device/123')).toBe(false);
  });

  it('should reject non-string input', () => {
    expect(isValidDeviceId(123)).toBe(false);
    expect(isValidDeviceId(null)).toBe(false);
    expect(isValidDeviceId(undefined)).toBe(false);
    expect(isValidDeviceId({})).toBe(false);
    expect(isValidDeviceId(['device-1'])).toBe(false);
  });

  it('should reject strings with angle brackets (XSS)', () => {
    expect(isValidDeviceId('<script>')).toBe(false);
    expect(isValidDeviceId('device<>')).toBe(false);
  });
});

describe('Edge Cases', () => {
  it('validatePagination with whitespace strings', () => {
    const result = validatePagination('  50  ', '  100  ');
    // parseInt handles leading/trailing whitespace
    expect(result.limit).toBe(50);
    expect(result.offset).toBe(100);
  });

  it('validateDays with leading zeros', () => {
    expect(validateDays('007')).toBe(7);
    expect(validateDays('030')).toBe(30);
  });

  it('validateRideData with extra fields', () => {
    const result = validateRideData({
      ...{
        timestamp: 1703894400000,
        duration_ms: 3600000,
        balance_left: 48,
        balance_right: 52,
        te_left: 75,
        te_right: 73,
        ps_left: 22,
        ps_right: 24,
        zone_optimal: 60,
        zone_attention: 30,
        zone_problem: 10,
        score: 85,
      },
      extra_field: 'ignored',
      another: 123,
    });
    expect(result.valid).toBe(true);
  });

  it('sanitizeString with only whitespace', () => {
    expect(sanitizeString('   ')).toBe('');
    expect(sanitizeString('\t\n\r')).toBe('');
  });

  it('sanitizeString with mixed content', () => {
    expect(sanitizeString('  <script>  test  </script>  ')).toBe('script  test  /script');
  });
});
