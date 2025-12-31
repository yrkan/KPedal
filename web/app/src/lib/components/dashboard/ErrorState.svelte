<script lang="ts">
  import { t } from '$lib/i18n';

  export let onRetry: () => void;
</script>

<div class="error-state">
  <p>{$t('common.loadError')}</p>
  <button class="btn btn-primary" on:click={onRetry}>{$t('common.retry')}</button>
</div>

<style>
  .error-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 60vh;
    gap: 16px;
    color: var(--text-secondary);
  }

  .error-state p {
    font-size: 15px;
    color: var(--text-secondary);
  }
</style>
