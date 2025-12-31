<script lang="ts">
  import { t } from '$lib/i18n';
  import { onMount } from 'svelte';

  let activeDataField = 0;

  $: dataFields = [
    { id: 'quick', name: $t('landing.dataFields.quickGlance.name'), desc: $t('landing.dataFields.quickGlance.desc') },
    { id: 'balance', name: $t('landing.dataFields.powerBalance.name'), desc: $t('landing.dataFields.powerBalance.desc') },
    { id: 'efficiency', name: $t('landing.dataFields.efficiency.name'), desc: $t('landing.dataFields.efficiency.desc') },
    { id: 'full', name: $t('landing.dataFields.fullOverview.name'), desc: $t('landing.dataFields.fullOverview.desc') },
    { id: 'trend', name: $t('landing.dataFields.balanceTrend.name'), desc: $t('landing.dataFields.balanceTrend.desc') }
  ];

  onMount(() => {
    const interval = setInterval(() => {
      activeDataField = (activeDataField + 1) % dataFields.length;
    }, 4000);
    return () => clearInterval(interval);
  });
</script>

<section id="datafields" class="section datafields-section" aria-labelledby="datafields-title">
  <h2 id="datafields-title" class="section-title">{$t('landing.dataFields.title')}</h2>
  <p class="section-subtitle">{$t('landing.dataFields.subtitle')}</p>

  <div class="datafields-showcase">
    <div class="datafield-tabs">
      <button class="datafield-tab" class:active={activeDataField === 0} on:click={() => activeDataField = 0}>
        {$t('landing.dataFields.tabs.quickGlance')}
      </button>
      <button class="datafield-tab" class:active={activeDataField === 1} on:click={() => activeDataField = 1}>
        {$t('landing.dataFields.tabs.balance')}
      </button>
      <button class="datafield-tab" class:active={activeDataField === 2} on:click={() => activeDataField = 2}>
        {$t('landing.dataFields.tabs.efficiency')}
      </button>
      <button class="datafield-tab" class:active={activeDataField === 3} on:click={() => activeDataField = 3}>
        {$t('landing.dataFields.tabs.fullView')}
      </button>
      <button class="datafield-tab" class:active={activeDataField === 4} on:click={() => activeDataField = 4}>
        {$t('landing.dataFields.tabs.trend')}
      </button>
    </div>
    <div class="datafield-preview">
      <div class="preview-device">
        <div class="device-frame">
          <div class="device-notch"></div>
          <div class="preview-screen karoo">
            {#if activeDataField === 0}
              <!-- Quick Glance -->
              <div class="karoo-layout quick-glance">
                <div class="qg-status-section">
                  <svg class="qg-status-icon optimal" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                    <polyline points="20 6 9 17 4 12"/>
                  </svg>
                  <div class="qg-status-text">{$t('landing.dataFields.karoo.optimal')}</div>
                </div>
                <div class="karoo-divider"></div>
                <div class="qg-balance-section">
                  <div class="karoo-label">{$t('landing.dataFields.karoo.balance')}</div>
                  <div class="balance-values">
                    <span class="balance-num">48</span>
                    <span class="balance-num">52</span>
                  </div>
                  <div class="balance-bar"><div class="bar-fill" style="width: 48%"></div></div>
                  <div class="balance-labels"><span>L</span><span>R</span></div>
                </div>
              </div>
            {:else if activeDataField === 1}
              <!-- Power Balance -->
              <div class="karoo-layout power-balance">
                <div class="pb-header">
                  <span class="karoo-label">{$t('landing.dataFields.karoo.balance')}</span>
                  <span class="pb-status optimal">{$t('landing.dataFields.karoo.optimal')}</span>
                </div>
                <div class="pb-main">
                  <div class="pb-side">
                    <span class="pb-value">48</span>
                    <span class="pb-label">{$t('landing.dataFields.karoo.left')}</span>
                  </div>
                  <div class="pb-divider"></div>
                  <div class="pb-side">
                    <span class="pb-value">52</span>
                    <span class="pb-label">{$t('landing.dataFields.karoo.right')}</span>
                  </div>
                </div>
                <div class="balance-bar"><div class="bar-fill" style="width: 48%"></div></div>
              </div>
            {:else if activeDataField === 2}
              <!-- Efficiency -->
              <div class="karoo-layout efficiency">
                <div class="eff-section">
                  <div class="eff-header">
                    <span class="karoo-label">{$t('landing.dataFields.karoo.torqueEff')}</span>
                    <span class="eff-avg optimal">76%</span>
                  </div>
                  <div class="eff-values">
                    <div class="eff-side"><span class="eff-num">74</span><span class="eff-side-label">L</span></div>
                    <div class="eff-divider"></div>
                    <div class="eff-side"><span class="eff-num">77</span><span class="eff-side-label">R</span></div>
                  </div>
                </div>
                <div class="karoo-divider"></div>
                <div class="eff-section">
                  <div class="eff-header">
                    <span class="karoo-label">{$t('landing.dataFields.karoo.smoothness')}</span>
                    <span class="eff-avg optimal">24%</span>
                  </div>
                  <div class="eff-values">
                    <div class="eff-side"><span class="eff-num">23</span><span class="eff-side-label">L</span></div>
                    <div class="eff-divider"></div>
                    <div class="eff-side"><span class="eff-num">25</span><span class="eff-side-label">R</span></div>
                  </div>
                </div>
              </div>
            {:else if activeDataField === 3}
              <!-- Full Overview -->
              <div class="karoo-layout full-overview">
                <div class="fo-section">
                  <div class="fo-header"><span class="karoo-label">{$t('landing.dataFields.karoo.balance')}</span><span class="fo-status optimal">{$t('landing.dataFields.karoo.ok')}</span></div>
                  <div class="fo-row"><span class="fo-num">48</span><span class="fo-sep">/</span><span class="fo-num">52</span></div>
                </div>
                <div class="karoo-divider"></div>
                <div class="fo-section">
                  <div class="fo-header"><span class="karoo-label">{$t('landing.dataFields.karoo.te')}</span><span class="fo-status optimal">76%</span></div>
                  <div class="fo-row"><span class="fo-num sm">74</span><span class="fo-sep">/</span><span class="fo-num sm">77</span></div>
                </div>
                <div class="karoo-divider"></div>
                <div class="fo-section">
                  <div class="fo-header"><span class="karoo-label">{$t('landing.dataFields.karoo.ps')}</span><span class="fo-status optimal">24%</span></div>
                  <div class="fo-row"><span class="fo-num sm">23</span><span class="fo-sep">/</span><span class="fo-num sm">25</span></div>
                </div>
              </div>
            {:else if activeDataField === 4}
              <!-- Balance Trend -->
              <div class="karoo-layout balance-trend">
                <div class="bt-section main">
                  <div class="karoo-label">{$t('landing.dataFields.karoo.now')}</div>
                  <div class="bt-values lg"><span>48</span><span class="bt-sep">:</span><span>52</span></div>
                </div>
                <div class="karoo-divider"></div>
                <div class="bt-section">
                  <div class="karoo-label">{$t('landing.dataFields.karoo.avg3s')}</div>
                  <div class="bt-values"><span>49</span><span class="bt-sep">:</span><span>51</span></div>
                </div>
                <div class="bt-section">
                  <div class="karoo-label">{$t('landing.dataFields.karoo.avg10s')}</div>
                  <div class="bt-values"><span>50</span><span class="bt-sep">:</span><span>50</span></div>
                </div>
              </div>
            {/if}
          </div>
        </div>
      </div>
      <div class="preview-info">
        <h4>{dataFields[activeDataField].name}</h4>
        <p>{dataFields[activeDataField].desc}</p>
      </div>
    </div>
  </div>
</section>

<style>
  .datafields-section {
    text-align: center;
  }
  .datafields-showcase {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 20px;
    overflow: hidden;
  }
  .datafield-tabs {
    display: flex;
    border-bottom: 1px solid var(--border-subtle);
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: none;
    padding: 0;
  }
  .datafield-tabs::-webkit-scrollbar { display: none; }
  .datafield-tab {
    flex: 1;
    min-width: 80px;
    padding: 16px 14px;
    background: none;
    border: none;
    cursor: pointer;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    color: var(--text-muted);
    transition: all 0.3s ease;
    border-bottom: 2px solid transparent;
    margin-bottom: -1px;
    position: relative;
  }
  .datafield-tab:hover {
    color: var(--text-secondary);
    background: var(--bg-hover);
  }
  .datafield-tab.active {
    color: var(--text-primary);
    border-bottom-color: var(--color-accent);
  }
  .datafield-preview {
    display: flex;
    gap: 40px;
    padding: 40px;
    align-items: center;
  }

  /* Device Frame - Premium look */
  .preview-device {
    flex-shrink: 0;
  }
  .device-frame {
    background: linear-gradient(160deg, #2a2a2e 0%, #1a1a1c 50%, #0c0c0e 100%);
    border-radius: 32px;
    padding: 10px;
    box-shadow:
      0 30px 80px rgba(0, 0, 0, 0.5),
      0 0 0 1px rgba(255, 255, 255, 0.05),
      inset 0 1px 0 rgba(255, 255, 255, 0.08);
    position: relative;
  }
  .device-notch {
    position: absolute;
    top: 4px;
    left: 50%;
    transform: translateX(-50%);
    width: 24px;
    height: 4px;
    background: #0c0c0e;
    border-radius: 2px;
  }
  .preview-screen.karoo {
    width: 160px;
    height: 260px;
    background: #000000;
    border-radius: 24px;
    overflow: hidden;
    font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
  }
  .preview-info {
    flex: 1;
    text-align: left;
  }
  .preview-info h4 {
    font-size: 20px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 14px;
    letter-spacing: -0.4px;
  }
  .preview-info p {
    font-size: 15px;
    color: var(--text-tertiary);
    line-height: 1.7;
    max-width: 320px;
  }

  /* Karoo Layout Base Styles */
  .karoo-layout {
    width: 100%;
    height: 100%;
    padding: 16px 14px;
    display: flex;
    flex-direction: column;
    background: #000000;
  }
  .karoo-label {
    font-size: 9px;
    color: #6e6e73;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    font-weight: 500;
  }
  .karoo-divider {
    height: 1px;
    background: rgba(255, 255, 255, 0.08);
    margin: 8px 0;
  }
  .karoo-layout .optimal {
    color: #30d158 !important;
  }

  /* Balance Bar (reused across layouts) */
  .balance-bar {
    width: 100%;
    height: 6px;
    background: #3a3a3c;
    border-radius: 3px;
    overflow: hidden;
    position: relative;
  }
  .bar-fill {
    position: absolute;
    left: 0;
    top: 0;
    height: 100%;
    background: linear-gradient(90deg, #8e8e93 0%, #a2a2a7 100%);
    border-radius: inherit;
  }
  .balance-values {
    display: flex;
    justify-content: space-between;
    width: 100%;
    padding: 0 8px;
  }
  .balance-num {
    font-size: 28px;
    font-weight: 600;
    color: #ffffff;
    font-variant-numeric: tabular-nums;
  }
  .balance-labels {
    display: flex;
    justify-content: space-between;
    font-size: 9px;
    color: #6e6e73;
    margin-top: 4px;
    padding: 0 4px;
    font-weight: 500;
  }

  /* Quick Glance */
  .quick-glance { justify-content: space-between; }
  .qg-status-section {
    flex: 0.4;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 6px;
  }
  .qg-status-icon {
    width: 48px;
    height: 48px;
    color: #30d158;
  }
  .qg-status-text {
    font-size: 10px;
    color: #6e6e73;
    font-weight: 500;
  }
  .qg-balance-section {
    flex: 0.6;
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding-top: 8px;
  }

  /* Power Balance */
  .power-balance {
    justify-content: space-between;
    gap: 12px;
  }
  .pb-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .pb-status {
    font-size: 9px;
    font-weight: 600;
    padding: 3px 8px;
    border-radius: 4px;
    background: rgba(48, 209, 88, 0.15);
    color: #30d158;
  }
  .pb-main {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
  }
  .pb-side {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
  }
  .pb-value {
    font-size: 42px;
    font-weight: 600;
    color: #ffffff;
    font-variant-numeric: tabular-nums;
    line-height: 1;
  }
  .pb-label {
    font-size: 10px;
    color: #6e6e73;
    font-weight: 500;
  }
  .pb-divider {
    width: 1px;
    height: 48px;
    background: rgba(255, 255, 255, 0.1);
  }

  /* Efficiency */
  .efficiency { justify-content: space-between; }
  .eff-section {
    flex: 1;
    display: flex;
    flex-direction: column;
    padding: 6px 0;
  }
  .eff-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .eff-avg {
    font-size: 10px;
    font-weight: 600;
  }
  .eff-values {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
  }
  .eff-side {
    display: flex;
    flex-direction: column;
    align-items: center;
    flex: 1;
    gap: 2px;
  }
  .eff-num {
    font-size: 32px;
    font-weight: 600;
    color: #ffffff;
    font-variant-numeric: tabular-nums;
    line-height: 1;
  }
  .eff-side-label {
    font-size: 9px;
    color: #6e6e73;
    font-weight: 500;
  }
  .eff-divider {
    width: 1px;
    height: 36px;
    background: rgba(255, 255, 255, 0.1);
  }

  /* Full Overview */
  .full-overview {
    justify-content: space-between;
    gap: 0;
  }
  .fo-section {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 4px;
    padding: 4px 0;
  }
  .fo-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .fo-status {
    font-size: 9px;
    font-weight: 600;
  }
  .fo-row {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
  }
  .fo-num {
    font-size: 24px;
    font-weight: 600;
    color: #ffffff;
    font-variant-numeric: tabular-nums;
  }
  .fo-num.sm { font-size: 20px; }
  .fo-sep {
    font-size: 16px;
    color: #48484a;
    font-weight: 400;
  }

  /* Balance Trend */
  .balance-trend {
    justify-content: space-between;
    gap: 4px;
  }
  .bt-section {
    display: flex;
    flex-direction: column;
    flex: 1;
  }
  .bt-section.main { flex: 1.4; }
  .bt-values {
    display: flex;
    justify-content: center;
    align-items: center;
    flex: 1;
    gap: 6px;
  }
  .bt-values span {
    font-size: 26px;
    font-weight: 600;
    color: #ffffff;
    font-variant-numeric: tabular-nums;
  }
  .bt-values.lg span { font-size: 36px; }
  .bt-sep {
    font-size: 20px;
    color: #48484a;
    font-weight: 400;
  }
  .bt-section:not(.main) .bt-values span {
    color: #8e8e93;
    font-weight: 400;
    font-size: 22px;
  }
  .bt-section:not(.main) .bt-sep {
    font-size: 16px;
  }

  /* Responsive - 768px */
  @media (max-width: 768px) {
    .datafield-tabs {
      overflow-x: auto;
      -webkit-overflow-scrolling: touch;
      scrollbar-width: none;
      flex-wrap: nowrap;
      padding: 0 12px;
    }
    .datafield-tabs::-webkit-scrollbar { display: none; }
    .datafield-tab {
      flex-shrink: 0;
      padding: 12px 16px;
      font-size: 13px;
      white-space: nowrap;
    }
    .datafield-preview {
      flex-direction: column;
      gap: 24px;
    }
    .preview-device { margin: 0 auto; }
    .device-frame { border-radius: 28px; }
    .preview-info { text-align: center; padding: 0 16px; }
    .preview-info h4 { font-size: 17px; }
    .preview-info p { font-size: 14px; }
  }

  /* Responsive - 480px */
  @media (max-width: 480px) {
    .datafield-tabs {
      padding: 0 10px;
    }
    .datafield-tab {
      padding: 10px 14px;
      font-size: 12px;
    }
    .datafield-preview {
      padding: 20px 16px;
      border-radius: 14px;
      gap: 20px;
    }
    .device-frame {
      border-radius: 24px;
      padding: 8px;
      transform: scale(0.95);
    }
    .preview-info { padding: 0 8px; }
    .preview-info h4 { font-size: 16px; margin-bottom: 6px; }
    .preview-info p { font-size: 13px; line-height: 1.5; }
  }

  /* Responsive - 360px */
  @media (max-width: 360px) {
    .datafield-tabs { padding: 0 8px; }
    .datafield-tab { padding: 8px 12px; font-size: 11px; }
    .datafield-preview { padding: 16px 12px; }
    .device-frame { transform: scale(0.9); }
  }

  /* Touch devices */
  @media (hover: none) and (pointer: coarse) {
    .datafield-tab:hover {
      transform: none;
    }
    .datafield-tab:active {
      background: var(--bg-active);
    }
  }

  /* Landscape phone */
  @media (max-width: 768px) and (orientation: landscape) {
    .device-frame { transform: scale(0.8); }
  }
</style>
