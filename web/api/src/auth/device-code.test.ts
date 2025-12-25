import { describe, it, expect } from 'vitest';

/**
 * Unit tests for Device Code Flow logic
 */

// Helper functions extracted for testing
function generateUserCode(): string {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ'; // No I, O to avoid confusion
  const nums = '0123456789';

  const randomBytes = new Uint8Array(8);
  crypto.getRandomValues(randomBytes);

  let code = '';
  for (let i = 0; i < 4; i++) {
    code += chars.charAt(randomBytes[i] % chars.length);
  }
  code += '-';
  for (let i = 4; i < 8; i++) {
    code += nums.charAt(randomBytes[i] % nums.length);
  }
  return code;
}

function normalizeUserCode(userCode: string): string | null {
  // Normalize user code (uppercase, remove spaces, ensure dash format XXXX-XXXX)
  let normalizedCode = userCode.toUpperCase().replace(/\s/g, '').replace(/-/g, '');

  // Validate code format (8 alphanumeric chars: 4 letters + 4 digits)
  if (!/^[A-Z]{4}[0-9]{4}$/.test(normalizedCode)) {
    return null;
  }

  // Add dash for lookup
  normalizedCode = normalizedCode.slice(0, 4) + '-' + normalizedCode.slice(4);
  return normalizedCode;
}

function getExpiryTimestamp(expirySeconds: number = 600): string {
  const expiry = new Date(Date.now() + expirySeconds * 1000);
  return expiry.toISOString().replace('T', ' ').replace('Z', '');
}

function isExpired(expiresAt: string): boolean {
  const expiry = new Date(expiresAt.replace(' ', 'T') + 'Z');
  return Date.now() > expiry.getTime();
}

function getRemainingSeconds(expiresAt: string): number {
  const expiry = new Date(expiresAt.replace(' ', 'T') + 'Z');
  return Math.max(0, Math.floor((expiry.getTime() - Date.now()) / 1000));
}

describe('User Code Generation', () => {
  it('should generate code in XXXX-0000 format', () => {
    const code = generateUserCode();
    expect(code).toMatch(/^[A-Z]{4}-[0-9]{4}$/);
  });

  it('should not contain I or O characters', () => {
    // Generate multiple codes to increase chance of catching I/O
    for (let i = 0; i < 100; i++) {
      const code = generateUserCode();
      expect(code).not.toContain('I');
      expect(code).not.toContain('O');
    }
  });

  it('should have 9 characters total (including dash)', () => {
    const code = generateUserCode();
    expect(code.length).toBe(9);
  });
});

describe('User Code Normalization', () => {
  it('should normalize valid code with dash', () => {
    expect(normalizeUserCode('ABCD-1234')).toBe('ABCD-1234');
  });

  it('should normalize valid code without dash', () => {
    expect(normalizeUserCode('ABCD1234')).toBe('ABCD-1234');
  });

  it('should convert lowercase to uppercase', () => {
    expect(normalizeUserCode('abcd-1234')).toBe('ABCD-1234');
    expect(normalizeUserCode('abcd1234')).toBe('ABCD-1234');
  });

  it('should remove spaces', () => {
    expect(normalizeUserCode('ABCD 1234')).toBe('ABCD-1234');
    expect(normalizeUserCode('AB CD 12 34')).toBe('ABCD-1234');
  });

  it('should remove multiple dashes', () => {
    expect(normalizeUserCode('AB-CD-12-34')).toBe('ABCD-1234');
  });

  it('should reject invalid format - wrong length', () => {
    expect(normalizeUserCode('ABC-123')).toBeNull();
    expect(normalizeUserCode('ABCDE-12345')).toBeNull();
  });

  it('should reject invalid format - letters in digit section', () => {
    expect(normalizeUserCode('ABCD-EFGH')).toBeNull();
  });

  it('should reject invalid format - digits in letter section', () => {
    expect(normalizeUserCode('1234-5678')).toBeNull();
  });

  it('should handle empty string', () => {
    expect(normalizeUserCode('')).toBeNull();
  });

  // Critical test: ensure normalized code matches stored format
  it('should produce same format as generateUserCode', () => {
    const generated = generateUserCode();
    const normalized = normalizeUserCode(generated);
    expect(normalized).toBe(generated);
  });
});

describe('Expiry Timestamp', () => {
  it('should generate valid ISO-like timestamp', () => {
    const timestamp = getExpiryTimestamp(600);
    // Format: "2024-12-24 12:34:56.789"
    expect(timestamp).toMatch(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}$/);
  });

  it('should not be expired immediately', () => {
    const timestamp = getExpiryTimestamp(600);
    expect(isExpired(timestamp)).toBe(false);
  });

  it('should detect expired timestamp', () => {
    // Create timestamp 1 second in the past
    const pastTimestamp = getExpiryTimestamp(-1);
    expect(isExpired(pastTimestamp)).toBe(true);
  });

  it('should return correct remaining seconds', () => {
    const timestamp = getExpiryTimestamp(600);
    const remaining = getRemainingSeconds(timestamp);
    // Should be approximately 600 (allow 5 second tolerance for test execution)
    expect(remaining).toBeGreaterThan(595);
    expect(remaining).toBeLessThanOrEqual(600);
  });

  it('should return 0 for expired timestamp', () => {
    const pastTimestamp = getExpiryTimestamp(-100);
    expect(getRemainingSeconds(pastTimestamp)).toBe(0);
  });
});

describe('SQLite datetime compatibility', () => {
  it('expiry timestamp should be comparable with SQLite datetime', () => {
    const timestamp = getExpiryTimestamp(600);
    // SQLite datetime('now') format: "2024-12-24 12:34:56"
    const sqliteNow = new Date().toISOString().replace('T', ' ').slice(0, 19);

    // Our format has milliseconds, SQLite doesn't
    // String comparison should still work:
    // "2024-12-24 12:34:56.789" > "2024-12-24 12:30:00" should be TRUE
    expect(timestamp > sqliteNow).toBe(true);
  });
});

describe('Device Code Format', () => {
  it('should generate valid UUID', () => {
    const uuid = crypto.randomUUID();
    expect(uuid).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/);
    expect(uuid.length).toBe(36);
  });
});

describe('Flow Validation', () => {
  it('generated code should survive normalization round-trip', () => {
    for (let i = 0; i < 50; i++) {
      const original = generateUserCode();
      const normalized = normalizeUserCode(original);
      expect(normalized).toBe(original);
    }
  });

  it('user input variations should all normalize to same value', () => {
    const variations = [
      'ABCD-1234',
      'abcd-1234',
      'ABCD1234',
      'abcd1234',
      'AB CD 12 34',
      'ab-cd-12-34',
      'A B C D 1 2 3 4',
    ];

    const normalized = variations.map(v => normalizeUserCode(v));

    // All should normalize to the same value
    normalized.forEach(n => {
      expect(n).toBe('ABCD-1234');
    });
  });
});
