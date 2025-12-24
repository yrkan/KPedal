import { User } from '../types/env';

interface GoogleUserData {
  googleId: string;
  email: string;
  name: string;
  picture: string;
}

/**
 * Get existing user by Google ID or create new one
 */
export async function getOrCreateUser(
  db: D1Database,
  data: GoogleUserData
): Promise<User> {
  // Try to find existing user
  const existing = await db
    .prepare('SELECT * FROM users WHERE google_id = ?')
    .bind(data.googleId)
    .first<User>();

  if (existing) {
    // Update user info (name/picture might change)
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

  // Create new user
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

/**
 * Get user by ID
 */
export async function getUserById(
  db: D1Database,
  id: string
): Promise<User | null> {
  return db
    .prepare('SELECT * FROM users WHERE id = ?')
    .bind(id)
    .first<User>();
}

/**
 * Get user by email
 */
export async function getUserByEmail(
  db: D1Database,
  email: string
): Promise<User | null> {
  return db
    .prepare('SELECT * FROM users WHERE email = ?')
    .bind(email)
    .first<User>();
}

/**
 * Delete user and all their data
 */
export async function deleteUser(
  db: D1Database,
  id: string
): Promise<void> {
  // Delete rides first (foreign key)
  await db.prepare('DELETE FROM rides WHERE user_id = ?').bind(id).run();
  // Delete user
  await db.prepare('DELETE FROM users WHERE id = ?').bind(id).run();
}
