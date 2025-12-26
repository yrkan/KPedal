import { describe, it, expect, beforeEach } from 'vitest';

/**
 * Unit tests for user repository functions
 *
 * Tests CRUD operations for user management
 */

// Mock D1 database
class MockD1Database {
  private users: Map<string, MockUser> = new Map();
  private rides: Map<string, { user_id: string }> = new Map();
  private lastPrepared: string = '';
  private boundValues: unknown[] = [];

  constructor() {
    this.reset();
  }

  reset() {
    this.users.clear();
    this.rides.clear();
    this.lastPrepared = '';
    this.boundValues = [];
  }

  seedUser(user: MockUser) {
    this.users.set(user.id, user);
  }

  seedRide(id: string, userId: string) {
    this.rides.set(id, { user_id: userId });
  }

  getUser(id: string): MockUser | undefined {
    return this.users.get(id);
  }

  getUserCount(): number {
    return this.users.size;
  }

  getRideCount(): number {
    return this.rides.size;
  }

  prepare(sql: string) {
    this.lastPrepared = sql;
    this.boundValues = [];
    return this;
  }

  bind(...values: unknown[]) {
    this.boundValues = values;
    return this;
  }

  async first<T>(): Promise<T | null> {
    const sql = this.lastPrepared.toLowerCase();

    if (sql.includes('select') && sql.includes('from users')) {
      if (sql.includes('google_id = ?')) {
        const googleId = this.boundValues[0] as string;
        for (const user of this.users.values()) {
          if (user.google_id === googleId) {
            return user as T;
          }
        }
        return null;
      }
      if (sql.includes('where id = ?')) {
        const id = this.boundValues[0] as string;
        return (this.users.get(id) as T) || null;
      }
      if (sql.includes('where email = ?')) {
        const email = this.boundValues[0] as string;
        for (const user of this.users.values()) {
          if (user.email === email) {
            return user as T;
          }
        }
        return null;
      }
    }

    return null;
  }

  async run(): Promise<{ success: boolean }> {
    const sql = this.lastPrepared.toLowerCase();

    if (sql.includes('update users')) {
      const id = this.boundValues[2] as string;
      const user = this.users.get(id);
      if (user) {
        user.name = this.boundValues[0] as string;
        user.picture = this.boundValues[1] as string;
        user.updated_at = new Date().toISOString();
      }
      return { success: true };
    }

    if (sql.includes('insert into users')) {
      const [id, email, name, picture, googleId] = this.boundValues as string[];
      this.users.set(id, {
        id,
        email,
        name,
        picture,
        google_id: googleId,
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString(),
      });
      return { success: true };
    }

    if (sql.includes('delete from rides')) {
      const userId = this.boundValues[0] as string;
      for (const [id, ride] of this.rides) {
        if (ride.user_id === userId) {
          this.rides.delete(id);
        }
      }
      return { success: true };
    }

    if (sql.includes('delete from users')) {
      const id = this.boundValues[0] as string;
      this.users.delete(id);
      return { success: true };
    }

    return { success: true };
  }
}

interface MockUser {
  id: string;
  email: string;
  name: string;
  picture: string;
  google_id: string;
  created_at: string;
  updated_at: string;
}

interface GoogleUserData {
  googleId: string;
  email: string;
  name: string;
  picture: string;
}

// Inline user functions for testing (mirrors user.ts)
async function getOrCreateUser(
  db: MockD1Database,
  data: GoogleUserData
): Promise<MockUser> {
  const existing = await db
    .prepare('SELECT * FROM users WHERE google_id = ?')
    .bind(data.googleId)
    .first<MockUser>();

  if (existing) {
    await db
      .prepare(`
        UPDATE users
        SET name = ?, picture = ?, updated_at = datetime('now')
        WHERE id = ?
      `)
      .bind(data.name, data.picture, existing.id)
      .run();

    return {
      ...existing,
      name: data.name,
      picture: data.picture,
    };
  }

  const id = crypto.randomUUID();

  await db
    .prepare(`
      INSERT INTO users (id, email, name, picture, google_id, created_at, updated_at)
      VALUES (?, ?, ?, ?, ?, datetime('now'), datetime('now'))
    `)
    .bind(id, data.email, data.name, data.picture, data.googleId)
    .run();

  return {
    id,
    email: data.email,
    name: data.name,
    picture: data.picture,
    google_id: data.googleId,
    created_at: new Date().toISOString(),
    updated_at: new Date().toISOString(),
  };
}

async function getUserById(
  db: MockD1Database,
  id: string
): Promise<MockUser | null> {
  return db.prepare('SELECT * FROM users WHERE id = ?').bind(id).first<MockUser>();
}

async function getUserByEmail(
  db: MockD1Database,
  email: string
): Promise<MockUser | null> {
  return db
    .prepare('SELECT * FROM users WHERE email = ?')
    .bind(email)
    .first<MockUser>();
}

async function deleteUser(db: MockD1Database, id: string): Promise<void> {
  await db.prepare('DELETE FROM rides WHERE user_id = ?').bind(id).run();
  await db.prepare('DELETE FROM users WHERE id = ?').bind(id).run();
}

describe('User Repository - getOrCreateUser', () => {
  let db: MockD1Database;

  beforeEach(() => {
    db = new MockD1Database();
  });

  it('should create new user when not exists', async () => {
    const googleData: GoogleUserData = {
      googleId: 'google-123',
      email: 'test@example.com',
      name: 'Test User',
      picture: 'https://example.com/photo.jpg',
    };

    const user = await getOrCreateUser(db, googleData);

    expect(user.email).toBe('test@example.com');
    expect(user.name).toBe('Test User');
    expect(user.picture).toBe('https://example.com/photo.jpg');
    expect(user.google_id).toBe('google-123');
    expect(user.id).toBeDefined();
    expect(db.getUserCount()).toBe(1);
  });

  it('should return existing user when found by google_id', async () => {
    const existingUser: MockUser = {
      id: 'user-456',
      email: 'existing@example.com',
      name: 'Old Name',
      picture: 'https://old.com/photo.jpg',
      google_id: 'google-123',
      created_at: '2024-01-01T00:00:00Z',
      updated_at: '2024-01-01T00:00:00Z',
    };
    db.seedUser(existingUser);

    const googleData: GoogleUserData = {
      googleId: 'google-123',
      email: 'existing@example.com',
      name: 'New Name',
      picture: 'https://new.com/photo.jpg',
    };

    const user = await getOrCreateUser(db, googleData);

    expect(user.id).toBe('user-456');
    expect(user.name).toBe('New Name');
    expect(user.picture).toBe('https://new.com/photo.jpg');
    expect(db.getUserCount()).toBe(1); // No new user created
  });

  it('should update name and picture for existing user', async () => {
    const existingUser: MockUser = {
      id: 'user-789',
      email: 'test@example.com',
      name: 'Original Name',
      picture: 'https://original.com/photo.jpg',
      google_id: 'google-456',
      created_at: '2024-01-01T00:00:00Z',
      updated_at: '2024-01-01T00:00:00Z',
    };
    db.seedUser(existingUser);

    const googleData: GoogleUserData = {
      googleId: 'google-456',
      email: 'test@example.com',
      name: 'Updated Name',
      picture: 'https://updated.com/photo.jpg',
    };

    await getOrCreateUser(db, googleData);

    const updatedUser = db.getUser('user-789');
    expect(updatedUser?.name).toBe('Updated Name');
    expect(updatedUser?.picture).toBe('https://updated.com/photo.jpg');
  });

  it('should generate unique ID for new user', async () => {
    const googleData1: GoogleUserData = {
      googleId: 'google-1',
      email: 'user1@example.com',
      name: 'User 1',
      picture: 'https://example.com/1.jpg',
    };
    const googleData2: GoogleUserData = {
      googleId: 'google-2',
      email: 'user2@example.com',
      name: 'User 2',
      picture: 'https://example.com/2.jpg',
    };

    const user1 = await getOrCreateUser(db, googleData1);
    const user2 = await getOrCreateUser(db, googleData2);

    expect(user1.id).not.toBe(user2.id);
    expect(db.getUserCount()).toBe(2);
  });
});

describe('User Repository - getUserById', () => {
  let db: MockD1Database;

  beforeEach(() => {
    db = new MockD1Database();
  });

  it('should return user when found', async () => {
    const existingUser: MockUser = {
      id: 'user-123',
      email: 'test@example.com',
      name: 'Test User',
      picture: 'https://example.com/photo.jpg',
      google_id: 'google-123',
      created_at: '2024-01-01T00:00:00Z',
      updated_at: '2024-01-01T00:00:00Z',
    };
    db.seedUser(existingUser);

    const user = await getUserById(db, 'user-123');

    expect(user).not.toBeNull();
    expect(user?.id).toBe('user-123');
    expect(user?.email).toBe('test@example.com');
  });

  it('should return null when user not found', async () => {
    const user = await getUserById(db, 'nonexistent-id');

    expect(user).toBeNull();
  });

  it('should return correct user among multiple', async () => {
    db.seedUser({
      id: 'user-1',
      email: 'user1@example.com',
      name: 'User 1',
      picture: '',
      google_id: 'g1',
      created_at: '',
      updated_at: '',
    });
    db.seedUser({
      id: 'user-2',
      email: 'user2@example.com',
      name: 'User 2',
      picture: '',
      google_id: 'g2',
      created_at: '',
      updated_at: '',
    });

    const user = await getUserById(db, 'user-2');

    expect(user?.email).toBe('user2@example.com');
  });
});

describe('User Repository - getUserByEmail', () => {
  let db: MockD1Database;

  beforeEach(() => {
    db = new MockD1Database();
  });

  it('should return user when found by email', async () => {
    db.seedUser({
      id: 'user-123',
      email: 'test@example.com',
      name: 'Test User',
      picture: '',
      google_id: 'g123',
      created_at: '',
      updated_at: '',
    });

    const user = await getUserByEmail(db, 'test@example.com');

    expect(user).not.toBeNull();
    expect(user?.id).toBe('user-123');
  });

  it('should return null when email not found', async () => {
    const user = await getUserByEmail(db, 'nonexistent@example.com');

    expect(user).toBeNull();
  });

  it('should be case-sensitive for email lookup', async () => {
    db.seedUser({
      id: 'user-123',
      email: 'Test@Example.com',
      name: 'Test User',
      picture: '',
      google_id: 'g123',
      created_at: '',
      updated_at: '',
    });

    // Exact match
    const found = await getUserByEmail(db, 'Test@Example.com');
    expect(found).not.toBeNull();

    // Different case - depends on DB behavior, our mock is case-sensitive
    const notFound = await getUserByEmail(db, 'test@example.com');
    expect(notFound).toBeNull();
  });
});

describe('User Repository - deleteUser', () => {
  let db: MockD1Database;

  beforeEach(() => {
    db = new MockD1Database();
  });

  it('should delete user from database', async () => {
    db.seedUser({
      id: 'user-123',
      email: 'test@example.com',
      name: 'Test User',
      picture: '',
      google_id: 'g123',
      created_at: '',
      updated_at: '',
    });
    expect(db.getUserCount()).toBe(1);

    await deleteUser(db, 'user-123');

    expect(db.getUserCount()).toBe(0);
    expect(db.getUser('user-123')).toBeUndefined();
  });

  it('should delete user rides before deleting user', async () => {
    db.seedUser({
      id: 'user-123',
      email: 'test@example.com',
      name: 'Test',
      picture: '',
      google_id: 'g123',
      created_at: '',
      updated_at: '',
    });
    db.seedRide('ride-1', 'user-123');
    db.seedRide('ride-2', 'user-123');
    db.seedRide('ride-3', 'other-user');

    expect(db.getRideCount()).toBe(3);

    await deleteUser(db, 'user-123');

    expect(db.getRideCount()).toBe(1); // Only other-user's ride remains
    expect(db.getUserCount()).toBe(0);
  });

  it('should handle deletion of nonexistent user gracefully', async () => {
    expect(db.getUserCount()).toBe(0);

    // Should not throw
    await deleteUser(db, 'nonexistent-id');

    expect(db.getUserCount()).toBe(0);
  });

  it('should not affect other users', async () => {
    db.seedUser({
      id: 'user-1',
      email: 'user1@example.com',
      name: 'User 1',
      picture: '',
      google_id: 'g1',
      created_at: '',
      updated_at: '',
    });
    db.seedUser({
      id: 'user-2',
      email: 'user2@example.com',
      name: 'User 2',
      picture: '',
      google_id: 'g2',
      created_at: '',
      updated_at: '',
    });

    await deleteUser(db, 'user-1');

    expect(db.getUserCount()).toBe(1);
    expect(db.getUser('user-2')).toBeDefined();
    expect(db.getUser('user-1')).toBeUndefined();
  });
});

describe('User Repository - Edge Cases', () => {
  let db: MockD1Database;

  beforeEach(() => {
    db = new MockD1Database();
  });

  it('should handle special characters in name', async () => {
    const googleData: GoogleUserData = {
      googleId: 'google-special',
      email: 'special@example.com',
      name: "O'Brien & Co. <script>",
      picture: 'https://example.com/photo.jpg',
    };

    const user = await getOrCreateUser(db, googleData);

    expect(user.name).toBe("O'Brien & Co. <script>");
  });

  it('should handle unicode in name', async () => {
    const googleData: GoogleUserData = {
      googleId: 'google-unicode',
      email: 'unicode@example.com',
      name: '田中太郎 🚴',
      picture: 'https://example.com/photo.jpg',
    };

    const user = await getOrCreateUser(db, googleData);

    expect(user.name).toBe('田中太郎 🚴');
  });

  it('should handle very long picture URL', async () => {
    const longUrl = 'https://example.com/' + 'a'.repeat(2000) + '.jpg';
    const googleData: GoogleUserData = {
      googleId: 'google-long',
      email: 'long@example.com',
      name: 'Long URL User',
      picture: longUrl,
    };

    const user = await getOrCreateUser(db, googleData);

    expect(user.picture).toBe(longUrl);
  });

  it('should handle empty picture URL', async () => {
    const googleData: GoogleUserData = {
      googleId: 'google-nopic',
      email: 'nopic@example.com',
      name: 'No Picture User',
      picture: '',
    };

    const user = await getOrCreateUser(db, googleData);

    expect(user.picture).toBe('');
  });
});
