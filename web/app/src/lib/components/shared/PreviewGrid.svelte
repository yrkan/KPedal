<script lang="ts">
  export let label: string;
</script>

<div class="preview-container">
  <span class="preview-label">{label}</span>
  <div class="preview-grid">
    <slot />
  </div>
</div>

<style>
  .preview-container {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 16px;
    margin-bottom: 20px;
  }

  .preview-label {
    display: block;
    font-size: 11px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.3px;
    color: var(--text-muted);
    margin-bottom: 12px;
  }

  .preview-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }

  :global(.preview-item) {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 12px;
    background: var(--bg-base);
    border-radius: 8px;
    font-size: 13px;
    color: var(--text-secondary);
  }

  :global(.preview-item svg) {
    color: var(--text-muted);
    flex-shrink: 0;
  }

  @media (max-width: 480px) {
    .preview-grid {
      grid-template-columns: 1fr;
    }
  }
</style>
