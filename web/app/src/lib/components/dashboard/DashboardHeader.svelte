<script lang="ts">
  export let selectedPeriod: '7' | '14' | '30' | '60';
  export let ridesIn7Days: number;
  export let ridesIn14Days: number;
  export let ridesIn30Days: number;
  export let ridesIn60Days: number;

  import { createEventDispatcher } from 'svelte';
  const dispatch = createEventDispatcher<{ periodChange: '7' | '14' | '30' | '60' }>();

  function selectPeriod(period: '7' | '14' | '30' | '60') {
    dispatch('periodChange', period);
  }
</script>

<header class="dash-header animate-in">
  <div class="dash-controls">
    <div class="period-selector">
      <button class="period-btn" class:active={selectedPeriod === '7'} on:click={() => selectPeriod('7')}>7d</button>
      {#if ridesIn14Days > ridesIn7Days}
        <button class="period-btn" class:active={selectedPeriod === '14'} on:click={() => selectPeriod('14')}>14d</button>
      {/if}
      {#if ridesIn30Days > ridesIn14Days}
        <button class="period-btn" class:active={selectedPeriod === '30'} on:click={() => selectPeriod('30')}>30d</button>
      {/if}
      {#if ridesIn60Days > ridesIn30Days}
        <button class="period-btn" class:active={selectedPeriod === '60'} on:click={() => selectPeriod('60')}>60d</button>
      {/if}
    </div>
  </div>
</header>

<style>
  .dash-header {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    margin-bottom: 20px;
  }
  .dash-controls {
    display: flex;
    gap: 8px;
  }

  .period-selector {
    display: flex;
    gap: 8px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 10px;
    padding: 4px;
  }
  .period-btn {
    padding: 8px 16px;
    font-size: 13px;
    font-weight: 500;
    color: var(--text-secondary);
    background: transparent;
    border: none;
    border-radius: 6px;
    cursor: pointer;
    transition: all 0.15s ease;
  }
  .period-btn:hover:not(:disabled) {
    color: var(--text-primary);
    background: var(--bg-hover);
  }
  .period-btn.active {
    background: var(--color-accent);
    color: var(--color-accent-text);
    font-weight: 600;
  }

  /* Responsive - 768px */
  @media (max-width: 768px) {
    .dash-header {
      margin-bottom: 16px;
    }
  }

  /* Responsive - 480px */
  @media (max-width: 480px) {
    .dash-header {
      justify-content: flex-end;
    }
    .period-btn {
      padding: 6px 12px;
      font-size: 12px;
    }
  }

  /* Responsive - 360px */
  @media (max-width: 360px) {
    .dash-header {
      margin-bottom: 14px;
    }
    .period-btn {
      padding: 5px 10px;
      font-size: 11px;
    }
  }
</style>
