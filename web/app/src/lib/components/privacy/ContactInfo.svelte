<script lang="ts">
  import { t } from '$lib/i18n';
</script>

<div class="contact-info">
  <a href="mailto:privacy@kpedal.com" class="contact-link">
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
      <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
      <polyline points="22,6 12,13 2,6"/>
    </svg>
    {$t('privacy.contact.email')}
  </a>
  <a href="https://github.com/yrkan/kpedal/issues" target="_blank" rel="noopener noreferrer" class="contact-link">
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
      <path d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22"/>
    </svg>
    {$t('privacy.contact.github')}
  </a>
</div>

<style>
  .contact-info {
    display: flex;
    gap: 16px;
    margin-top: 16px;
  }

  .contact-link {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 12px 20px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 8px;
    font-size: 14px;
    color: var(--text-primary);
    text-decoration: none;
    transition: all 0.15s ease;
  }

  .contact-link:hover {
    background: var(--bg-hover);
    border-color: var(--border-default);
  }

  .contact-link svg {
    color: var(--text-tertiary);
  }

  @media (max-width: 640px) {
    .contact-info {
      flex-direction: column;
    }

    .contact-link {
      padding: 10px 16px;
      font-size: 13px;
    }
  }
</style>
