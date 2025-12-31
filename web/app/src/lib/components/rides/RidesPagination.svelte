<script lang="ts">
  import { t } from '$lib/i18n';

  export let offset: number;
  export let limit: number;
  export let total: number;
  export let onPrevPage: () => void;
  export let onNextPage: () => void;

  $: currentPage = Math.floor(offset / limit) + 1;
  $: totalPages = Math.ceil(total / limit);
</script>

{#if total > limit}
  <div class="pagination animate-in">
    <button class="page-btn" on:click={onPrevPage} disabled={offset === 0} aria-label={$t('rides.list.prevPage')}>
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <polyline points="15,18 9,12 15,6"/>
      </svg>
    </button>
    <span class="page-info">{$t('rides.list.pageInfo', { values: { page: currentPage, total: totalPages } })}</span>
    <button class="page-btn" on:click={onNextPage} disabled={offset + limit >= total} aria-label={$t('rides.list.nextPage')}>
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <polyline points="9,6 15,12 9,18"/>
      </svg>
    </button>
  </div>
{/if}

<style>
  .pagination {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 16px;
    padding: 16px;
  }

  .page-btn {
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    color: var(--text-secondary);
    cursor: pointer;
    transition: all 0.15s;
  }

  .page-btn:hover:not(:disabled) {
    background: var(--bg-hover);
    border-color: var(--border-default);
    color: var(--text-primary);
  }

  .page-btn:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }

  .page-info {
    font-size: 13px;
    color: var(--text-muted);
    min-width: 80px;
    text-align: center;
  }
</style>
