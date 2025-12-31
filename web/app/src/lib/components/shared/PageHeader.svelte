<script lang="ts">
  import { t } from '$lib/i18n';

  export let title: string;
  export let backHref: string = '/';
  export let backLabel: string | undefined = undefined;
</script>

<header class="page-header animate-in">
  <a href={backHref} class="back-link" aria-label={backLabel || $t('common.backToDashboard')}>
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <polyline points="15,18 9,12 15,6"/>
    </svg>
  </a>
  <h1>{title}</h1>
  <slot />
</header>
