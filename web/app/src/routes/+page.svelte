<script lang="ts">
  import { onMount } from 'svelte';
  import { isAuthenticated, user, authFetch } from '$lib/auth';
  import { theme } from '$lib/theme';
  import { API_URL } from '$lib/config';

  interface Stats {
    total_rides: number;
    avg_balance_left: number;
    avg_balance_right: number;
    avg_te_left: number;
    avg_te_right: number;
    avg_ps_left: number;
    avg_ps_right: number;
    avg_score: number;
    total_duration_ms: number;
    avg_zone_optimal: number;
  }

  interface Ride {
    id: number;
    timestamp: number;
    duration_ms: number;
    balance_left: number;
    balance_right: number;
    score: number;
  }

  let stats: Stats | null = null;
  let recentRides: Ride[] = [];
  let loading = true;
  let error: string | null = null;

  onMount(async () => {
    if (!$isAuthenticated) {
      loading = false;
      return;
    }

    try {
      const [statsRes, ridesRes] = await Promise.all([
        authFetch('/api/rides/stats/summary'),
        authFetch('/api/rides?limit=5'),
      ]);

      if (statsRes.ok) {
        const statsData = await statsRes.json();
        if (statsData.success) stats = statsData.data;
      }

      if (ridesRes.ok) {
        const ridesData = await ridesRes.json();
        if (ridesData.success) recentRides = ridesData.data.rides || [];
      }
    } catch (err) {
      error = 'Failed to load data';
    } finally {
      loading = false;
    }
  });

  function handleLogin() {
    window.location.href = `${API_URL}/auth/login`;
  }

  function formatDuration(ms: number): string {
    const hours = Math.floor(ms / 3600000);
    const minutes = Math.floor((ms % 3600000) / 60000);
    return hours > 0 ? `${hours}h ${minutes}m` : `${minutes}m`;
  }

  function formatDate(timestamp: number): string {
    return new Date(timestamp).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
    });
  }

  function getBalanceStatus(left: number): 'optimal' | 'attention' | 'problem' {
    const diff = Math.abs(left - 50);
    if (diff <= 2.5) return 'optimal';
    if (diff <= 5) return 'attention';
    return 'problem';
  }

  function getScoreStatus(score: number): 'optimal' | 'attention' | 'problem' {
    if (score >= 80) return 'optimal';
    if (score >= 60) return 'attention';
    return 'problem';
  }

  function getFirstName(name: string | undefined): string {
    return name?.split(' ')[0] || 'Cyclist';
  }
</script>

<svelte:head>
  <title>{$isAuthenticated ? 'Dashboard' : 'KPedal - Pedaling Efficiency for Karoo'}</title>
</svelte:head>

{#if !$isAuthenticated}
  <!-- Landing Page -->
  <div class="landing">
    <!-- Theme Toggle -->
    <button class="theme-toggle" on:click={() => theme.toggle()} aria-label="Toggle theme">
      {#if $theme === 'dark' || ($theme === 'system' && typeof window !== 'undefined' && window.matchMedia('(prefers-color-scheme: dark)').matches)}
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="5"/>
          <line x1="12" y1="1" x2="12" y2="3"/>
          <line x1="12" y1="21" x2="12" y2="23"/>
          <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/>
          <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
          <line x1="1" y1="12" x2="3" y2="12"/>
          <line x1="21" y1="12" x2="23" y2="12"/>
          <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/>
          <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
        </svg>
      {:else}
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
        </svg>
      {/if}
    </button>

    <div class="landing-container">
      <!-- Hero -->
      <section class="hero">
        <div class="hero-badge">Karoo Extension</div>
        <div class="logo">
          <span class="logo-dot"></span>
          <span class="logo-text">KPedal</span>
        </div>
        <p class="hero-tagline">Real-time pedaling efficiency for Hammerhead Karoo</p>
        <p class="hero-description">
          Track balance, torque effectiveness, and pedal smoothness during your rides.
          Improve technique with guided drills and detailed analytics.
        </p>

        <button class="cta-btn" on:click={handleLogin}>
          <svg viewBox="0 0 24 24" width="20" height="20">
            <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
            <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
            <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
            <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
          </svg>
          Sign in with Google
        </button>
      </section>

      <!-- Metrics Section -->
      <section class="section">
        <h2 class="section-title">Pedaling Metrics</h2>
        <p class="section-subtitle">Research-based thresholds for optimal performance</p>

        <div class="metrics-cards">
          <div class="metric-info-card">
            <div class="metric-header">
              <span class="metric-icon balance"></span>
              <span class="metric-name">Balance</span>
            </div>
            <div class="metric-optimal">48-52%</div>
            <p class="metric-desc">Left/right power distribution. Pro cyclists maintain within 2%.</p>
          </div>

          <div class="metric-info-card">
            <div class="metric-header">
              <span class="metric-icon te"></span>
              <span class="metric-name">Torque Effectiveness</span>
            </div>
            <div class="metric-optimal">70-80%</div>
            <p class="metric-desc">Power application efficiency. Higher is NOT better - above 80% reduces total power.</p>
          </div>

          <div class="metric-info-card">
            <div class="metric-header">
              <span class="metric-icon ps"></span>
              <span class="metric-name">Pedal Smoothness</span>
            </div>
            <div class="metric-optimal">20%+</div>
            <p class="metric-desc">How smooth your pedal stroke is. Elite cyclists: 25-35%.</p>
          </div>
        </div>
      </section>

      <!-- Features Section -->
      <section class="section">
        <h2 class="section-title">Features</h2>

        <div class="features-list">
          <div class="feature-row">
            <div class="feature-icon-box">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="2" y="3" width="20" height="14" rx="2"/>
                <line x1="8" y1="21" x2="16" y2="21"/>
                <line x1="12" y1="17" x2="12" y2="21"/>
              </svg>
            </div>
            <div class="feature-content">
              <h3>6 Data Field Layouts</h3>
              <p>Quick Glance, Power Balance, Efficiency, Full Overview, Balance Trend, Single Balance</p>
            </div>
          </div>

          <div class="feature-row">
            <div class="feature-icon-box">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <polyline points="12,6 12,12 16,14"/>
              </svg>
            </div>
            <div class="feature-content">
              <h3>10 Training Drills</h3>
              <p>Left/Right Focus, Smooth Circles, Power Transfer, Balance Challenge, and more</p>
            </div>
          </div>

          <div class="feature-row">
            <div class="feature-icon-box">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polygon points="12,2 15.09,8.26 22,9.27 17,14.14 18.18,21.02 12,17.77 5.82,21.02 7,14.14 2,9.27 8.91,8.26"/>
              </svg>
            </div>
            <div class="feature-content">
              <h3>16 Achievements</h3>
              <p>Ride milestones, balance mastery, efficiency goals, streaks, and drill completion</p>
            </div>
          </div>

          <div class="feature-row">
            <div class="feature-icon-box">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
              </svg>
            </div>
            <div class="feature-content">
              <h3>7/30 Day Analytics</h3>
              <p>Track your progress with trend charts and performance insights</p>
            </div>
          </div>

          <div class="feature-row">
            <div class="feature-icon-box">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
                <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
              </svg>
            </div>
            <div class="feature-content">
              <h3>Real-time Alerts</h3>
              <p>Visual, sound, and vibration alerts when technique needs correction</p>
            </div>
          </div>

          <div class="feature-row">
            <div class="feature-icon-box">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21.21 15.89A10 10 0 1 1 8 2.83"/>
                <path d="M22 12A10 10 0 0 0 12 2v10z"/>
              </svg>
            </div>
            <div class="feature-content">
              <h3>Background Mode</h3>
              <p>Collects data for ALL rides, even without data fields on screen</p>
            </div>
          </div>
        </div>
      </section>

      <!-- Status Colors -->
      <section class="section">
        <h2 class="section-title">Status Colors</h2>
        <div class="colors-grid">
          <div class="color-item">
            <span class="color-dot white"></span>
            <span class="color-label">Normal</span>
          </div>
          <div class="color-item">
            <span class="color-dot green"></span>
            <span class="color-label">Optimal</span>
          </div>
          <div class="color-item">
            <span class="color-dot yellow"></span>
            <span class="color-label">Attention</span>
          </div>
          <div class="color-item">
            <span class="color-dot red"></span>
            <span class="color-label">Problem</span>
          </div>
        </div>
      </section>

      <!-- Compatible Pedals -->
      <section class="section">
        <h2 class="section-title">Compatible Pedals</h2>
        <p class="section-subtitle">Requires dual-sided power meter with ANT+ Cycling Dynamics</p>

        <div class="pedals-grid">
          <div class="pedal-item">
            <span class="pedal-name">Garmin Rally RS/RK</span>
            <span class="pedal-support full">Balance + TE/PS</span>
          </div>
          <div class="pedal-item">
            <span class="pedal-name">Garmin Vector 3</span>
            <span class="pedal-support full">Balance + TE/PS</span>
          </div>
          <div class="pedal-item">
            <span class="pedal-name">Favero Assioma DUO</span>
            <span class="pedal-support full">Balance + TE/PS</span>
          </div>
          <div class="pedal-item">
            <span class="pedal-name">SRM X-Power</span>
            <span class="pedal-support full">Balance + TE/PS</span>
          </div>
          <div class="pedal-item">
            <span class="pedal-name">Wahoo POWRLINK Zero</span>
            <span class="pedal-support partial">Balance only</span>
          </div>
          <div class="pedal-item">
            <span class="pedal-name">Rotor 2INpower</span>
            <span class="pedal-support full">Balance + TE/PS</span>
          </div>
        </div>
      </section>

      <!-- Requirements -->
      <section class="section requirements-section">
        <h2 class="section-title">Requirements</h2>
        <div class="requirements-list">
          <div class="requirement">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="20,6 9,17 4,12"/>
            </svg>
            <span>Hammerhead Karoo 2 or Karoo 3</span>
          </div>
          <div class="requirement">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="20,6 9,17 4,12"/>
            </svg>
            <span>Dual-sided power meter pedals</span>
          </div>
          <div class="requirement">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="20,6 9,17 4,12"/>
            </svg>
            <span>ANT+ Cycling Dynamics support</span>
          </div>
        </div>
      </section>

      <!-- CTA -->
      <section class="section cta-section">
        <button class="cta-btn large" on:click={handleLogin}>
          <svg viewBox="0 0 24 24" width="20" height="20">
            <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
            <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
            <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
            <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
          </svg>
          Get Started with Google
        </button>
        <p class="cta-note">Free to use. Your data syncs from Karoo automatically.</p>
      </section>

      <!-- Footer -->
      <footer class="landing-footer">
        <div class="footer-logo">
          <span class="logo-dot small"></span>
          <span>KPedal</span>
        </div>
        <p>Real-time pedaling efficiency for Karoo</p>
        <a href="/privacy" class="privacy-link">Privacy Policy</a>
      </footer>
    </div>
  </div>
{:else}
  <!-- Dashboard -->
  <div class="dashboard">
    <div class="container">
      {#if loading}
        <div class="loading-state">
          <div class="spinner"></div>
        </div>
      {:else if error}
        <div class="error-state">
          <p>{error}</p>
          <button class="btn btn-primary" on:click={() => location.reload()}>Retry</button>
        </div>
      {:else}
        <!-- Header -->
        <header class="page-header animate-in">
          <div class="greeting">
            <h1>Welcome back, {getFirstName($user?.name)}</h1>
            <p>Here's your pedaling efficiency overview</p>
          </div>
        </header>

        {#if stats && stats.total_rides > 0}
          <!-- Stats Grid -->
          <div class="stats-grid animate-in">
            <div class="stat-card">
              <div class="stat-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
                </svg>
              </div>
              <div class="stat-content">
                <span class="stat-value">{stats.total_rides}</span>
                <span class="stat-label">Total Rides</span>
              </div>
            </div>

            <div class="stat-card">
              <div class="stat-icon score-{getScoreStatus(stats.avg_score)}">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <polygon points="12,2 15.09,8.26 22,9.27 17,14.14 18.18,21.02 12,17.77 5.82,21.02 7,14.14 2,9.27 8.91,8.26"/>
                </svg>
              </div>
              <div class="stat-content">
                <span class="stat-value">{Math.round(stats.avg_score)}</span>
                <span class="stat-label">Avg Score</span>
              </div>
            </div>

            <div class="stat-card">
              <div class="stat-icon balance-{getBalanceStatus(stats.avg_balance_left)}">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="12" y1="2" x2="12" y2="22"/>
                  <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
                </svg>
              </div>
              <div class="stat-content">
                <span class="stat-value">{stats.avg_balance_left.toFixed(1)}%</span>
                <span class="stat-label">Left Balance</span>
              </div>
            </div>

            <div class="stat-card">
              <div class="stat-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="10"/>
                  <polyline points="12,6 12,12 16,14"/>
                </svg>
              </div>
              <div class="stat-content">
                <span class="stat-value">{formatDuration(stats.total_duration_ms)}</span>
                <span class="stat-label">Total Time</span>
              </div>
            </div>
          </div>

          <!-- Metrics Section -->
          <section class="section animate-in">
            <h2 class="section-title">Efficiency Metrics</h2>
            <div class="dashboard-metrics-grid">
              <div class="dashboard-metric-card">
                <div class="dashboard-metric-header">
                  <span class="dashboard-metric-title">Torque Effectiveness</span>
                  <span class="dashboard-metric-badge">TE</span>
                </div>
                <div class="dashboard-metric-values">
                  <div class="dashboard-metric-side">
                    <span class="side-label">Left</span>
                    <span class="side-value">{stats.avg_te_left.toFixed(1)}%</span>
                  </div>
                  <div class="dashboard-metric-divider"></div>
                  <div class="dashboard-metric-side">
                    <span class="side-label">Right</span>
                    <span class="side-value">{stats.avg_te_right.toFixed(1)}%</span>
                  </div>
                </div>
              </div>

              <div class="dashboard-metric-card">
                <div class="dashboard-metric-header">
                  <span class="dashboard-metric-title">Pedal Smoothness</span>
                  <span class="dashboard-metric-badge">PS</span>
                </div>
                <div class="dashboard-metric-values">
                  <div class="dashboard-metric-side">
                    <span class="side-label">Left</span>
                    <span class="side-value">{stats.avg_ps_left.toFixed(1)}%</span>
                  </div>
                  <div class="dashboard-metric-divider"></div>
                  <div class="dashboard-metric-side">
                    <span class="side-label">Right</span>
                    <span class="side-value">{stats.avg_ps_right.toFixed(1)}%</span>
                  </div>
                </div>
              </div>

              <div class="dashboard-metric-card">
                <div class="dashboard-metric-header">
                  <span class="dashboard-metric-title">Optimal Zone</span>
                  <span class="dashboard-metric-badge optimal">Zone</span>
                </div>
                <div class="dashboard-metric-single">
                  <span class="single-value text-optimal">{stats.avg_zone_optimal.toFixed(1)}%</span>
                  <span class="single-label">Time in optimal zone</span>
                </div>
              </div>
            </div>
          </section>

          <!-- Recent Rides -->
          {#if recentRides.length > 0}
            <section class="section animate-in">
              <div class="section-header">
                <h2 class="section-title">Recent Rides</h2>
                <a href="/rides" class="view-all-link">View all</a>
              </div>
              <div class="rides-list">
                {#each recentRides as ride}
                  <div class="ride-item">
                    <div class="ride-date">
                      <span class="date-day">{formatDate(ride.timestamp)}</span>
                      <span class="date-duration">{formatDuration(ride.duration_ms)}</span>
                    </div>
                    <div class="ride-metrics">
                      <span class="ride-balance balance-{getBalanceStatus(ride.balance_left)}">
                        {ride.balance_left.toFixed(1)}% L
                      </span>
                      <span class="ride-score score-{getScoreStatus(ride.score)}">
                        {ride.score}
                      </span>
                    </div>
                  </div>
                {/each}
              </div>
            </section>
          {/if}
        {:else}
          <!-- Empty State -->
          <div class="empty-state animate-in">
            <div class="empty-icon">
              <svg width="72" height="72" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
              </svg>
            </div>
            <h2>No rides yet</h2>
            <p>Start recording rides with KPedal on your Karoo to see your analytics here.</p>

            <div class="setup-card">
              <h3>Quick Setup</h3>
              <div class="setup-steps">
                <div class="setup-step">
                  <span class="step-number">1</span>
                  <span class="step-text">Install KPedal on your Karoo device</span>
                </div>
                <div class="setup-step">
                  <span class="step-number">2</span>
                  <span class="step-text">Add KPedal data fields to your ride profile</span>
                </div>
                <div class="setup-step">
                  <span class="step-number">3</span>
                  <span class="step-text">Start riding — data syncs automatically</span>
                </div>
              </div>
            </div>
          </div>
        {/if}
      {/if}
    </div>
  </div>
{/if}

<style>
  /* ============================================
     Landing Page
     ============================================ */
  .landing {
    min-height: 100vh;
    padding: 24px;
    position: relative;
  }

  .theme-toggle {
    position: fixed;
    top: 24px;
    right: 24px;
    width: 44px;
    height: 44px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 12px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    color: var(--text-secondary);
    cursor: pointer;
    transition: all 0.2s ease;
    z-index: 100;
  }

  .theme-toggle:hover {
    background: var(--bg-hover);
    color: var(--text-primary);
  }

  .landing-container {
    max-width: 720px;
    margin: 0 auto;
    padding: 40px 0 60px;
  }

  /* Hero */
  .hero {
    text-align: center;
    padding: 40px 0 60px;
  }

  .hero-badge {
    display: inline-block;
    padding: 6px 14px;
    background: var(--color-optimal-soft);
    color: var(--color-optimal);
    font-size: 12px;
    font-weight: 600;
    border-radius: 20px;
    margin-bottom: 24px;
  }

  .logo {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    margin-bottom: 16px;
  }

  .logo-dot {
    width: 12px;
    height: 12px;
    background: var(--color-optimal);
    border-radius: 50%;
  }

  .logo-dot.small {
    width: 8px;
    height: 8px;
  }

  .logo-text {
    font-size: 32px;
    font-weight: 700;
    color: var(--text-primary);
    letter-spacing: -0.5px;
  }

  .hero-tagline {
    font-size: 18px;
    color: var(--text-secondary);
    margin-bottom: 12px;
  }

  .hero-description {
    font-size: 15px;
    color: var(--text-tertiary);
    max-width: 480px;
    margin: 0 auto 28px;
    line-height: 1.6;
  }

  .cta-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    padding: 14px 28px;
    background: var(--bg-surface);
    border: 1px solid var(--border-default);
    border-radius: 10px;
    font-size: 15px;
    font-weight: 500;
    color: var(--text-primary);
    cursor: pointer;
    transition: all 0.2s ease;
  }

  .cta-btn:hover {
    background: var(--bg-hover);
    border-color: var(--border-strong);
  }

  .cta-btn.large {
    padding: 16px 32px;
    font-size: 16px;
  }

  /* Sections */
  .section {
    margin-bottom: 48px;
  }

  .section-title {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 8px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  .section-subtitle {
    font-size: 14px;
    color: var(--text-tertiary);
    margin-bottom: 20px;
  }

  /* Metrics Cards */
  .metrics-cards {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 12px;
  }

  .metric-info-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 16px;
  }

  .metric-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
  }

  .metric-icon {
    width: 8px;
    height: 8px;
    border-radius: 50%;
  }

  .metric-icon.balance { background: var(--color-attention); }
  .metric-icon.te { background: var(--color-optimal); }
  .metric-icon.ps { background: var(--color-problem); }

  .metric-name {
    font-size: 12px;
    font-weight: 500;
    color: var(--text-secondary);
  }

  .metric-optimal {
    font-size: 24px;
    font-weight: 700;
    color: var(--color-optimal);
    margin-bottom: 8px;
  }

  .metric-desc {
    font-size: 12px;
    color: var(--text-tertiary);
    line-height: 1.5;
  }

  /* Features List */
  .features-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .feature-row {
    display: flex;
    align-items: flex-start;
    gap: 14px;
  }

  .feature-icon-box {
    width: 40px;
    height: 40px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--color-optimal);
    flex-shrink: 0;
  }

  .feature-content h3 {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 4px;
  }

  .feature-content p {
    font-size: 13px;
    color: var(--text-tertiary);
    line-height: 1.5;
  }

  /* Colors Grid */
  .colors-grid {
    display: flex;
    gap: 24px;
  }

  .color-item {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .color-dot {
    width: 12px;
    height: 12px;
    border-radius: 50%;
  }

  .color-dot.white { background: var(--text-primary); }
  .color-dot.green { background: var(--color-optimal); }
  .color-dot.yellow { background: var(--color-attention); }
  .color-dot.red { background: var(--color-problem); }

  .color-label {
    font-size: 13px;
    color: var(--text-secondary);
  }

  /* Pedals Grid */
  .pedals-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
  }

  .pedal-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 14px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 8px;
  }

  .pedal-name {
    font-size: 13px;
    color: var(--text-primary);
  }

  .pedal-support {
    font-size: 11px;
    padding: 3px 8px;
    border-radius: 4px;
  }

  .pedal-support.full {
    background: var(--color-optimal-soft);
    color: var(--color-optimal);
  }

  .pedal-support.partial {
    background: var(--color-attention-soft);
    color: var(--color-attention);
  }

  /* Requirements */
  .requirements-section {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 20px;
  }

  .requirements-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .requirement {
    display: flex;
    align-items: center;
    gap: 10px;
    color: var(--text-secondary);
    font-size: 14px;
  }

  .requirement svg {
    color: var(--color-optimal);
    flex-shrink: 0;
  }

  /* CTA Section */
  .cta-section {
    text-align: center;
    padding: 32px 0;
  }

  .cta-note {
    margin-top: 16px;
    font-size: 13px;
    color: var(--text-tertiary);
  }

  /* Footer */
  .landing-footer {
    text-align: center;
    padding-top: 32px;
    border-top: 1px solid var(--border-subtle);
  }

  .footer-logo {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    margin-bottom: 8px;
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
  }

  .landing-footer p {
    font-size: 13px;
    color: var(--text-muted);
  }

  .privacy-link {
    display: inline-block;
    margin-top: 16px;
    font-size: 13px;
    color: var(--text-tertiary);
  }

  .privacy-link:hover {
    color: var(--color-accent);
  }

  /* ============================================
     Dashboard
     ============================================ */
  .dashboard {
    padding: 32px 0 64px;
  }

  .loading-state, .error-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 60vh;
    gap: 16px;
    color: var(--text-secondary);
  }

  .page-header {
    margin-bottom: 32px;
  }

  .greeting h1 {
    font-size: 28px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 4px;
    letter-spacing: -0.5px;
  }

  .greeting p {
    font-size: 15px;
    color: var(--text-secondary);
  }

  /* Stats Grid */
  .stats-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
    margin-bottom: 40px;
  }

  .stat-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 16px;
    display: flex;
    align-items: flex-start;
    gap: 12px;
  }

  .stat-icon {
    width: 36px;
    height: 36px;
    border-radius: 8px;
    background: var(--bg-elevated);
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--text-secondary);
    flex-shrink: 0;
  }

  .stat-icon.score-optimal, .stat-icon.balance-optimal {
    background: var(--color-optimal-soft);
    color: var(--color-optimal);
  }
  .stat-icon.score-attention, .stat-icon.balance-attention {
    background: var(--color-attention-soft);
    color: var(--color-attention);
  }
  .stat-icon.score-problem, .stat-icon.balance-problem {
    background: var(--color-problem-soft);
    color: var(--color-problem);
  }

  .stat-content {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .stat-value {
    font-size: 22px;
    font-weight: 600;
    color: var(--text-primary);
    line-height: 1.2;
  }

  .stat-label {
    font-size: 12px;
    color: var(--text-tertiary);
  }

  /* Dashboard Metrics */
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
  }

  .view-all-link {
    font-size: 13px;
    color: var(--text-tertiary);
  }

  .view-all-link:hover {
    color: var(--color-accent);
  }

  .dashboard-metrics-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;
  }

  .dashboard-metric-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 16px;
  }

  .dashboard-metric-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 14px;
  }

  .dashboard-metric-title {
    font-size: 13px;
    color: var(--text-secondary);
  }

  .dashboard-metric-badge {
    font-size: 10px;
    font-weight: 600;
    padding: 3px 6px;
    border-radius: 4px;
    background: var(--bg-elevated);
    color: var(--text-tertiary);
  }

  .dashboard-metric-badge.optimal {
    background: var(--color-optimal-soft);
    color: var(--color-optimal);
  }

  .dashboard-metric-values {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .dashboard-metric-side {
    flex: 1;
    text-align: center;
  }

  .side-label {
    display: block;
    font-size: 11px;
    color: var(--text-muted);
    margin-bottom: 4px;
  }

  .side-value {
    font-size: 20px;
    font-weight: 600;
    color: var(--text-primary);
  }

  .dashboard-metric-divider {
    width: 1px;
    height: 32px;
    background: var(--border-subtle);
  }

  .dashboard-metric-single {
    text-align: center;
  }

  .single-value {
    display: block;
    font-size: 28px;
    font-weight: 600;
    margin-bottom: 4px;
  }

  .single-label {
    font-size: 11px;
    color: var(--text-muted);
  }

  /* Rides List */
  .rides-list {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    overflow: hidden;
  }

  .ride-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 14px 16px;
    border-bottom: 1px solid var(--border-subtle);
  }

  .ride-item:last-child {
    border-bottom: none;
  }

  .ride-date {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .date-day {
    font-size: 14px;
    font-weight: 500;
    color: var(--text-primary);
  }

  .date-duration {
    font-size: 12px;
    color: var(--text-tertiary);
  }

  .ride-metrics {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .ride-balance, .ride-score {
    font-size: 13px;
    font-weight: 500;
    padding: 4px 8px;
    border-radius: 6px;
  }

  .ride-balance.balance-optimal { background: var(--color-optimal-soft); color: var(--color-optimal); }
  .ride-balance.balance-attention { background: var(--color-attention-soft); color: var(--color-attention); }
  .ride-balance.balance-problem { background: var(--color-problem-soft); color: var(--color-problem); }

  .ride-score.score-optimal { background: var(--color-optimal-soft); color: var(--color-optimal); }
  .ride-score.score-attention { background: var(--color-attention-soft); color: var(--color-attention); }
  .ride-score.score-problem { background: var(--color-problem-soft); color: var(--color-problem); }

  /* Empty State */
  .empty-state {
    text-align: center;
    padding: 80px 24px;
  }

  .empty-icon {
    margin-bottom: 24px;
    color: var(--text-muted);
  }

  .empty-state h2 {
    font-size: 24px;
    font-weight: 600;
    margin-bottom: 8px;
    color: var(--text-primary);
  }

  .empty-state > p {
    color: var(--text-secondary);
    margin-bottom: 32px;
  }

  .setup-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 20px;
    text-align: left;
    max-width: 360px;
    margin: 0 auto;
  }

  .setup-card h3 {
    font-size: 14px;
    font-weight: 600;
    margin-bottom: 16px;
    color: var(--text-primary);
  }

  .setup-steps {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .setup-step {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .step-number {
    width: 24px;
    height: 24px;
    border-radius: 50%;
    background: var(--color-optimal);
    color: white;
    font-size: 12px;
    font-weight: 600;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .step-text {
    font-size: 13px;
    color: var(--text-secondary);
    text-align: left;
  }

  /* Responsive */
  @media (max-width: 768px) {
    .metrics-cards {
      grid-template-columns: 1fr;
    }
    .pedals-grid {
      grid-template-columns: 1fr;
    }
    .colors-grid {
      flex-wrap: wrap;
    }
    .stats-grid {
      grid-template-columns: repeat(2, 1fr);
    }
    .dashboard-metrics-grid {
      grid-template-columns: 1fr;
    }
  }

  @media (max-width: 480px) {
    .stats-grid {
      grid-template-columns: 1fr;
    }
    .logo-text {
      font-size: 28px;
    }
  }
</style>
