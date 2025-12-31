<script lang="ts">
  import { t } from '$lib/i18n';
  import type { RightCard } from './types';

  export let items: RightCard[];
</script>

<div class="rights-grid">
  {#each items as item}
    <div class="right-card">
      <h4>{$t(item.titleKey)}</h4>
      <p>{$t(item.descKey)}</p>
    </div>
  {/each}
</div>

<style>
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
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 8px;
  }

  .right-card p {
    margin: 0;
    font-size: 14px;
    color: var(--text-secondary);
    line-height: 1.7;
  }

  @media (max-width: 640px) {
    .rights-grid {
      grid-template-columns: 1fr;
    }

    .right-card {
      padding: 16px;
    }
  }
</style>
