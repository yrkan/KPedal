<script lang="ts">
  import { t } from '$lib/i18n';
  import type { SecurityItem } from './types';

  export let items: SecurityItem[];
</script>

<div class="security-list">
  {#each items as item}
    <div class="security-item">
      <strong>{$t(item.titleKey)}</strong>
      <p>{$t(item.descKey)}</p>
    </div>
  {/each}
</div>

<style>
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
    line-height: 1.7;
  }

  @media (max-width: 640px) {
    .security-item {
      padding: 12px 14px;
    }

    .security-item strong {
      font-size: 13px;
    }

    .security-item p {
      font-size: 12px;
    }
  }
</style>
