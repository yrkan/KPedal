<script lang="ts">
  import { isAuthenticated } from '$lib/auth';
  import { theme } from '$lib/theme';
</script>

<svelte:head>
  <title>Privacy Policy - KPedal</title>
  <meta name="description" content="KPedal Privacy Policy - Learn how we collect, use, and protect your data.">
</svelte:head>

<div class="privacy-page">
  <div class="privacy-container">
    {#if !$isAuthenticated}
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
    {/if}

    <header class="privacy-header">
      <a href="/" class="back-link">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15,18 9,12 15,6"/>
        </svg>
        Back to KPedal
      </a>
      <h1>Privacy Policy</h1>
      <p class="last-updated">Last updated: December 24, 2025</p>
    </header>

    <!-- Quick Summary -->
    <section class="quick-summary">
      <div class="summary-grid">
        <div class="summary-item">
          <div class="summary-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
            </svg>
          </div>
          <div class="summary-text">
            <strong>Encrypted & Secure</strong>
            <span>All data encrypted in transit and at rest</span>
          </div>
        </div>
        <div class="summary-item">
          <div class="summary-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"/>
              <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/>
            </svg>
          </div>
          <div class="summary-text">
            <strong>No Location Tracking</strong>
            <span>We never collect GPS or route data</span>
          </div>
        </div>
        <div class="summary-item">
          <div class="summary-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="12" y1="1" x2="12" y2="23"/>
              <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
            </svg>
          </div>
          <div class="summary-text">
            <strong>Never Sold</strong>
            <span>Your data is never sold to anyone</span>
          </div>
        </div>
        <div class="summary-item">
          <div class="summary-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="3,6 5,6 21,6"/>
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
            </svg>
          </div>
          <div class="summary-text">
            <strong>Your Control</strong>
            <span>Delete your data anytime</span>
          </div>
        </div>
      </div>
    </section>

    <div class="privacy-content">
      <!-- Table of Contents -->
      <nav class="toc">
        <h2>Contents</h2>
        <ol>
          <li><a href="#what-we-collect">What We Collect</a></li>
          <li><a href="#what-we-dont-collect">What We Don't Collect</a></li>
          <li><a href="#how-data-flows">How Data Flows</a></li>
          <li><a href="#storage-security">Storage & Security</a></li>
          <li><a href="#your-rights">Your Rights</a></li>
          <li><a href="#third-parties">Third-Party Services</a></li>
          <li><a href="#contact">Contact</a></li>
        </ol>
      </nav>

      <!-- Section 1: What We Collect -->
      <section id="what-we-collect" class="privacy-section">
        <h2>1. What We Collect</h2>

        <h3>Account Data</h3>
        <p>When you sign in with Google, we receive and store:</p>
        <div class="data-table">
          <div class="data-row">
            <span class="data-field">Email</span>
            <span class="data-purpose">Account identification, login</span>
          </div>
          <div class="data-row">
            <span class="data-field">Name</span>
            <span class="data-purpose">Display in dashboard</span>
          </div>
          <div class="data-row">
            <span class="data-field">Profile Picture URL</span>
            <span class="data-purpose">Display as avatar</span>
          </div>
          <div class="data-row">
            <span class="data-field">Google ID</span>
            <span class="data-purpose">Link Google account to KPedal</span>
          </div>
        </div>
        <p class="note">We do not have access to your Google password. Authentication uses OAuth 2.0.</p>

        <h3>Pedaling Metrics</h3>
        <p>For each ride synced from your Karoo device:</p>
        <div class="data-table">
          <div class="data-row">
            <span class="data-field">Timestamp</span>
            <span class="data-purpose">When the ride was recorded</span>
          </div>
          <div class="data-row">
            <span class="data-field">Duration</span>
            <span class="data-purpose">How long the ride lasted (milliseconds)</span>
          </div>
          <div class="data-row">
            <span class="data-field">Balance Left / Right</span>
            <span class="data-purpose">Power distribution between legs (%)</span>
          </div>
          <div class="data-row">
            <span class="data-field">Torque Effectiveness L/R</span>
            <span class="data-purpose">How efficiently you apply power (%)</span>
          </div>
          <div class="data-row">
            <span class="data-field">Pedal Smoothness L/R</span>
            <span class="data-purpose">How smooth your pedal stroke is (%)</span>
          </div>
          <div class="data-row">
            <span class="data-field">Zone Distribution</span>
            <span class="data-purpose">Time in Optimal/Attention/Problem zones (%)</span>
          </div>
          <div class="data-row">
            <span class="data-field">Efficiency Score</span>
            <span class="data-purpose">Calculated overall score (0-100)</span>
          </div>
        </div>

        <h3>Device Data</h3>
        <div class="data-table">
          <div class="data-row">
            <span class="data-field">Device ID</span>
            <span class="data-purpose">Unique identifier for your Karoo</span>
          </div>
          <div class="data-row">
            <span class="data-field">Device Name</span>
            <span class="data-purpose">e.g., "Karoo 3"</span>
          </div>
          <div class="data-row">
            <span class="data-field">Device Type</span>
            <span class="data-purpose">e.g., "karoo"</span>
          </div>
          <div class="data-row">
            <span class="data-field">Last Sync Time</span>
            <span class="data-purpose">When data was last synced</span>
          </div>
        </div>

        <h3>User Settings (Synced if Signed In)</h3>
        <p>When signed in, your app settings are synced between your Karoo device and web dashboard:</p>
        <div class="data-table">
          <div class="data-row">
            <span class="data-field">Threshold Settings</span>
            <span class="data-purpose">Balance threshold, TE optimal range, PS minimum</span>
          </div>
          <div class="data-row">
            <span class="data-field">Alert Preferences</span>
            <span class="data-purpose">Which metrics trigger alerts, vibration/sound/visual options</span>
          </div>
          <div class="data-row">
            <span class="data-field">Alert Cooldowns</span>
            <span class="data-purpose">Time between repeated alerts (seconds)</span>
          </div>
          <div class="data-row">
            <span class="data-field">Screen Wake</span>
            <span class="data-purpose">Whether alerts wake the Karoo screen</span>
          </div>
          <div class="data-row">
            <span class="data-field">Background Mode</span>
            <span class="data-purpose">Collect data even when data field is hidden</span>
          </div>
          <div class="data-row">
            <span class="data-field">Auto-Sync</span>
            <span class="data-purpose">Automatically sync rides after completion</span>
          </div>
        </div>
        <p class="note">Settings sync allows you to configure thresholds on the web and have them apply on your Karoo automatically. If not signed in, settings remain local only.</p>

        <h3>Data Stored Only on Your Karoo (Never Synced)</h3>
        <p>The following data is stored locally on your Karoo device and is <strong>never</strong> sent to our servers:</p>
        <div class="data-table">
          <div class="data-row">
            <span class="data-field">Ride Ratings</span>
            <span class="data-purpose">1-5 star ratings you assign to rides</span>
          </div>
          <div class="data-row">
            <span class="data-field">Ride Notes</span>
            <span class="data-purpose">Personal notes you add to rides</span>
          </div>
          <div class="data-row">
            <span class="data-field">Drill Results</span>
            <span class="data-purpose">Scores, time in target, phase performance</span>
          </div>
          <div class="data-row">
            <span class="data-field">Achievements</span>
            <span class="data-purpose">Unlocked achievements and progress</span>
          </div>
          <div class="data-row">
            <span class="data-field">Custom Drills</span>
            <span class="data-purpose">Training drills you create yourself</span>
          </div>
          <div class="data-row">
            <span class="data-field">Onboarding Status</span>
            <span class="data-purpose">Whether you've completed the app tutorial</span>
          </div>
        </div>
        <p class="note">This data remains on your device. If you uninstall the app, this data is deleted.</p>
      </section>

      <!-- Section 2: What We Don't Collect -->
      <section id="what-we-dont-collect" class="privacy-section">
        <h2>2. What We Don't Collect</h2>
        <p>KPedal is focused exclusively on pedaling efficiency. We do not collect:</p>

        <div class="dont-collect-grid">
          <div class="dont-item">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
            <span>GPS / Location</span>
          </div>
          <div class="dont-item">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
            <span>Route / Map Data</span>
          </div>
          <div class="dont-item">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
            <span>Heart Rate</span>
          </div>
          <div class="dont-item">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
            <span>Speed / Distance</span>
          </div>
          <div class="dont-item">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
            <span>Elevation / Climbing</span>
          </div>
          <div class="dont-item">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
            <span>Temperature</span>
          </div>
          <div class="dont-item">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
            <span>Total Power Output</span>
          </div>
          <div class="dont-item">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
            <span>Strava / Other Platforms</span>
          </div>
        </div>

        <h3>Data Used But Not Stored</h3>
        <p>The following data is read in real-time for display purposes but is <strong>never saved</strong> to device or cloud:</p>
        <div class="data-table">
          <div class="data-row">
            <span class="data-field">Power (Watts)</span>
            <span class="data-purpose">Read from sensor but only for real-time display</span>
          </div>
          <div class="data-row">
            <span class="data-field">Cadence (RPM)</span>
            <span class="data-purpose">Shown during drill execution for pacing guidance</span>
          </div>
          <div class="data-row">
            <span class="data-field">3s/10s Smoothed Balance</span>
            <span class="data-purpose">Real-time trend indicators, not stored</span>
          </div>
        </div>
        <p class="note">These values are processed at ~20Hz during rides but discarded after display. Only the aggregated averages (Balance, TE, PS) are stored.</p>

        <h3>Android Permissions</h3>
        <p>The KPedal app requests the following permissions on your Karoo:</p>
        <div class="data-table">
          <div class="data-row">
            <span class="data-field">Foreground Service</span>
            <span class="data-purpose">Run in background to collect data during rides</span>
          </div>
          <div class="data-row">
            <span class="data-field">Notifications</span>
            <span class="data-purpose">Show alerts when pedaling technique needs attention</span>
          </div>
          <div class="data-row">
            <span class="data-field">Boot Completed</span>
            <span class="data-purpose">Start automatically when Karoo powers on</span>
          </div>
        </div>
        <p class="note">No internet, location, camera, microphone, or storage permissions are requested.</p>
      </section>

      <!-- Section 3: How Data Flows -->
      <section id="how-data-flows" class="privacy-section">
        <h2>3. How Data Flows</h2>

        <div class="flow-diagram">
          <div class="flow-step">
            <div class="flow-number">1</div>
            <div class="flow-content">
              <h4>Power Meter Pedals</h4>
              <p>Your pedals transmit balance, TE, and PS via ANT+ Cycling Dynamics protocol to your Karoo.</p>
            </div>
          </div>
          <div class="flow-arrow">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="12" y1="5" x2="12" y2="19"/>
              <polyline points="19,12 12,19 5,12"/>
            </svg>
          </div>
          <div class="flow-step">
            <div class="flow-number">2</div>
            <div class="flow-content">
              <h4>KPedal App on Karoo</h4>
              <p>The app processes real-time data locally at ~20Hz, calculates averages, and stores ride summaries in local database. Up to 100 rides are kept on device; older rides are auto-deleted.</p>
            </div>
          </div>
          <div class="flow-arrow">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="12" y1="5" x2="12" y2="19"/>
              <polyline points="19,12 12,19 5,12"/>
            </svg>
          </div>
          <div class="flow-step">
            <div class="flow-number">3</div>
            <div class="flow-content">
              <h4>Cloud Sync (Optional)</h4>
              <p>If signed in, ride summaries and settings are synced to KPedal cloud via HTTPS. Only aggregated data is sent — no raw sensor streams. Settings changes on web are automatically applied to your Karoo.</p>
            </div>
          </div>
          <div class="flow-arrow">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="12" y1="5" x2="12" y2="19"/>
              <polyline points="19,12 12,19 5,12"/>
            </svg>
          </div>
          <div class="flow-step">
            <div class="flow-number">4</div>
            <div class="flow-content">
              <h4>Web Dashboard</h4>
              <p>View your ride history, trends, and analytics at kpedal.com. Data is retrieved only for your authenticated account.</p>
            </div>
          </div>
        </div>

        <h3>Offline Mode</h3>
        <p>The KPedal app works fully offline. You can use all features without signing in or syncing. Data stays on your device.</p>
      </section>

      <!-- Section 4: Storage & Security -->
      <section id="storage-security" class="privacy-section">
        <h2>4. Storage & Security</h2>

        <h3>Infrastructure</h3>
        <div class="infra-table">
          <div class="infra-row">
            <span class="infra-service">Cloudflare Workers</span>
            <span class="infra-use">API processing (serverless). Request logs stored ~7 days.</span>
          </div>
          <div class="infra-row">
            <span class="infra-service">Cloudflare D1</span>
            <span class="infra-use">SQLite database for users and rides</span>
          </div>
          <div class="infra-row">
            <span class="infra-service">Cloudflare KV</span>
            <span class="infra-use">Session tokens (auto-expire after 7 days)</span>
          </div>
          <div class="infra-row">
            <span class="infra-service">Cloudflare Pages</span>
            <span class="infra-use">Static web dashboard hosting</span>
          </div>
        </div>

        <h3>Security Measures</h3>
        <div class="security-list">
          <div class="security-item">
            <strong>HTTPS Everywhere</strong>
            <p>All connections use TLS 1.3 encryption</p>
          </div>
          <div class="security-item">
            <strong>JWT Authentication</strong>
            <p>Signed tokens with 15-minute expiry for access, 7-day for refresh</p>
          </div>
          <div class="security-item">
            <strong>Rate Limiting</strong>
            <p>Auth: 10 req/min, API: 100 req/min, Sync: 30 req/min. Uses IP address for counting, auto-deleted after 60 seconds.</p>
          </div>
          <div class="security-item">
            <strong>Input Validation</strong>
            <p>All data validated and sanitized before storage</p>
          </div>
          <div class="security-item">
            <strong>CSRF Protection</strong>
            <p>OAuth state verification prevents cross-site attacks</p>
          </div>
        </div>

        <h3>Data Retention</h3>
        <h4>Cloud Storage (kpedal.com)</h4>
        <div class="retention-table">
          <div class="retention-row">
            <span class="retention-data">Ride data</span>
            <span class="retention-period">Until you delete it</span>
          </div>
          <div class="retention-row">
            <span class="retention-data">User settings</span>
            <span class="retention-period">Until account deletion</span>
          </div>
          <div class="retention-row">
            <span class="retention-data">Account data</span>
            <span class="retention-period">Until account deletion</span>
          </div>
          <div class="retention-row">
            <span class="retention-data">Access tokens</span>
            <span class="retention-period">15 minutes</span>
          </div>
          <div class="retention-row">
            <span class="retention-data">Refresh tokens</span>
            <span class="retention-period">7 days</span>
          </div>
          <div class="retention-row">
            <span class="retention-data">OAuth state</span>
            <span class="retention-period">10 minutes</span>
          </div>
        </div>

        <h4>Local Storage (Karoo Device)</h4>
        <div class="retention-table">
          <div class="retention-row">
            <span class="retention-data">Ride history</span>
            <span class="retention-period">Up to 100 rides (oldest auto-deleted)</span>
          </div>
          <div class="retention-row">
            <span class="retention-data">Drill results</span>
            <span class="retention-period">Until app uninstall</span>
          </div>
          <div class="retention-row">
            <span class="retention-data">Achievements</span>
            <span class="retention-period">Until app uninstall</span>
          </div>
          <div class="retention-row">
            <span class="retention-data">Custom drills</span>
            <span class="retention-period">Until you delete them</span>
          </div>
          <div class="retention-row">
            <span class="retention-data">Settings</span>
            <span class="retention-period">Until app uninstall</span>
          </div>
        </div>
      </section>

      <!-- Section 5: Your Rights -->
      <section id="your-rights" class="privacy-section">
        <h2>5. Your Rights</h2>

        <div class="rights-grid">
          <div class="right-card">
            <h4>Access</h4>
            <p>View all your data anytime in the web dashboard. Every metric we store is visible to you.</p>
          </div>
          <div class="right-card">
            <h4>Delete</h4>
            <p>Delete individual rides from the Rides page. For full account deletion, email us.</p>
          </div>
          <div class="right-card">
            <h4>Export</h4>
            <p>Request a complete export of your data by contacting us.</p>
          </div>
          <div class="right-card">
            <h4>Opt Out</h4>
            <p>Use KPedal offline without signing in. No data leaves your device.</p>
          </div>
        </div>
      </section>

      <!-- Section 6: Third Parties -->
      <section id="third-parties" class="privacy-section">
        <h2>6. Third-Party Services</h2>

        <div class="third-party-list">
          <div class="third-party-item">
            <h4>Google OAuth</h4>
            <p>Used for sign-in only. We request minimal scopes: <code>openid</code>, <code>email</code>, <code>profile</code>.</p>
            <a href="https://policies.google.com/privacy" target="_blank" rel="noopener noreferrer">Google Privacy Policy →</a>
          </div>
          <div class="third-party-item">
            <h4>Cloudflare</h4>
            <p>Our infrastructure provider. Handles API, database, and web hosting.</p>
            <a href="https://www.cloudflare.com/privacypolicy/" target="_blank" rel="noopener noreferrer">Cloudflare Privacy Policy →</a>
          </div>
        </div>

        <h3>What We Don't Use</h3>
        <p>KPedal does not use:</p>
        <ul>
          <li>Google Analytics or any analytics service</li>
          <li>Facebook Pixel or any advertising tracker</li>
          <li>Cookies (we use localStorage instead)</li>
          <li>Crash reporting services (no Firebase Crashlytics, Sentry, etc.)</li>
          <li>Any third-party data brokers</li>
        </ul>

        <h3>Browser Storage</h3>
        <p>The KPedal web dashboard stores the following in your browser:</p>
        <div class="data-table">
          <div class="data-row">
            <span class="data-field">Access Token</span>
            <span class="data-purpose">sessionStorage (cleared when tab closes)</span>
          </div>
          <div class="data-row">
            <span class="data-field">Refresh Token</span>
            <span class="data-purpose">localStorage (persists for 7 days)</span>
          </div>
          <div class="data-row">
            <span class="data-field">Theme Preference</span>
            <span class="data-purpose">localStorage (light/dark/system)</span>
          </div>
        </div>
        <p class="note">You can clear this data anytime by signing out or clearing browser storage.</p>
      </section>

      <!-- Section 7: Contact -->
      <section id="contact" class="privacy-section">
        <h2>7. Contact</h2>
        <p>For privacy questions, data requests, or account deletion:</p>
        <div class="contact-info">
          <a href="mailto:privacy@kpedal.com" class="contact-link">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
              <polyline points="22,6 12,13 2,6"/>
            </svg>
            privacy@kpedal.com
          </a>
          <a href="https://github.com/yrkan/kpedal/issues" target="_blank" rel="noopener noreferrer" class="contact-link">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22"/>
            </svg>
            GitHub Issues
          </a>
        </div>
      </section>

      <!-- Policy Changes -->
      <section class="privacy-section">
        <h2>Policy Changes</h2>
        <p>When we make significant changes to this policy, we update the "Last updated" date above. We encourage you to review this page periodically.</p>
      </section>
    </div>

    <footer class="privacy-footer">
      <a href="/" class="footer-logo">
        <span class="logo-dot"></span>
        <span>KPedal</span>
      </a>
      <p>Pedaling efficiency analytics for Karoo</p>
    </footer>
  </div>
</div>

<style>
  .privacy-page {
    min-height: 100vh;
    padding: 24px;
    background: var(--bg-base);
  }

  .privacy-container {
    max-width: 720px;
    margin: 0 auto;
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

  /* Header */
  .privacy-header {
    padding: 40px 0 32px;
    border-bottom: 1px solid var(--border-subtle);
    margin-bottom: 32px;
  }

  .back-link {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    color: var(--text-tertiary);
    font-size: 14px;
    margin-bottom: 24px;
    transition: color 0.2s ease;
  }

  .back-link:hover {
    color: var(--text-primary);
  }

  .privacy-header h1 {
    font-size: 36px;
    font-weight: 700;
    color: var(--text-primary);
    margin-bottom: 8px;
    letter-spacing: -0.5px;
  }

  .last-updated {
    font-size: 14px;
    color: var(--text-tertiary);
  }

  /* Quick Summary */
  .quick-summary {
    margin-bottom: 40px;
  }

  .summary-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }

  .summary-item {
    display: flex;
    align-items: flex-start;
    gap: 14px;
    padding: 16px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
  }

  .summary-icon {
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--bg-elevated);
    border-radius: 10px;
    color: var(--text-secondary);
    flex-shrink: 0;
  }

  .summary-text {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .summary-text strong {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
  }

  .summary-text span {
    font-size: 13px;
    color: var(--text-tertiary);
  }

  /* Table of Contents */
  .toc {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 20px 24px;
    margin-bottom: 40px;
  }

  .toc h2 {
    font-size: 12px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    color: var(--text-tertiary);
    margin-bottom: 12px;
  }

  .toc ol {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 8px 24px;
    list-style: none;
    padding: 0;
    margin: 0;
    counter-reset: toc;
  }

  .toc li {
    counter-increment: toc;
  }

  .toc a {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    color: var(--text-secondary);
    transition: color 0.15s ease;
  }

  .toc a::before {
    content: counter(toc) ".";
    color: var(--text-muted);
    font-size: 13px;
  }

  .toc a:hover {
    color: var(--text-primary);
  }

  /* Sections */
  .privacy-section {
    margin-bottom: 48px;
  }

  .privacy-section h2 {
    font-size: 22px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 20px;
    padding-bottom: 12px;
    border-bottom: 1px solid var(--border-subtle);
  }

  .privacy-section h3 {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 24px 0 12px;
  }

  .privacy-section h4 {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 8px;
  }

  .privacy-section p {
    color: var(--text-secondary);
    line-height: 1.7;
    margin-bottom: 12px;
  }

  .privacy-section ul {
    color: var(--text-secondary);
    line-height: 1.7;
    margin: 12px 0;
    padding-left: 20px;
  }

  .privacy-section li {
    margin-bottom: 6px;
  }

  .privacy-section a {
    color: var(--color-accent);
  }

  .privacy-section a:hover {
    text-decoration: underline;
  }

  .note {
    font-size: 13px;
    color: var(--text-tertiary);
    font-style: italic;
  }

  /* Data Tables */
  .data-table, .infra-table, .retention-table {
    display: flex;
    flex-direction: column;
    gap: 1px;
    background: var(--border-subtle);
    border-radius: 8px;
    overflow: hidden;
    margin: 12px 0;
  }

  .data-row, .infra-row, .retention-row {
    display: grid;
    grid-template-columns: 180px 1fr;
    background: var(--bg-surface);
    padding: 12px 16px;
  }

  .data-field, .infra-service, .retention-data {
    font-weight: 500;
    color: var(--text-primary);
    font-size: 14px;
  }

  .data-purpose, .infra-use, .retention-period {
    color: var(--text-tertiary);
    font-size: 14px;
  }

  /* Don't Collect Grid */
  .dont-collect-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    margin-top: 16px;
  }

  .dont-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px 16px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 8px;
    font-size: 14px;
    color: var(--text-secondary);
  }

  .dont-item svg {
    color: var(--text-muted);
    flex-shrink: 0;
  }

  /* Flow Diagram */
  .flow-diagram {
    display: flex;
    flex-direction: column;
    gap: 0;
    margin: 20px 0;
  }

  .flow-step {
    display: flex;
    gap: 16px;
    padding: 16px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
  }

  .flow-number {
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--bg-elevated);
    border-radius: 50%;
    font-size: 14px;
    font-weight: 600;
    color: var(--text-secondary);
    flex-shrink: 0;
  }

  .flow-content h4 {
    margin-bottom: 4px;
  }

  .flow-content p {
    margin: 0;
    font-size: 14px;
  }

  .flow-arrow {
    display: flex;
    justify-content: center;
    padding: 8px 0;
    color: var(--text-muted);
  }

  /* Security List */
  .security-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
    margin: 12px 0;
  }

  .security-item {
    padding: 14px 16px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 8px;
  }

  .security-item strong {
    display: block;
    font-size: 14px;
    color: var(--text-primary);
    margin-bottom: 4px;
  }

  .security-item p {
    margin: 0;
    font-size: 13px;
    color: var(--text-tertiary);
  }

  /* Rights Grid */
  .rights-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }

  .right-card {
    padding: 20px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
  }

  .right-card h4 {
    margin-bottom: 8px;
  }

  .right-card p {
    margin: 0;
    font-size: 14px;
  }

  /* Third Party */
  .third-party-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
    margin: 12px 0;
  }

  .third-party-item {
    padding: 16px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 10px;
  }

  .third-party-item h4 {
    margin-bottom: 6px;
  }

  .third-party-item p {
    margin-bottom: 8px;
    font-size: 14px;
  }

  .third-party-item a {
    font-size: 13px;
  }

  /* Contact */
  .contact-info {
    display: flex;
    gap: 16px;
    margin-top: 16px;
  }

  .contact-link {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 12px 20px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 8px;
    font-size: 14px;
    color: var(--text-primary);
    transition: all 0.15s ease;
  }

  .contact-link:hover {
    background: var(--bg-hover);
    border-color: var(--border-default);
    text-decoration: none;
  }

  .contact-link svg {
    color: var(--text-tertiary);
  }

  /* Code */
  code {
    background: var(--bg-elevated);
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 13px;
    font-family: 'SF Mono', Monaco, monospace;
  }

  /* Footer */
  .privacy-footer {
    text-align: center;
    padding-top: 40px;
    border-top: 1px solid var(--border-subtle);
    margin-top: 40px;
  }

  .footer-logo {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
  }

  .logo-dot {
    width: 8px;
    height: 8px;
    background: var(--color-optimal);
    border-radius: 50%;
  }

  .privacy-footer p {
    font-size: 13px;
    color: var(--text-muted);
  }

  /* Responsive */
  @media (max-width: 640px) {
    .summary-grid {
      grid-template-columns: 1fr;
    }

    .toc ol {
      grid-template-columns: 1fr;
    }

    .dont-collect-grid {
      grid-template-columns: 1fr;
    }

    .rights-grid {
      grid-template-columns: 1fr;
    }

    .data-row, .infra-row, .retention-row {
      grid-template-columns: 1fr;
      gap: 4px;
    }

    .contact-info {
      flex-direction: column;
    }

    .privacy-header h1 {
      font-size: 28px;
    }
  }
</style>
