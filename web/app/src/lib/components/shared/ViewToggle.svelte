<script lang="ts">
  import { t } from '$lib/i18n';

  export let viewMode: 'table' | 'cards';
  export let tableLabel: string | undefined = undefined;
  export let cardsLabel: string | undefined = undefined;
</script>

<div class="view-toggle">
  <button
    class="view-btn"
    class:active={viewMode === 'table'}
    on:click={() => viewMode = 'table'}
    title={tableLabel || $t('common.tableView')}
  >
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <line x1="3" y1="6" x2="21" y2="6"/>
      <line x1="3" y1="12" x2="21" y2="12"/>
      <line x1="3" y1="18" x2="21" y2="18"/>
    </svg>
  </button>
  <button
    class="view-btn"
    class:active={viewMode === 'cards'}
    on:click={() => viewMode = 'cards'}
    title={cardsLabel || $t('common.cardView')}
  >
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <rect x="3" y="3" width="7" height="7"/>
      <rect x="14" y="3" width="7" height="7"/>
      <rect x="3" y="14" width="7" height="7"/>
      <rect x="14" y="14" width="7" height="7"/>
    </svg>
  </button>
</div>
