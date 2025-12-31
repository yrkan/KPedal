<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { isAuthenticated, authFetch } from '$lib/auth';
  import { t } from '$lib/i18n';
  import type { KPedalSettings } from '$lib/types/settings';
  import { DEFAULT_SETTINGS } from '$lib/types/settings';
  import {
    AccountSection,
    DevicesSection,
    MetricsSection,
    AppearanceSection,
    DataSection,
    SessionSection,
    PrivacySection,
    AboutSection,
    SaveToast
  } from '$lib/components/settings';

  let devicesSection: DevicesSection;
  let metricsSection: MetricsSection;

  // KPedal settings state
  let kpedalSettings: KPedalSettings = { ...DEFAULT_SETTINGS };
  let settingsLoading = true;
  let settingsError = false;
  let savingSettings = false;
  let settingsSaved = false;
  let saveTimeout: ReturnType<typeof setTimeout> | null = null;

  onMount(async () => {
    if (!$isAuthenticated) {
      goto('/login');
      return;
    }
    await Promise.all([
      devicesSection?.loadDevices(),
      loadSettings()
    ]);
  });

  // Load KPedal settings from server
  async function loadSettings() {
    settingsLoading = true;
    settingsError = false;
    try {
      const res = await authFetch('/settings');
      const data = await res.json();
      if (data.success && data.data?.settings) {
        kpedalSettings = { ...DEFAULT_SETTINGS, ...data.data.settings };
      }
    } catch (err) {
      settingsError = true;
    } finally {
      settingsLoading = false;
    }
  }

  // Save settings with debounce
  async function saveSettings(changes: Partial<KPedalSettings>) {
    // Apply changes immediately for responsive UI
    kpedalSettings = { ...kpedalSettings, ...changes };

    // Clear previous timeout
    if (saveTimeout) clearTimeout(saveTimeout);

    // Debounce API call
    saveTimeout = setTimeout(async () => {
      savingSettings = true;
      settingsSaved = false;
      try {
        const res = await authFetch('/settings', {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(changes),
        });
        const data = await res.json();
        if (data.success && data.data?.settings) {
          kpedalSettings = { ...DEFAULT_SETTINGS, ...data.data.settings };
          settingsSaved = true;
          setTimeout(() => settingsSaved = false, 2000);
        }
      } catch (err) {
        console.error('Failed to save settings:', err);
      } finally {
        savingSettings = false;
      }
    }, 500);
  }
</script>

<svelte:head>
  <title>{$t('settings.title')} - KPedal</title>
</svelte:head>

<div class="page settings-page">
  <div class="container container-md">
    <header class="page-header animate-in">
      <h1>{$t('settings.title')}</h1>
    </header>

    <AccountSection />
    <DevicesSection bind:this={devicesSection} />
    <MetricsSection
      bind:this={metricsSection}
      settings={kpedalSettings}
      loading={settingsLoading}
      error={settingsError}
      onSave={saveSettings}
    />
    <AppearanceSection />
    <DataSection />
    <SessionSection />
    <PrivacySection />
    <AboutSection />
  </div>
</div>

<SaveToast saving={savingSettings} saved={settingsSaved} />

<style>
  .settings-page {
    padding: 24px 16px 100px;
    min-height: 100vh;
    background: var(--bg-base);
  }

  .container-md {
    max-width: 640px;
    margin: 0 auto;
    display: flex;
    flex-direction: column;
    gap: 24px;
  }

  .page-header {
    margin-bottom: 8px;
  }

  .page-header h1 {
    font-size: 28px;
    font-weight: 700;
    color: var(--text-primary);
    margin: 0;
  }

  @media (max-width: 640px) {
    .settings-page {
      padding: 16px 16px 80px;
    }

    .container-md {
      gap: 20px;
    }

    .page-header h1 {
      font-size: 24px;
    }
  }
</style>
