<script lang="ts">
  import { t } from '$lib/i18n';

  export let items: string[];
</script>

<div class="dont-collect-grid">
  {#each items as item}
    <div class="dont-item">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <line x1="18" y1="6" x2="6" y2="18"/>
        <line x1="6" y1="6" x2="18" y2="18"/>
      </svg>
      <span>{$t(item)}</span>
    </div>
  {/each}
</div>

<style>
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

  @media (max-width: 640px) {
    .dont-collect-grid {
      grid-template-columns: 1fr;
    }

    .dont-item {
      padding: 10px 14px;
      font-size: 13px;
    }
  }
</style>
