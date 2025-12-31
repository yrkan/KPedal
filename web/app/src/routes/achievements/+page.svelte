<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { isAuthenticated, authFetch, isDemo } from '$lib/auth';
  import { getDemoAchievements } from '$lib/demoData';
  import InfoTip from '$lib/components/InfoTip.svelte';
  import { t, locale } from '$lib/i18n';

  function getLocaleString(currentLocale: string | null | undefined): string {
    const localeMap: Record<string, string> = { en: 'en-US', es: 'es-ES' };
    return currentLocale ? localeMap[currentLocale] || currentLocale : 'en-US';
  }

  // Milestone IDs with category keys
  const MILESTONE_IDS = [
    'first_ride', 'rides_10', 'rides_50', 'rides_100', 'rides_500',
    'perfect_balance', 'balance_master',
    'score_80', 'score_90', 'score_95',
    'streak_3', 'streak_7', 'streak_30',
    'first_drill', 'drill_perfect', 'drills_10',
    'zone_optimal_80', 'zone_optimal_90',
    'te_optimal',
    'duration_1h', 'duration_2h', 'duration_4h'
  ] as const;

  interface UnlockedMilestone {
    id: number;
    achievement_id: string;
    unlocked_at: number;
    created_at?: string;
  }

  interface MilestoneStats {
    unlocked_count: number;
    first_unlocked_at: number | null;
    last_unlocked_at: number | null;
  }

  let milestones: UnlockedMilestone[] = [];
  let stats: { summary: MilestoneStats; recent: { achievement_id: string; unlocked_at: number }[] } | null = null;
  let loading = true;
  let error = false;
  let viewMode: 'table' | 'cards' = 'table';
  let viewModeInitialized = false;

  // Save viewMode to localStorage when it changes (only after initialization)
  $: if (viewModeInitialized && typeof localStorage !== 'undefined') {
    localStorage.setItem('achievements-view-mode', viewMode);
  }

  $: achievedIds = new Set(milestones.map(a => a.achievement_id));
  $: totalMilestones = MILESTONE_IDS.length;
  $: achievedCount = milestones.length;
  $: progressPercent = Math.round((achievedCount / totalMilestones) * 100);

  // Sort: achieved first, then by unlock date (newest first), then alphabetically
  $: sortedMilestoneIds = [...MILESTONE_IDS].sort((a, b) => {
    const aAchieved = achievedIds.has(a);
    const bAchieved = achievedIds.has(b);
    if (aAchieved && !bAchieved) return -1;
    if (!aAchieved && bAchieved) return 1;
    // Among achieved, sort by date (newest first)
    if (aAchieved && bAchieved) {
      const aDate = milestones.find(m => m.achievement_id === a)?.unlocked_at || 0;
      const bDate = milestones.find(m => m.achievement_id === b)?.unlocked_at || 0;
      return bDate - aDate;
    }
    return 0;
  });

  onMount(async () => {
    // Restore viewMode from localStorage
    const savedViewMode = localStorage.getItem('achievements-view-mode');
    if (savedViewMode === 'table' || savedViewMode === 'cards') {
      viewMode = savedViewMode;
    }
    viewModeInitialized = true;

    if (!$isAuthenticated) {
      goto('/login');
      return;
    }
    await loadDashboard();
  });

  async function loadDashboard() {
    loading = true;
    error = false;

    try {
      // Demo mode: use static data (0ms, no API call)
      if ($isDemo) {
        const data = getDemoAchievements();
        milestones = data.achievements;
        stats = data.stats;
        loading = false;
        return;
      }

      // Regular users: API call
      const res = await authFetch('/achievements/dashboard');
      if (res.ok) {
        const data = await res.json();
        if (data.success) {
          milestones = data.data.achievements || [];
          stats = data.data.stats;
        }
      } else {
        error = true;
      }
    } catch (err) {
      error = true;
    } finally {
      loading = false;
    }
  }

  function getAchievedDate(id: string, currentLocale?: string | null): string | null {
    const milestone = milestones.find(a => a.achievement_id === id);
    if (!milestone) return null;
    return formatDate(milestone.unlocked_at, currentLocale);
  }

  function formatDate(timestamp: number, currentLocale?: string | null): string {
    const date = new Date(timestamp);
    return date.toLocaleDateString(getLocaleString(currentLocale), { month: 'short', day: 'numeric' });
  }

  function formatFullDate(timestamp: number, currentLocale?: string | null): string {
    const date = new Date(timestamp);
    return date.toLocaleDateString(getLocaleString(currentLocale), { month: 'short', day: 'numeric', year: 'numeric' });
  }
</script>

<svelte:head>
  <title>{$t('achievements.title')} - KPedal</title>
</svelte:head>

<div class="page milestones-page">
  <div class="container container-lg">
    <header class="page-header animate-in">
      <a href="/" class="back-link" aria-label={$t('common.backToDashboard')}>
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="15,18 9,12 15,6"/>
        </svg>
      </a>
      <h1>{$t('achievements.title')}</h1>
      {#if achievedCount > 0}
        <div class="view-toggle">
          <button class="view-btn" class:active={viewMode === 'table'} on:click={() => viewMode = 'table'} title={$t('common.tableView')}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/>
            </svg>
          </button>
          <button class="view-btn" class:active={viewMode === 'cards'} on:click={() => viewMode = 'cards'} title={$t('common.cardView')}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/>
            </svg>
          </button>
        </div>
      {/if}
    </header>

    {#if loading}
      <div class="loading-state animate-in">
        <div class="spinner"></div>
      </div>
    {:else if error}
      <div class="error-state animate-in">
        <p>{$t('errors.failedToLoadMilestones')}</p>
        <button class="retry-btn" on:click={loadDashboard}>{$t('common.retry')}</button>
      </div>
    {:else if achievedCount === 0 && !loading}
      <div class="empty-state-enhanced animate-in">
        <div class="empty-icon-wrap">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <polygon points="12,2 15.09,8.26 22,9.27 17,14.14 18.18,21.02 12,17.77 5.82,21.02 7,14.14 2,9.27 8.91,8.26"/>
          </svg>
        </div>
        <h3>{$t('achievements.noAchievements')}</h3>
        <p>{$t('achievements.noAchievementsHint')}</p>

        <div class="achievement-categories">
          <span class="cat-label">{$t('achievements.availableCategories')}</span>
          <div class="cat-grid">
            <div class="cat-item">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 12h-4l-3 9L9 3l-3 9H2"/></svg>
              <span>{$t('achievements.categories.milestones')}</span>
            </div>
            <div class="cat-item">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M8 12h8"/></svg>
              <span>{$t('achievements.categories.balance')}</span>
            </div>
            <div class="cat-item">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 20V10M18 20V4M6 20v-4"/></svg>
              <span>{$t('achievements.categories.performance')}</span>
            </div>
            <div class="cat-item">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/></svg>
              <span>{$t('achievements.categories.streaks')}</span>
            </div>
            <div class="cat-item">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
              <span>{$t('achievements.categories.duration')}</span>
            </div>
            <div class="cat-item">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="3"/></svg>
              <span>{$t('achievements.categories.drills')}</span>
            </div>
          </div>
        </div>

        <div class="empty-actions">
          <a href="/" class="empty-action-btn">← {$t('nav.dashboard')}</a>
        </div>
      </div>
    {:else}
      <!-- Stats Strip -->
      <div class="stats-strip animate-in">
        <div class="stat-chip highlight">
          <span class="stat-chip-val">{achievedCount}</span>
          <span class="stat-chip-label">{$t('achievements.stats.achieved')} <InfoTip text={$t('achievements.stats.achievedTip')} position="bottom" size="sm" /></span>
        </div>
        <div class="stat-chip">
          <span class="stat-chip-val">{totalMilestones - achievedCount}</span>
          <span class="stat-chip-label">{$t('achievements.stats.remaining')} <InfoTip text={$t('achievements.stats.remainingTip')} position="bottom" size="sm" /></span>
        </div>
        <div class="stat-chip">
          <span class="stat-chip-val">{totalMilestones}</span>
          <span class="stat-chip-label">{$t('achievements.stats.total')} <InfoTip text={$t('achievements.stats.totalTip')} position="bottom" size="sm" /></span>
        </div>
        <div class="stat-chip">
          <span class="stat-chip-val optimal">{progressPercent}%</span>
          <span class="stat-chip-label">{$t('achievements.stats.progress')} <InfoTip text={$t('achievements.stats.progressTip')} position="bottom" size="sm" /></span>
        </div>
        {#if stats?.recent && stats.recent.length > 0}
          <div class="stat-chip">
            <span class="stat-chip-val">{formatDate(stats.recent[0].unlocked_at, $locale)}</span>
            <span class="stat-chip-label">{$t('achievements.stats.last')} <InfoTip text={$t('achievements.stats.lastTip')} position="bottom" size="sm" /></span>
          </div>
        {/if}
      </div>

      {#if viewMode === 'table'}
        <!-- Table View -->
        <div class="data-table-wrapper milestones-table-wrapper animate-in">
          <table class="data-table milestones-table">
            <thead>
              <tr>
                <th class="col-status"></th>
                <th class="col-milestone">{$t('achievements.table.milestone')} <InfoTip text={$t('achievements.table.milestoneTip')} position="bottom" size="sm" /></th>
                <th class="col-category">{$t('achievements.table.category')} <InfoTip text={$t('achievements.table.categoryTip')} position="bottom" size="sm" /></th>
                <th class="col-target">{$t('achievements.table.target')} <InfoTip text={$t('achievements.table.targetTip')} position="bottom" size="sm" /></th>
                <th class="col-date">{$t('achievements.table.date')}</th>
              </tr>
            </thead>
            <tbody>
              {#each sortedMilestoneIds as id}
                {@const isAchieved = achievedIds.has(id)}
                <tr class:achieved={isAchieved}>
                  <td class="col-status">
                    {#if isAchieved}
                      <span class="status-icon achieved">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                          <polyline points="20,6 9,17 4,12"/>
                        </svg>
                      </span>
                    {:else}
                      <span class="status-icon pending">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <circle cx="12" cy="12" r="10"/>
                        </svg>
                      </span>
                    {/if}
                  </td>
                  <td class="col-milestone">
                    <span class="milestone-name">{$t(`achievements.milestones.${id}.name`)}</span>
                    <span class="milestone-desc">{$t(`achievements.milestones.${id}.description`)}</span>
                  </td>
                  <td class="col-category">{$t(`achievements.milestones.${id}.category`)}</td>
                  <td class="col-target">
                    <span class="target-badge">{$t(`achievements.milestones.${id}.target`)}</span>
                  </td>
                  <td class="col-date">
                    {#if isAchieved}
                      <span class="date-value">{getAchievedDate(id, $locale)}</span>
                    {:else}
                      <span class="date-pending">—</span>
                    {/if}
                  </td>
                </tr>
              {/each}
            </tbody>
          </table>
        </div>
      {:else}
        <!-- Card View -->
        <div class="cards-grid milestones-grid animate-in">
          {#each sortedMilestoneIds as id}
            {@const isAchieved = achievedIds.has(id)}
            <article class="milestone-card" class:achieved={isAchieved}>
              <div class="card-header">
                <span class="card-category">{$t(`achievements.milestones.${id}.category`)}</span>
                {#if isAchieved}
                  <span class="card-status achieved">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                      <polyline points="20,6 9,17 4,12"/>
                    </svg>
                  </span>
                {/if}
              </div>
              <div class="card-content">
                <h3 class="card-name">{$t(`achievements.milestones.${id}.name`)}</h3>
                <p class="card-desc">{$t(`achievements.milestones.${id}.description`)}</p>
              </div>
              <div class="card-footer">
                <span class="card-target">{$t(`achievements.milestones.${id}.target`)}</span>
                {#if isAchieved}
                  <span class="card-date">{getAchievedDate(id, $locale)}</span>
                {:else}
                  <span class="card-date pending">{$t('achievements.notAchieved')}</span>
                {/if}
              </div>
            </article>
          {/each}
        </div>
      {/if}
    {/if}
  </div>
</div>

<style>
  /* Table-specific styles */
  .milestones-table tbody tr:not(.achieved) { opacity: 0.5; }
  .milestones-table tbody tr.achieved { opacity: 1; }

  .col-status { width: 50px; text-align: center; }
  .col-milestone { white-space: nowrap; }
  .col-category { width: 100px; color: var(--text-secondary); font-size: 12px; white-space: nowrap; }
  .col-target { width: 90px; white-space: nowrap; }
  .col-date { width: 100px; }

  .status-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 26px;
    height: 26px;
    border-radius: 50%;
  }

  .status-icon.achieved {
    background: var(--color-optimal-soft);
    color: var(--color-optimal-text);
  }

  .status-icon.pending {
    background: var(--bg-elevated);
    color: var(--text-muted);
  }

  .milestone-name {
    display: block;
    font-weight: 500;
    color: var(--text-primary);
  }

  .milestone-desc {
    display: block;
    font-size: 12px;
    color: var(--text-secondary);
    margin-top: 2px;
  }

  .target-badge {
    display: inline-block;
    padding: 4px 10px;
    background: var(--bg-elevated);
    border-radius: 6px;
    font-size: 12px;
    font-weight: 600;
    color: var(--text-primary);
    white-space: nowrap;
  }

  .date-value {
    font-size: 13px;
    font-weight: 500;
    color: var(--color-optimal-text);
  }

  .date-pending {
    color: var(--text-muted);
  }

  /* Card View */
  .milestones-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 14px;
  }

  .milestone-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 16px;
    opacity: 0.5;
    transition: all 0.15s ease;
  }

  .milestone-card.achieved { opacity: 1; }
  .milestone-card:hover { border-color: var(--border-default); }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;
  }

  .card-category {
    font-size: 11px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.3px;
    color: var(--text-muted);
  }

  .card-status.achieved {
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--color-optimal-soft);
    color: var(--color-optimal-text);
    border-radius: 6px;
  }

  .card-content { margin-bottom: 12px; }

  .card-name {
    font-size: 15px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 4px;
  }

  .card-desc {
    font-size: 13px;
    color: var(--text-secondary);
    line-height: 1.4;
  }

  .card-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: 12px;
    border-top: 1px solid var(--border-subtle);
  }

  .card-target {
    font-size: 12px;
    font-weight: 500;
    padding: 4px 8px;
    background: var(--bg-elevated);
    border-radius: 6px;
    color: var(--text-secondary);
  }

  .card-date {
    font-size: 12px;
    color: var(--color-optimal-text);
  }

  .card-date.pending {
    color: var(--text-muted);
  }

  /* Enhanced Empty State */
  .empty-state-enhanced {
    text-align: center;
    padding: 60px 24px;
    max-width: 480px;
    margin: 0 auto;
  }
  .empty-icon-wrap {
    width: 80px;
    height: 80px;
    margin: 0 auto 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 20px;
    color: var(--text-muted);
  }
  .empty-state-enhanced h3 {
    font-size: 20px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 8px;
  }
  .empty-state-enhanced > p {
    font-size: 14px;
    color: var(--text-secondary);
    line-height: 1.5;
    margin-bottom: 24px;
  }
  .achievement-categories {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 16px;
    margin-bottom: 20px;
  }
  .cat-label {
    display: block;
    font-size: 11px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.3px;
    color: var(--text-muted);
    margin-bottom: 12px;
  }
  .cat-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }
  .cat-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 12px;
    background: var(--bg-base);
    border-radius: 8px;
    font-size: 13px;
    color: var(--text-secondary);
  }
  .cat-item svg {
    color: var(--text-muted);
    flex-shrink: 0;
  }
  .empty-actions {
    display: flex;
    gap: 12px;
    justify-content: center;
  }
  .empty-action-btn {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 10px 18px;
    border-radius: 8px;
    font-size: 13px;
    font-weight: 500;
    text-decoration: none;
    transition: all 0.15s;
    background: var(--bg-surface);
    color: var(--text-secondary);
    border: 1px solid var(--border-subtle);
  }
  .empty-action-btn:hover {
    border-color: var(--border-default);
    color: var(--text-primary);
  }

  /* Mobile */
  @media (max-width: 768px) {
    .col-category { display: none; }
    .col-target { width: 80px; }
    .target-badge { padding: 3px 8px; font-size: 11px; }
  }

  @media (max-width: 480px) {
    .col-status { width: 36px; }
    .status-icon { width: 22px; height: 22px; }
    .status-icon svg { width: 12px; height: 12px; }
    .milestone-name { font-size: 12px; }
    .milestone-desc { display: none; }
    .col-target { width: 70px; }
    .target-badge { padding: 2px 6px; font-size: 10px; }
    .col-date { width: 70px; }
    .date-value { font-size: 11px; }
    /* Empty state */
    .cat-grid { grid-template-columns: 1fr; }
  }
</style>
