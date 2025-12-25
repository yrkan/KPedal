import { Hono } from 'hono';
import { Env, ApiResponse } from '../types/env';
import { revokeRefreshTokensForDevice } from '../auth/jwt';

const devices = new Hono<{ Bindings: Env }>();

/**
 * GET /api/devices
 * Get all devices for the current user
 */
devices.get('/', async (c) => {
  const user = c.get('user');
  const env = c.env;

  try {
    const result = await env.DB.prepare(
      `SELECT id, name, type, last_sync, created_at
       FROM devices
       WHERE user_id = ?
       ORDER BY last_sync DESC NULLS LAST, created_at DESC`
    ).bind(user.sub).all<{
      id: string;
      name: string;
      type: string;
      last_sync: string | null;
      created_at: string;
    }>();

    // Calculate connection status for each device
    const devicesWithStatus = result.results.map(device => {
      let status: 'connected' | 'idle' | 'offline' = 'offline';
      let lastSyncRelative: string | null = null;

      if (device.last_sync) {
        const lastSyncDate = new Date(device.last_sync.replace(' ', 'T') + 'Z');
        const now = Date.now();
        const diffMs = now - lastSyncDate.getTime();
        const diffMinutes = Math.floor(diffMs / 60000);
        const diffHours = Math.floor(diffMs / 3600000);
        const diffDays = Math.floor(diffMs / 86400000);

        // Status based on last activity time
        // connected: active within last 30 min (device is likely being used)
        // idle: seen within last 24 hours (device was used today)
        // offline: not seen for more than 24 hours
        if (diffMinutes < 30) {
          status = 'connected';
        } else if (diffHours < 24) {
          status = 'idle';
        } else {
          status = 'offline';
        }

        // Human-readable relative time
        if (diffMinutes < 1) {
          lastSyncRelative = 'Just now';
        } else if (diffMinutes < 60) {
          lastSyncRelative = `${diffMinutes} min ago`;
        } else if (diffHours < 24) {
          lastSyncRelative = `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
        } else if (diffDays < 7) {
          lastSyncRelative = `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
        } else {
          lastSyncRelative = lastSyncDate.toLocaleDateString();
        }
      } else {
        // Device just linked but never synced/pinged
        status = 'idle';
        lastSyncRelative = 'Just linked';
      }

      return {
        ...device,
        status,
        last_sync_relative: lastSyncRelative,
      };
    });

    return c.json<ApiResponse>({
      success: true,
      data: {
        devices: devicesWithStatus,
        count: devicesWithStatus.length,
      },
    });
  } catch (err) {
    console.error('Error fetching devices:', err);
    return c.json<ApiResponse>({
      success: false,
      error: 'Failed to fetch devices',
    }, 500);
  }
});

/**
 * POST /api/devices/:id/request-sync
 * Request a sync from the device (device will check this flag)
 */
devices.post('/:id/request-sync', async (c) => {
  const user = c.get('user');
  const env = c.env;
  const deviceId = c.req.param('id');

  try {
    // Verify device belongs to user
    const device = await env.DB.prepare(
      'SELECT id FROM devices WHERE id = ? AND user_id = ?'
    ).bind(deviceId, user.sub).first();

    if (!device) {
      return c.json<ApiResponse>({
        success: false,
        error: 'Device not found',
      }, 404);
    }

    // Store sync request in KV with 5 minute TTL
    await env.SESSIONS.put(
      `sync_request:${deviceId}`,
      JSON.stringify({ requestedAt: Date.now(), userId: user.sub }),
      { expirationTtl: 300 }
    );

    return c.json<ApiResponse>({
      success: true,
      message: 'Sync requested. Open KPedal on your device to sync.',
    });
  } catch (err) {
    console.error('Error requesting sync:', err);
    return c.json<ApiResponse>({
      success: false,
      error: 'Failed to request sync',
    }, 500);
  }
});

/**
 * DELETE /api/devices/:id
 * Remove a device from the user's account
 */
devices.delete('/:id', async (c) => {
  const user = c.get('user');
  const env = c.env;
  const deviceId = c.req.param('id');

  try {
    // Verify device belongs to user before deleting
    const device = await env.DB.prepare(
      'SELECT id FROM devices WHERE id = ? AND user_id = ?'
    ).bind(deviceId, user.sub).first();

    if (!device) {
      return c.json<ApiResponse>({
        success: false,
        error: 'Device not found',
      }, 404);
    }

    // Revoke all refresh tokens for this device (security: prevent continued access)
    const revokedTokens = await revokeRefreshTokensForDevice(env, user.sub, deviceId);
    console.log(`Revoked ${revokedTokens} refresh token(s) for device ${deviceId}`);

    // Delete the device
    await env.DB.prepare(
      'DELETE FROM devices WHERE id = ? AND user_id = ?'
    ).bind(deviceId, user.sub).run();

    return c.json<ApiResponse>({
      success: true,
      message: 'Device removed',
    });
  } catch (err) {
    console.error('Error deleting device:', err);
    return c.json<ApiResponse>({
      success: false,
      error: 'Failed to remove device',
    }, 500);
  }
});

export { devices as devicesRoutes };
