<script lang="ts">
  import { t } from '$lib/i18n';
  import type { ThirdParty } from './types';

  export let items: ThirdParty[];
</script>

<div class="third-party-list">
  {#each items as item}
    <div class="third-party-item">
      <h4>{$t(item.titleKey)}</h4>
      <p>{$t(item.descKey)}</p>
      <a href={item.linkUrl} target="_blank" rel="noopener noreferrer">{$t(item.linkText)}</a>
    </div>
  {/each}
</div>

<style>
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
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 6px;
  }

  .third-party-item p {
    margin-bottom: 8px;
    font-size: 14px;
    color: var(--text-secondary);
    line-height: 1.7;
  }

  .third-party-item a {
    font-size: 13px;
    color: var(--color-accent);
  }

  .third-party-item a:hover {
    text-decoration: underline;
  }

  @media (max-width: 640px) {
    .third-party-item {
      padding: 14px;
    }

    .third-party-item p {
      font-size: 13px;
    }
  }
</style>
