<script lang="ts">
  export let title: string;
  export let icon: string;
</script>

<section class="settings-section animate-in">
  <div class="section-header">
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
      {@html icon}
    </svg>
    <h2>{title}</h2>
  </div>
  <slot />
</section>

<style>
  .settings-section {
    margin-bottom: 32px;
  }

  .section-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
  }

  .section-header :global(svg) {
    color: var(--text-muted);
  }

  .settings-section h2 {
    font-size: 12px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    color: var(--text-tertiary);
  }

  @media (max-width: 640px) {
    .settings-section {
      margin-bottom: 24px;
    }
  }
</style>
