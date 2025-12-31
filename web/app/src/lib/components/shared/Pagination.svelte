<script lang="ts">
  import { t } from '$lib/i18n';

  export let offset: number;
  export let limit: number;
  export let total: number;
  export let onPrev: () => void;
  export let onNext: () => void;
  export let pageInfoKey: string = 'common.pageInfo';

  $: currentPage = Math.floor(offset / limit) + 1;
  $: totalPages = Math.ceil(total / limit);
  $: hasPrev = offset > 0;
  $: hasNext = offset + limit < total;
</script>

{#if total > limit}
  <div class="pagination animate-in">
    <button
      class="page-btn"
      on:click={onPrev}
      disabled={!hasPrev}
      aria-label={$t('common.prevPage')}
    >
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <polyline points="15,18 9,12 15,6"/>
      </svg>
    </button>
    <span class="page-info">{$t(pageInfoKey, { values: { page: currentPage, total: totalPages } })}</span>
    <button
      class="page-btn"
      on:click={onNext}
      disabled={!hasNext}
      aria-label={$t('common.nextPage')}
    >
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <polyline points="9,6 15,12 9,18"/>
      </svg>
    </button>
  </div>
{/if}
