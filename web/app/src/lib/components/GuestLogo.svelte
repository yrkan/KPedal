<script lang="ts">
  import { t } from '$lib/i18n';
</script>

<a href="/" class="guest-logo" aria-label={$t('aria.home')}>
  <span class="logo-dot" aria-hidden="true"></span>
  <span class="logo-text">KPedal</span>
</a>

<style>
  .guest-logo {
    display: flex;
    align-items: center;
    gap: 10px;
    position: fixed;
    top: 20px;
    left: 20px;
    text-decoration: none;
    z-index: 100;
  }

  .logo-dot {
    width: 8px;
    height: 8px;
    background: var(--color-optimal);
    border-radius: 50%;
  }

  .logo-text {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
  }

  @media (max-width: 640px) {
    .guest-logo {
      top: 16px;
      left: 16px;
    }

    .logo-text {
      font-size: 14px;
    }
  }
</style>
