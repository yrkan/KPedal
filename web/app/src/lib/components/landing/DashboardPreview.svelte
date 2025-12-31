<script lang="ts">
  import { t } from '$lib/i18n';
</script>

<section class="section" aria-labelledby="analytics-title">
  <h2 id="analytics-title" class="section-title">{$t('landing.analytics.title')}</h2>
  <p class="section-subtitle">{$t('landing.analytics.subtitle')}</p>

  <div class="dashboard-preview">
    <div class="dash-browser">
      <div class="browser-bar">
        <div class="browser-dots"><span></span><span></span><span></span></div>
        <div class="browser-url">kpedal.com</div>
      </div>
      <div class="browser-content">
        <div class="dash-header-preview">
          <div class="dash-greeting-preview">{$t('landing.analytics.preview.greeting')}</div>
          <div class="period-selector-preview">
            <span class="period-btn-preview active">7d</span>
            <span class="period-btn-preview">14d</span>
            <span class="period-btn-preview">30d</span>
          </div>
        </div>

        <!-- Hero Stats Row -->
        <div class="hero-stats-preview">
          <div class="hero-stat-preview asymmetry-preview">
            <span class="hero-value optimal">0.8%</span>
            <span class="hero-label">{$t('metrics.asymmetry')}</span>
            <span class="hero-detail">{$t('dashboard.balanced')}</span>
          </div>
          <div class="hero-stat-preview balance-preview">
            <div class="balance-bar-preview">
              <span class="balance-side-preview">49</span>
              <div class="balance-visual-preview">
                <div class="balance-fill-left" style="width: 49%"></div>
                <div class="balance-fill-right" style="width: 51%"></div>
              </div>
              <span class="balance-side-preview">51</span>
            </div>
          </div>
          <div class="hero-stat-preview zones-preview">
            <div class="zone-bars-preview">
              <div class="zone-bar optimal" style="width: 72%"></div>
              <div class="zone-bar attention" style="width: 20%"></div>
              <div class="zone-bar problem" style="width: 8%"></div>
            </div>
            <span class="hero-label">{$t('zones.timeInZone')}</span>
          </div>
          <div class="hero-stat-preview summary-preview">
            <div class="summary-row">
              <span class="summary-num">12</span>
              <span class="summary-unit">{$t('dashboard.weeklyChart.rides')}</span>
            </div>
            <div class="summary-row">
              <span class="summary-num">8.4</span>
              <span class="summary-unit">{$t('dashboard.weeklyChart.hours')}</span>
            </div>
          </div>
        </div>

        <!-- Main Grid -->
        <div class="main-grid-preview">
          <div class="grid-card-preview rides-card">
            <div class="card-header-preview">
              <span>{$t('landing.analytics.preview.recentRides')}</span>
            </div>
            <div class="recent-rides-preview">
              <div class="ride-row-preview">
                <div class="ride-info">
                  <span class="ride-title">{$t('landing.analytics.preview.morningRide')}</span>
                  <span class="ride-meta">{$t('common.today')} · 1h 23m</span>
                </div>
                <div class="ride-score optimal">87</div>
              </div>
              <div class="ride-row-preview">
                <div class="ride-info">
                  <span class="ride-title">{$t('landing.analytics.preview.eveningSpin')}</span>
                  <span class="ride-meta">{$t('common.yesterday')} · 45m</span>
                </div>
                <div class="ride-score optimal">92</div>
              </div>
              <div class="ride-row-preview">
                <div class="ride-info">
                  <span class="ride-title">{$t('landing.analytics.preview.longRide')}</span>
                  <span class="ride-meta">2 {$t('landing.analytics.preview.daysAgo')} · 2h 15m</span>
                </div>
                <div class="ride-score attention">71</div>
              </div>
            </div>
          </div>
          <div class="grid-card-preview technique-card">
            <div class="card-header-preview">
              <span>{$t('landing.analytics.preview.technique')}</span>
              <span class="card-subtitle-preview">7d {$t('landing.analytics.preview.avgSuffix')}</span>
            </div>
            <div class="technique-preview">
              <div class="technique-metric-lr">
                <span class="metric-name">{$t('metrics.te')}</span>
                <div class="lr-comparison">
                  <div class="leg left">
                    <span class="leg-label">L</span>
                    <div class="leg-bar-wrap">
                      <div class="leg-bar te" style="width: 72%"></div>
                    </div>
                    <span class="leg-value">72%</span>
                  </div>
                  <div class="leg right">
                    <span class="leg-label">R</span>
                    <div class="leg-bar-wrap">
                      <div class="leg-bar te" style="width: 76%"></div>
                    </div>
                    <span class="leg-value">76%</span>
                  </div>
                </div>
              </div>
              <div class="technique-metric-lr">
                <span class="metric-name">{$t('metrics.ps')}</span>
                <div class="lr-comparison">
                  <div class="leg left">
                    <span class="leg-label">L</span>
                    <div class="leg-bar-wrap">
                      <div class="leg-bar ps" style="width: 52%"></div>
                    </div>
                    <span class="leg-value">21%</span>
                  </div>
                  <div class="leg right">
                    <span class="leg-label">R</span>
                    <div class="leg-bar-wrap">
                      <div class="leg-bar ps" style="width: 58%"></div>
                    </div>
                    <span class="leg-value">23%</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="dash-features">
      <div class="dash-feature">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
        </svg>
        <span>{$t('landing.analytics.features.periods')}</span>
      </div>
      <div class="dash-feature">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>
        </svg>
        <span>{$t('landing.analytics.features.history')}</span>
      </div>
      <div class="dash-feature">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/>
          <path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>
        </svg>
        <span>{$t('landing.analytics.features.multiDevice')}</span>
      </div>
      <div class="dash-feature">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22"/>
        </svg>
        <span>{$t('landing.analytics.features.openSource')}</span>
      </div>
    </div>
  </div>
</section>

<style>
  .dashboard-preview {
    display: flex;
    flex-direction: column;
    gap: 32px;
  }
  .dash-browser {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 20px;
    overflow: hidden;
  }
  .browser-bar {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 14px 20px;
    background: var(--bg-elevated);
    border-bottom: 1px solid var(--border-subtle);
  }
  .browser-dots {
    display: flex;
    gap: 8px;
  }
  .browser-dots span {
    width: 12px;
    height: 12px;
    border-radius: 50%;
    opacity: 0.7;
  }
  .browser-dots span:first-child { background: #ff5f56; }
  .browser-dots span:nth-child(2) { background: #ffbd2e; }
  .browser-dots span:last-child { background: #27c93f; }
  .browser-url {
    flex: 1;
    text-align: center;
    font-size: 12px;
    color: var(--text-muted);
    letter-spacing: -0.1px;
  }
  .browser-content {
    padding: 20px;
  }
  .dash-header-preview {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
  }
  .dash-greeting-preview {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
  }
  .period-selector-preview {
    display: flex;
    gap: 4px;
  }
  .period-btn-preview {
    font-size: 10px;
    padding: 4px 8px;
    background: var(--bg-base);
    border-radius: 6px;
    color: var(--text-muted);
  }
  .period-btn-preview.active {
    background: var(--text-primary);
    color: var(--bg-base);
  }
  .hero-stats-preview {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 10px;
    margin-bottom: 14px;
  }
  .hero-stat-preview {
    background: var(--bg-base);
    border-radius: 10px;
    padding: 12px;
  }
  .asymmetry-preview {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  .hero-value {
    font-size: 18px;
    font-weight: 600;
    color: var(--text-primary);
  }
  .hero-value.optimal { color: var(--color-optimal); }
  .hero-label {
    font-size: 9px;
    color: var(--text-muted);
    text-transform: uppercase;
    letter-spacing: 0.3px;
  }
  .hero-detail {
    font-size: 10px;
    color: var(--text-tertiary);
  }
  .balance-bar-preview {
    display: flex;
    align-items: center;
    gap: 6px;
    height: 100%;
  }
  .balance-side-preview {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
  }
  .balance-visual-preview {
    flex: 1;
    height: 8px;
    display: flex;
    border-radius: 4px;
    overflow: hidden;
  }
  .balance-fill-left {
    background: var(--color-optimal);
    height: 100%;
  }
  .balance-fill-right {
    background: var(--color-optimal);
    opacity: 0.5;
    height: 100%;
  }
  .zones-preview {
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 6px;
  }
  .zone-bars-preview {
    display: flex;
    height: 8px;
    border-radius: 4px;
    overflow: hidden;
  }
  .zone-bar.optimal { background: var(--color-optimal); }
  .zone-bar.attention { background: var(--color-attention); }
  .zone-bar.problem { background: var(--color-problem); }
  .summary-preview {
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 4px;
  }
  .summary-row {
    display: flex;
    align-items: baseline;
    gap: 4px;
  }
  .summary-row .summary-num {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
  }
  .summary-row .summary-unit {
    font-size: 10px;
    color: var(--text-muted);
  }
  .main-grid-preview {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
    align-items: stretch;
  }
  .main-grid-preview .grid-card-preview {
    display: flex;
    flex-direction: column;
  }
  .grid-card-preview {
    background: var(--bg-base);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 14px;
  }
  .card-header-preview {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
    font-size: 12px;
    font-weight: 600;
    color: var(--text-primary);
  }
  .card-subtitle-preview {
    font-weight: 500;
    color: var(--text-tertiary);
    font-size: 10px;
  }
  /* Recent Rides Card */
  .recent-rides-preview {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  .ride-row-preview {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 10px;
    background: var(--bg-elevated);
    border-radius: 8px;
  }
  .ride-info {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  .ride-title {
    font-size: 11px;
    font-weight: 600;
    color: var(--text-primary);
  }
  .ride-meta {
    font-size: 9px;
    color: var(--text-muted);
  }
  .ride-score {
    font-size: 14px;
    font-weight: 700;
    padding: 4px 8px;
    border-radius: 6px;
  }
  .ride-score.optimal {
    color: var(--color-optimal);
    background: var(--color-optimal-soft);
  }
  .ride-score.attention {
    color: var(--color-attention);
    background: var(--color-attention-soft);
  }
  /* Technique Card - L/R Comparison */
  .technique-card .technique-preview {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 14px;
  }
  .technique-metric-lr {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }
  .metric-name {
    font-size: 10px;
    font-weight: 600;
    color: var(--text-secondary);
    text-transform: uppercase;
    letter-spacing: 0.3px;
  }
  .lr-comparison {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }
  .leg {
    display: flex;
    align-items: center;
    gap: 6px;
  }
  .leg-label {
    font-size: 10px;
    font-weight: 600;
    color: var(--text-muted);
    width: 12px;
  }
  .leg-bar-wrap {
    flex: 1;
    height: 6px;
    background: var(--bg-elevated);
    border-radius: 3px;
    overflow: hidden;
  }
  .leg-bar {
    height: 100%;
    border-radius: 3px;
    transition: width 0.3s ease;
  }
  .leg-bar.te {
    background: linear-gradient(90deg, var(--color-optimal) 0%, #4ade80 100%);
  }
  .leg-bar.ps {
    background: linear-gradient(90deg, #60a5fa 0%, #3b82f6 100%);
  }
  .leg-value {
    font-size: 11px;
    font-weight: 600;
    color: var(--text-primary);
    width: 28px;
    text-align: right;
  }
  .dash-features {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
  .dash-feature {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 16px 20px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 14px;
    font-size: 13px;
    color: var(--text-tertiary);
    letter-spacing: -0.1px;
  }
  .dash-feature svg {
    color: var(--text-muted);
    flex-shrink: 0;
    width: 18px;
    height: 18px;
  }

  /* Responsive - 768px */
  @media (max-width: 768px) {
    .hero-stats-preview { grid-template-columns: repeat(2, 1fr); }
    .main-grid-preview { grid-template-columns: 1fr; }
    .dash-features { grid-template-columns: 1fr; }
  }

  /* Responsive - 480px */
  @media (max-width: 480px) {
    .dash-browser { border-radius: 16px; }
    .browser-content { padding: 16px; }
    .hero-stats-preview { gap: 8px; }
    .hero-stat-preview { padding: 10px; }
    .dash-feature { padding: 14px 16px; font-size: 12px; }
  }

  /* Responsive - 360px */
  @media (max-width: 360px) {
    .browser-bar { padding: 10px 14px; }
    .browser-content { padding: 12px; }
    .hero-stats-preview { gap: 6px; }
    .hero-stat-preview { padding: 8px; }
    .hero-value { font-size: 16px; }
    .main-grid-preview { gap: 10px; }
    .grid-card-preview { padding: 12px; }
    .dash-features { gap: 8px; }
    .dash-feature { padding: 12px 14px; font-size: 11px; gap: 10px; }
  }
</style>
