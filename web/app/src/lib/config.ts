// API Configuration
// No secrets here - only public URLs
export const API_URL = import.meta.env.VITE_API_URL || 'https://api.kpedal.com';

// Token expiry buffer (refresh 1 minute before expiry)
export const TOKEN_REFRESH_BUFFER_MS = 60 * 1000;
