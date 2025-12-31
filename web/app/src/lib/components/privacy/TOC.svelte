<script lang="ts">
  import { t } from '$lib/i18n';
  import type { TocItem } from './types';

  export let items: TocItem[];
</script>

<nav class="toc">
  <h2>{$t('privacy.toc.title')}</h2>
  <ol>
    {#each items as item}
      <li><a href="#{item.id}">{$t(item.labelKey)}</a></li>
    {/each}
  </ol>
</nav>

<style>
  .toc {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 20px 24px;
    margin-bottom: 40px;
  }

  .toc h2 {
    font-size: 12px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    color: var(--text-tertiary);
    margin-bottom: 12px;
  }

  .toc ol {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 8px 24px;
    list-style: none;
    padding: 0;
    margin: 0;
    counter-reset: toc;
  }

  .toc li {
    counter-increment: toc;
  }

  .toc a {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    color: var(--text-secondary);
    text-decoration: none;
    transition: color 0.15s ease;
  }

  .toc a::before {
    content: counter(toc) ".";
    color: var(--text-muted);
    font-size: 13px;
  }

  .toc a:hover {
    color: var(--text-primary);
  }

  @media (max-width: 640px) {
    .toc {
      padding: 16px;
    }

    .toc ol {
      grid-template-columns: 1fr;
    }

    .toc a {
      font-size: 13px;
    }
  }
</style>
