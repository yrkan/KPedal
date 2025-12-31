<script lang="ts">
  import { t } from '$lib/i18n';

  export let saving = false;
  export let saved = false;
</script>

{#if saving || saved}
  <div class="save-toast" class:saving class:saved>
    {#if saving}
      <div class="toast-spinner"></div>
      <span>{$t('settings.saving')}</span>
    {:else}
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
        <polyline points="20 6 9 17 4 12"/>
      </svg>
      <span>{$t('settings.saved')}</span>
    {/if}
  </div>
{/if}

<style>
  .save-toast {
    position: fixed;
    bottom: 100px;
    right: 24px;
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 16px;
    background: var(--bg-elevated);
    border: 1px solid var(--border-subtle);
    border-radius: 10px;
    font-size: 13px;
    font-weight: 500;
    color: var(--text-secondary);
    box-shadow: var(--shadow-lg);
    z-index: 1000;
    animation: toast-in 0.2s ease-out;
  }

  .save-toast.saved {
    background: var(--color-optimal-soft);
    border-color: var(--color-optimal);
    color: var(--color-optimal-text);
  }

  .save-toast.saved svg {
    color: var(--color-optimal);
  }

  .toast-spinner {
    width: 14px;
    height: 14px;
    border: 2px solid var(--border-default);
    border-top-color: var(--text-secondary);
    border-radius: 50%;
    animation: spin 0.6s linear infinite;
  }

  @keyframes toast-in {
    from {
      opacity: 0;
      transform: translateY(10px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  @keyframes spin {
    to { transform: rotate(360deg); }
  }

  @media (max-width: 768px) {
    .save-toast {
      bottom: 80px;
      right: 16px;
    }
  }
</style>
