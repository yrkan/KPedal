<script lang="ts">
  import { t } from '$lib/i18n';
  import type { FlowStep } from './types';

  export let steps: FlowStep[];
</script>

<div class="flow-diagram">
  {#each steps as step, i}
    <div class="flow-step">
      <div class="flow-number">{step.number}</div>
      <div class="flow-content">
        <h4>{$t(step.titleKey)}</h4>
        <p>{$t(step.descKey)}</p>
      </div>
    </div>
    {#if i < steps.length - 1}
      <div class="flow-arrow">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="12" y1="5" x2="12" y2="19"/>
          <polyline points="19,12 12,19 5,12"/>
        </svg>
      </div>
    {/if}
  {/each}
</div>

<style>
  .flow-diagram {
    display: flex;
    flex-direction: column;
    gap: 0;
    margin: 20px 0;
  }

  .flow-step {
    display: flex;
    gap: 16px;
    padding: 16px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
  }

  .flow-number {
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--bg-elevated);
    border-radius: 50%;
    font-size: 14px;
    font-weight: 600;
    color: var(--text-secondary);
    flex-shrink: 0;
  }

  .flow-content h4 {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 4px;
  }

  .flow-content p {
    margin: 0;
    font-size: 14px;
    color: var(--text-secondary);
    line-height: 1.7;
  }

  .flow-arrow {
    display: flex;
    justify-content: center;
    padding: 8px 0;
    color: var(--text-muted);
  }

  @media (max-width: 640px) {
    .flow-step {
      padding: 14px;
      gap: 12px;
    }

    .flow-number {
      width: 28px;
      height: 28px;
      font-size: 13px;
    }

    .flow-content h4 {
      font-size: 13px;
    }

    .flow-content p {
      font-size: 13px;
    }
  }
</style>
