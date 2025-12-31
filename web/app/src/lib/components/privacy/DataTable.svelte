<script lang="ts">
  import { t } from '$lib/i18n';
  import type { DataRow } from './types';

  export let rows: DataRow[];
  export let variant: 'data' | 'infra' | 'retention' = 'data';
</script>

<div class="table" class:data-table={variant === 'data'} class:infra-table={variant === 'infra'} class:retention-table={variant === 'retention'}>
  {#each rows as row}
    <div class="row">
      <span class="field">{@html row.field}</span>
      <span class="purpose">{row.purpose}</span>
    </div>
  {/each}
</div>

<style>
  .table {
    display: flex;
    flex-direction: column;
    gap: 1px;
    background: var(--border-subtle);
    border-radius: 8px;
    overflow: hidden;
    margin: 12px 0;
  }

  .row {
    display: grid;
    grid-template-columns: 180px 1fr;
    background: var(--bg-surface);
    padding: 12px 16px;
  }

  .field {
    font-weight: 500;
    color: var(--text-primary);
    font-size: 14px;
  }

  .field :global(code) {
    background: var(--bg-elevated);
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 13px;
    font-family: 'SF Mono', Monaco, monospace;
    word-break: break-all;
  }

  .purpose {
    color: var(--text-tertiary);
    font-size: 14px;
  }

  @media (max-width: 640px) {
    .row {
      grid-template-columns: 1fr;
      gap: 4px;
      padding: 10px 14px;
    }

    .field, .purpose {
      font-size: 13px;
    }
  }
</style>
