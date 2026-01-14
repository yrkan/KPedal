<script lang="ts">
  import { t } from '$lib/i18n';
  import { onMount } from 'svelte';

  const categories = [
    { id: 'balance', fields: ['power-balance', 'single-balance', 'balance-trend', 'symmetry-index', 'cadence-balance'] },
    { id: 'efficiency', fields: ['efficiency', 'pedaling-score', 'hr-efficiency'] },
    { id: 'legs', fields: ['left-leg', 'right-leg'] },
    { id: 'overview', fields: ['quick-glance', 'full-overview', 'live', 'compact-multi'] },
    { id: 'modes', fields: ['fatigue-indicator', 'delta-average', 'climbing-mode', 'power-focus', 'sprint-mode'] }
  ];

  let activeCategory = $state(0);
  let activeField = $state(0);
  let autoRotate = $state(true);
  let isTransitioning = $state(false);

  // Get current category's fields
  const currentFields = $derived(categories[activeCategory].fields);
  const currentFieldId = $derived(currentFields[activeField] || currentFields[0]);

  // Auto-rotate within category with transition
  onMount(() => {
    const interval = setInterval(() => {
      if (autoRotate) {
        isTransitioning = true;
        setTimeout(() => {
          activeField = (activeField + 1) % currentFields.length;
          isTransitioning = false;
        }, 150);
      }
    }, 3500);
    return () => clearInterval(interval);
  });

  function selectCategory(index: number) {
    if (index === activeCategory) return;
    isTransitioning = true;
    setTimeout(() => {
      activeCategory = index;
      activeField = 0;
      autoRotate = true;
      isTransitioning = false;
    }, 150);
  }

  function selectField(index: number) {
    if (index === activeField) return;
    isTransitioning = true;
    setTimeout(() => {
      activeField = index;
      autoRotate = false;
      isTransitioning = false;
    }, 150);
    // Resume auto-rotate after 10 seconds
    setTimeout(() => { autoRotate = true; }, 10000);
  }
</script>

<section id="datafields" class="section datafields-section" aria-labelledby="datafields-title">
  <div class="section-header-block">
    <h2 id="datafields-title" class="section-title">{$t('landing.dataFields.title')}</h2>
    <p class="section-subtitle">{$t('landing.dataFields.subtitle')}</p>
  </div>

  <div class="datafields-showcase">
    <div class="category-tabs">
      {#each categories as cat, i}
        <button
          class="category-pill"
          class:active={activeCategory === i}
          onclick={() => selectCategory(i)}
        >
          <span class="cat-name">{$t(`landing.dataFields.categories.${cat.id}`)}</span>
          <span class="cat-count">{cat.fields.length}</span>
        </button>
      {/each}
    </div>

    <!-- Main Content -->
    <div class="datafield-content">
      <div class="preview-device">
        <div class="device-frame">
          <div class="device-notch"></div>
          <div class="preview-screen karoo" class:transitioning={isTransitioning}>
            <!-- QuickGlance LARGE: Status icon + text | Divider | BALANCE L/R columns -->
            {#if currentFieldId === 'quick-glance'}
              <div class="karoo-layout">
                <div class="karoo-section center">
                  <span class="karoo-value-lg optimal">✓</span>
                  <span class="karoo-value-sm optimal">OK</span>
                </div>
                <div class="karoo-divider"></div>
                <div class="karoo-section center">
                  <span class="karoo-label">BALANCE</span>
                  <div class="lr-cols">
                    <div class="lr-col"><span class="karoo-label">L</span><span class="karoo-value">49</span></div>
                    <div class="lr-col"><span class="karoo-label">R</span><span class="karoo-value">51</span></div>
                  </div>
                </div>
              </div>
            <!-- PowerBalance LARGE: POWER label + value 28sp | Divider | BALANCE L/R cols -->
            {:else if currentFieldId === 'power-balance'}
              <div class="karoo-layout">
                <div class="karoo-section center">
                  <span class="karoo-label">POWER</span>
                  <span class="karoo-value-lg">245</span>
                </div>
                <div class="karoo-divider"></div>
                <div class="karoo-section center">
                  <span class="karoo-label">BALANCE</span>
                  <div class="lr-cols">
                    <div class="lr-col"><span class="karoo-label">L</span><span class="karoo-value">48</span></div>
                    <div class="lr-col"><span class="karoo-label">R</span><span class="karoo-value">52</span></div>
                  </div>
                </div>
              </div>
            <!-- SingleBalance LARGE: BALANCE label | LEFT/RIGHT columns 32sp + vertical divider -->
            {:else if currentFieldId === 'single-balance'}
              <div class="karoo-layout center">
                <span class="karoo-label">BALANCE</span>
                <div class="lr-cols-wide">
                  <div class="lr-col">
                    <span class="karoo-label">LEFT</span>
                    <span class="karoo-value-xl">48</span>
                  </div>
                  <div class="v-divider-tall"></div>
                  <div class="lr-col">
                    <span class="karoo-label">RIGHT</span>
                    <span class="karoo-value-xl">52</span>
                  </div>
                </div>
              </div>
            <!-- BalanceTrend LARGE: Header | Divider | NOW 24sp | Divider | 3s/10s row -->
            {:else if currentFieldId === 'balance-trend'}
              <div class="karoo-layout">
                <div class="karoo-header center">
                  <span class="karoo-label">TREND</span>
                </div>
                <div class="karoo-divider"></div>
                <div class="karoo-section center">
                  <span class="karoo-label">NOW</span>
                  <div class="lr-inline">
                    <span class="karoo-value-lg">48</span>
                    <span class="karoo-sep">|</span>
                    <span class="karoo-value-lg">52</span>
                  </div>
                </div>
                <div class="karoo-divider"></div>
                <div class="karoo-section">
                  <div class="two-col-row">
                    <div class="col-item"><span class="karoo-label">3s</span><span class="karoo-value-md">49|51</span></div>
                    <div class="v-divider"></div>
                    <div class="col-item"><span class="karoo-label">10s</span><span class="karoo-value-md optimal">50|50</span></div>
                  </div>
                </div>
              </div>
            <!-- SymmetryIndex LARGE: Header + quality | Divider | 34sp% | Divider | L/R row -->
            {:else if currentFieldId === 'symmetry-index'}
              <div class="karoo-layout">
                <div class="karoo-header center">
                  <span class="karoo-label">SYMMETRY</span>
                  <span class="karoo-value-xs optimal"> · OK</span>
                </div>
                <div class="karoo-divider"></div>
                <div class="karoo-section center">
                  <div class="value-with-unit">
                    <span class="karoo-value-xl optimal">96</span>
                    <span class="karoo-value-md optimal">%</span>
                  </div>
                </div>
                <div class="karoo-divider"></div>
                <div class="karoo-section">
                  <div class="two-col-row">
                    <div class="col-item"><span class="karoo-label">L</span><span class="karoo-value-lg">48</span></div>
                    <div class="v-divider"></div>
                    <div class="col-item"><span class="karoo-label">R</span><span class="karoo-value-lg">52</span></div>
                  </div>
                </div>
              </div>
            <!-- CadenceBalance LARGE: CADENCE 28sp + rpm | Divider | BALANCE L/R 20sp -->
            {:else if currentFieldId === 'cadence-balance'}
              <div class="karoo-layout">
                <div class="karoo-section center">
                  <span class="karoo-label">CADENCE</span>
                  <div class="value-with-unit">
                    <span class="karoo-value-lg optimal">92</span>
                    <span class="karoo-unit">rpm</span>
                  </div>
                </div>
                <div class="karoo-divider"></div>
                <div class="karoo-section center">
                  <span class="karoo-label">BALANCE</span>
                  <div class="lr-cols">
                    <div class="lr-col"><span class="karoo-label">L</span><span class="karoo-value">48</span></div>
                    <div class="lr-col"><span class="karoo-label">R</span><span class="karoo-value">52</span></div>
                  </div>
                </div>
              </div>
            <!-- Efficiency LARGE: TE section (label+avg + L/R cols 20sp) | Divider | PS section -->
            {:else if currentFieldId === 'efficiency'}
              <div class="karoo-layout">
                <div class="karoo-section">
                  <div class="metric-row">
                    <div class="metric-label-col">
                      <span class="karoo-label">TE</span>
                      <span class="karoo-value-xs optimal">76</span>
                    </div>
                    <div class="lr-cols">
                      <div class="lr-col"><span class="karoo-label">L</span><span class="karoo-value">74</span></div>
                      <div class="lr-col"><span class="karoo-label">R</span><span class="karoo-value optimal">78</span></div>
                    </div>
                  </div>
                </div>
                <div class="karoo-divider"></div>
                <div class="karoo-section">
                  <div class="metric-row">
                    <div class="metric-label-col">
                      <span class="karoo-label">PS</span>
                      <span class="karoo-value-xs optimal">24</span>
                    </div>
                    <div class="lr-cols">
                      <div class="lr-col"><span class="karoo-label">L</span><span class="karoo-value optimal">23</span></div>
                      <div class="lr-col"><span class="karoo-label">R</span><span class="karoo-value optimal">25</span></div>
                    </div>
                  </div>
                </div>
              </div>
            <!-- PedalingScore LARGE: SCORE label + score | Divider | BAL/EFF/ZONE row -->
            {:else if currentFieldId === 'pedaling-score'}
              <div class="karoo-layout">
                <div class="karoo-section center">
                  <span class="karoo-label">SCORE</span>
                  <div class="value-with-unit">
                    <span class="karoo-value-xl optimal">78</span>
                    <span class="karoo-unit">/100</span>
                  </div>
                </div>
                <div class="karoo-divider"></div>
                <div class="karoo-section">
                  <div class="three-col-row">
                    <div class="col-item"><span class="karoo-label">BAL</span><span class="karoo-value-sm optimal">92</span></div>
                    <div class="v-divider"></div>
                    <div class="col-item"><span class="karoo-label">EFF</span><span class="karoo-value-sm">71</span></div>
                    <div class="v-divider"></div>
                    <div class="col-item"><span class="karoo-label">ZN</span><span class="karoo-value-sm optimal">65</span></div>
                  </div>
                </div>
              </div>
            <!-- HREfficiency LARGE: Header + indicator | Divider | HR 30sp + bpm | Divider | TE/PS row 20sp -->
            {:else if currentFieldId === 'hr-efficiency'}
              <div class="karoo-layout">
                <div class="karoo-header center">
                  <span class="karoo-label">HR + EFF</span>
                  <span class="karoo-value-xs optimal"> ●</span>
                </div>
                <div class="karoo-divider"></div>
                <div class="karoo-section center">
                  <span class="karoo-label">HR</span>
                  <div class="value-with-unit">
                    <span class="karoo-value-lg">145</span>
                    <span class="karoo-unit">bpm</span>
                  </div>
                </div>
                <div class="karoo-divider"></div>
                <div class="karoo-section">
                  <div class="two-col-row">
                    <div class="col-item"><span class="karoo-label">TE</span><span class="karoo-value-md optimal">76</span></div>
                    <div class="v-divider"></div>
                    <div class="col-item"><span class="karoo-label">PS</span><span class="karoo-value-md optimal">24</span></div>
                  </div>
                </div>
              </div>
            <!-- LeftLeg LARGE: Header | Divider | BALANCE 28sp | Divider | TE/PS row 20sp -->
            {:else if currentFieldId === 'left-leg'}
              <div class="karoo-layout">
                <div class="karoo-header center">
                  <span class="karoo-label">LEFT LEG</span>
                </div>
                <div class="karoo-divider"></div>
                <div class="karoo-section center">
                  <span class="karoo-label">BALANCE</span>
                  <span class="karoo-value-lg">48<span class="karoo-label">%</span></span>
                </div>
                <div class="karoo-divider"></div>
                <div class="karoo-section">
                  <div class="two-col-row">
                    <div class="col-item"><span class="karoo-label">TE</span><span class="karoo-value-md optimal">74</span></div>
                    <div class="v-divider"></div>
                    <div class="col-item"><span class="karoo-label">PS</span><span class="karoo-value-md optimal">23</span></div>
                  </div>
                </div>
              </div>
            <!-- RightLeg LARGE: Header | Divider | BALANCE 28sp | Divider | TE/PS row 20sp -->
            {:else if currentFieldId === 'right-leg'}
              <div class="karoo-layout">
                <div class="karoo-header center">
                  <span class="karoo-label">RIGHT LEG</span>
                </div>
                <div class="karoo-divider"></div>
                <div class="karoo-section center">
                  <span class="karoo-label">BALANCE</span>
                  <span class="karoo-value-lg">52<span class="karoo-label">%</span></span>
                </div>
                <div class="karoo-divider"></div>
                <div class="karoo-section">
                  <div class="two-col-row">
                    <div class="col-item"><span class="karoo-label">TE</span><span class="karoo-value-md optimal">78</span></div>
                    <div class="v-divider"></div>
                    <div class="col-item"><span class="karoo-label">PS</span><span class="karoo-value-md optimal">25</span></div>
                  </div>
                </div>
              </div>
            <!-- FullOverview LARGE: BALANCE 22sp | Divider | TE section L/R 18sp | Divider | PS section L/R 18sp -->
            {:else if currentFieldId === 'full-overview'}
              <div class="karoo-layout">
                <div class="karoo-section center">
                  <span class="karoo-label">BALANCE</span>
                  <div class="lr-cols-sm">
                    <div class="balance-val"><span class="karoo-label">L</span><span class="karoo-value">48</span></div>
                    <span class="karoo-sep">|</span>
                    <div class="balance-val"><span class="karoo-label">R</span><span class="karoo-value">52</span></div>
                  </div>
                </div>
                <div class="karoo-divider"></div>
                <div class="karoo-section">
                  <div class="metric-row">
                    <div class="metric-label-col">
                      <span class="karoo-label">TE</span>
                      <span class="karoo-value-xs optimal">76</span>
                    </div>
                    <div class="metric-values">
                      <span class="karoo-value-md">74</span>
                      <div class="v-divider"></div>
                      <span class="karoo-value-md optimal">78</span>
                    </div>
                  </div>
                </div>
                <div class="karoo-divider"></div>
                <div class="karoo-section">
                  <div class="metric-row">
                    <div class="metric-label-col">
                      <span class="karoo-label">PS</span>
                      <span class="karoo-value-xs optimal">24</span>
                    </div>
                    <div class="metric-values">
                      <span class="karoo-value-md optimal">23</span>
                      <div class="v-divider"></div>
                      <span class="karoo-value-md optimal">25</span>
                    </div>
                  </div>
                </div>
              </div>
            <!-- Live LARGE: BALANCE 20sp | Divider | TE/PS side-by-side 16sp | Divider | ZONE + bar -->
            {:else if currentFieldId === 'live'}
              <div class="karoo-layout">
                <div class="karoo-section center">
                  <span class="karoo-label">BALANCE</span>
                  <div class="lr-cols-sm">
                    <div class="balance-val"><span class="karoo-label">L</span><span class="karoo-value">49</span></div>
                    <span class="karoo-sep">|</span>
                    <div class="balance-val"><span class="karoo-label">R</span><span class="karoo-value optimal">51</span></div>
                  </div>
                </div>
                <div class="karoo-divider"></div>
                <div class="karoo-section">
                  <div class="te-ps-row">
                    <div class="te-ps-col">
                      <span class="karoo-label">TE</span>
                      <div class="lr-inline-sm"><span class="karoo-value-sm">74</span><span class="karoo-sep-sm">|</span><span class="karoo-value-sm optimal">78</span></div>
                    </div>
                    <div class="te-ps-col">
                      <span class="karoo-label">PS</span>
                      <div class="lr-inline-sm"><span class="karoo-value-sm optimal">23</span><span class="karoo-sep-sm">|</span><span class="karoo-value-sm optimal">25</span></div>
                    </div>
                  </div>
                </div>
                <div class="karoo-divider"></div>
                <div class="karoo-section center">
                  <span class="karoo-label">ZONE</span>
                  <div class="zone-values">
                    <span class="karoo-value-sm optimal">65%</span>
                    <span class="karoo-sep-sm">|</span>
                    <span class="karoo-value-sm attention">25%</span>
                    <span class="karoo-sep-sm">|</span>
                    <span class="karoo-value-sm problem">10%</span>
                  </div>
                  <div class="zone-bar-container">
                    <div class="zone-bar-fill optimal" style="width: 65%"></div>
                    <div class="zone-bar-fill attention" style="width: 25%"></div>
                    <div class="zone-bar-fill problem" style="width: 10%"></div>
                  </div>
                </div>
              </div>
            <!-- CompactMulti LARGE: Balance | Divider | TE L/R | Divider | PS L/R -->
            {:else if currentFieldId === 'compact-multi'}
              <div class="karoo-layout">
                <div class="karoo-section center">
                  <span class="karoo-label">BALANCE</span>
                  <div class="lr-cols-sm">
                    <div class="balance-val"><span class="karoo-label">L</span><span class="karoo-value">49</span></div>
                    <span class="karoo-sep">|</span>
                    <div class="balance-val"><span class="karoo-label">R</span><span class="karoo-value optimal">51</span></div>
                  </div>
                </div>
                <div class="karoo-divider"></div>
                <div class="karoo-section">
                  <div class="two-col-row">
                    <div class="col-item"><span class="karoo-label">TE L</span><span class="karoo-value-sm">74</span></div>
                    <div class="v-divider"></div>
                    <div class="col-item"><span class="karoo-label">TE R</span><span class="karoo-value-sm optimal">78</span></div>
                  </div>
                </div>
                <div class="karoo-divider"></div>
                <div class="karoo-section">
                  <div class="two-col-row">
                    <div class="col-item"><span class="karoo-label">PS L</span><span class="karoo-value-sm optimal">23</span></div>
                    <div class="v-divider"></div>
                    <div class="col-item"><span class="karoo-label">PS R</span><span class="karoo-value-sm optimal">25</span></div>
                  </div>
                </div>
              </div>
            <!-- FatigueIndicator LARGE: FATIGUE + icon + text | Divider | BAL/TE/PS trends -->
            {:else if currentFieldId === 'fatigue-indicator'}
              <div class="karoo-layout">
                <div class="karoo-section center">
                  <span class="karoo-label">FATIGUE</span>
                  <div class="fatigue-display">
                    <span class="karoo-value-lg attention">↗</span>
                    <span class="karoo-value-sm attention">RISING</span>
                  </div>
                </div>
                <div class="karoo-divider"></div>
                <div class="karoo-section">
                  <div class="three-col-row">
                    <div class="col-item"><span class="karoo-label">BAL</span><span class="karoo-value-sm">→</span></div>
                    <div class="v-divider"></div>
                    <div class="col-item"><span class="karoo-label">TE</span><span class="karoo-value-sm attention">↘</span></div>
                    <div class="v-divider"></div>
                    <div class="col-item"><span class="karoo-label">PS</span><span class="karoo-value-sm attention">↘</span></div>
                  </div>
                </div>
              </div>
            <!-- DeltaAverage LARGE: Header | Divider | BAL/TE/PS deltas 18sp | Divider | NOW/AVG summary -->
            {:else if currentFieldId === 'delta-average'}
              <div class="karoo-layout">
                <div class="karoo-header center">
                  <span class="karoo-label">DELTA</span>
                </div>
                <div class="karoo-divider"></div>
                <div class="karoo-section">
                  <div class="three-col-row">
                    <div class="col-item"><span class="karoo-label">BAL</span><span class="karoo-value-md attention">+2%</span></div>
                    <div class="v-divider"></div>
                    <div class="col-item"><span class="karoo-label">TE</span><span class="karoo-value-md problem">-5</span></div>
                    <div class="v-divider"></div>
                    <div class="col-item"><span class="karoo-label">PS</span><span class="karoo-value-md">+1</span></div>
                  </div>
                </div>
                <div class="karoo-divider"></div>
                <div class="karoo-section">
                  <div class="two-col-row">
                    <div class="col-item"><span class="karoo-label">NOW</span><span class="karoo-value-md">52%</span></div>
                    <div class="v-divider"></div>
                    <div class="col-item"><span class="karoo-label">AVG</span><span class="karoo-value-md">50%</span></div>
                  </div>
                </div>
              </div>
            <!-- ClimbingMode LARGE: Header + mode indicator | Divider | GRADE/ELEV+ 20sp | Divider | POWER/BALANCE 20sp -->
            {:else if currentFieldId === 'climbing-mode'}
              <div class="karoo-layout">
                <div class="karoo-header center">
                  <span class="karoo-value-xs attention">▲</span>
                  <span class="karoo-label"> CLIMB</span>
                </div>
                <div class="karoo-divider"></div>
                <div class="karoo-section">
                  <div class="two-col-row">
                    <div class="col-item"><span class="karoo-label-xs">GRADE</span><span class="karoo-value-md attention">+8%</span></div>
                    <div class="v-divider"></div>
                    <div class="col-item"><span class="karoo-label-xs">ELEV</span><span class="karoo-value-sm">342m</span></div>
                  </div>
                </div>
                <div class="karoo-divider"></div>
                <div class="karoo-section">
                  <div class="two-col-row">
                    <div class="col-item"><span class="karoo-label-xs">PWR</span><span class="karoo-value-sm">285</span></div>
                    <div class="v-divider"></div>
                    <div class="col-item"><span class="karoo-label-xs">BAL</span><span class="karoo-value-xs">47:53</span></div>
                  </div>
                </div>
              </div>
            <!-- PowerFocus LARGE: Header | Divider | Power | Divider | BAL/TE/PS row -->
            {:else if currentFieldId === 'power-focus'}
              <div class="karoo-layout">
                <div class="karoo-header center">
                  <span class="karoo-label">POWER</span>
                </div>
                <div class="karoo-divider"></div>
                <div class="karoo-section center">
                  <div class="value-with-unit">
                    <span class="karoo-value-xl">245</span>
                    <span class="karoo-unit-lg">W</span>
                  </div>
                </div>
                <div class="karoo-divider"></div>
                <div class="karoo-section">
                  <div class="compact-stats-row">
                    <div class="compact-stat"><span class="karoo-label-xs">BAL</span><span class="karoo-value-xs">48:52</span></div>
                    <div class="compact-stat"><span class="karoo-label-xs">TE</span><span class="karoo-value-xs optimal">76</span></div>
                    <div class="compact-stat"><span class="karoo-label-xs">PS</span><span class="karoo-value-xs optimal">24</span></div>
                  </div>
                </div>
              </div>
            <!-- SprintMode LARGE: Header + indicator | Divider | Power | Divider | CAD/BAL/GRADE row -->
            {:else if currentFieldId === 'sprint-mode'}
              <div class="karoo-layout">
                <div class="karoo-header center">
                  <span class="karoo-value-xs problem">●</span>
                  <span class="karoo-label"> SPRINT</span>
                </div>
                <div class="karoo-divider"></div>
                <div class="karoo-section center">
                  <div class="value-with-unit">
                    <span class="karoo-value-xl">485</span>
                    <span class="karoo-unit-lg">W</span>
                  </div>
                </div>
                <div class="karoo-divider"></div>
                <div class="karoo-section">
                  <div class="compact-stats-row">
                    <div class="compact-stat"><span class="karoo-label-xs">CAD</span><span class="karoo-value-xs optimal">102</span></div>
                    <div class="compact-stat"><span class="karoo-label-xs">BAL</span><span class="karoo-value-xs">46:54</span></div>
                    <div class="compact-stat"><span class="karoo-label-xs">GRD</span><span class="karoo-value-xs">+2%</span></div>
                  </div>
                </div>
              </div>
            {/if}
          </div>
        </div>
      </div>

      <div class="preview-right">
        <div class="datatype-grid">
          {#each currentFields as fieldId, i}
            <button
              class="datatype-card"
              class:active={activeField === i}
              onclick={() => selectField(i)}
            >
              {$t(`landing.dataFields.${fieldId.replace(/-/g, '')}.name`)}
            </button>
          {/each}
        </div>

        <div class="preview-info">
          <h4>{$t(`landing.dataFields.${currentFieldId.replace(/-/g, '')}.name`)}</h4>
          <p>{$t(`landing.dataFields.${currentFieldId.replace(/-/g, '')}.desc`)}</p>
        </div>
      </div>
    </div>
  </div>
</section>

<style>
  .datafields-section {
    text-align: center;
  }

  .section-header-block {
    margin-bottom: 40px;
  }

  .datafields-showcase {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 20px;
    overflow: hidden;
  }

  .category-tabs {
    display: flex;
    gap: 8px;
    padding: 16px 20px;
    border-bottom: 1px solid var(--border-subtle);
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: none;
    justify-content: center;
  }
  .category-tabs::-webkit-scrollbar { display: none; }

  .category-pill {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 18px;
    background: var(--bg-base);
    border: 1px solid var(--border-subtle);
    border-radius: 100px;
    cursor: pointer;
    transition: all 0.2s ease;
    white-space: nowrap;
    color: var(--text-secondary);
  }
  .category-pill:hover {
    border-color: var(--border-strong);
    background: var(--bg-elevated);
  }
  .category-pill.active {
    background: var(--text-primary);
    border-color: var(--text-primary);
    color: var(--bg-base);
  }
  .cat-name {
    font-size: 14px;
    font-weight: 500;
  }
  .cat-count {
    font-size: 11px;
    opacity: 0.6;
    font-weight: 600;
  }

  .datafield-content {
    display: flex;
    gap: 48px;
    padding: 48px;
    align-items: flex-start;
  }

  .preview-device {
    flex-shrink: 0;
  }

  .device-frame {
    background: #2D2D2D;
    border-radius: 24px;
    padding: 12px 10px;
    box-shadow:
      0 0 0 2px #1A1A1A,
      0 8px 32px rgba(0, 0, 0, 0.4);
  }
  .device-notch {
    display: none;
  }

  .preview-screen.karoo {
    width: 144px;
    height: 240px;
    background: #000000;
    border-radius: 8px;
    overflow: hidden;
    font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
    padding: 1px;
    transition: opacity 0.15s ease;
  }

  .preview-screen.transitioning {
    opacity: 0.6;
  }

  /* Right Side */
  .preview-right {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 24px;
    min-width: 0;
  }

  .datatype-grid {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .datatype-card {
    padding: 10px 16px;
    background: var(--bg-base);
    border: 1px solid var(--border-subtle);
    border-radius: 10px;
    cursor: pointer;
    transition: all 0.2s ease;
    font-size: 13px;
    color: var(--text-secondary);
  }

  .datatype-card:hover {
    border-color: var(--border-strong);
    background: var(--bg-elevated);
  }

  .datatype-card.active {
    background: var(--text-primary);
    border-color: var(--text-primary);
    color: var(--bg-base);
  }

  .preview-info {
    text-align: left;
    padding: 20px;
    background: var(--bg-base);
    border-radius: 14px;
    border: 1px solid var(--border-subtle);
  }
  .preview-info h4 {
    font-size: 18px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 8px;
  }
  .preview-info p {
    font-size: 14px;
    color: var(--text-tertiary);
    line-height: 1.6;
  }

  /* ========================================
     KAROO LAYOUT STYLES
     Matches Android GlanceColors exactly:
     - Background: #000000
     - Frame: #1A1A1A
     - White: #FFFFFF
     - Label: #AAAAAA
     - Separator: #666666
     - Divider: #555555
     - Optimal: #4CAF50
     - Attention: #FF9800
     - Problem: #F44336
     ======================================== */

  .karoo-layout {
    width: 100%;
    height: 100%;
    padding: 6px;
    display: flex;
    flex-direction: column;
    background: #000000;
    gap: 4px;
  }
  .karoo-layout.center {
    align-items: center;
    justify-content: center;
  }
  .karoo-layout.compact {
    gap: 2px;
  }

  .karoo-section {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 4px;
  }
  .karoo-section.center {
    align-items: center;
  }
  .karoo-section-sm {
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 2px;
    padding: 4px 0;
  }

  /* Divider - matches GlanceColors.Divider (#555555) */
  .karoo-divider {
    height: 1px;
    background: #555555;
    margin: 3px 8px;
  }

  /* Header - for DataType titles */
  .karoo-header {
    display: flex;
    align-items: center;
    justify-content: flex-start;
    padding: 4px 8px;
    gap: 2px;
  }
  .karoo-header.center {
    justify-content: center;
  }

  /* Column layouts - for 2 and 3 column rows with dividers */
  .two-col-row, .three-col-row {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    width: 100%;
    padding: 0 4px;
  }
  .col-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
    flex: 1;
  }

  /* Value with unit - horizontal alignment */
  .value-with-unit {
    display: flex;
    align-items: baseline;
    justify-content: center;
    gap: 2px;
  }

  /* Balance value - inline L/R display */
  .balance-val {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 1px;
  }

  /* Fatigue display */
  .fatigue-display {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
  }

  /* Zone bar - for time-in-zone visualization */
  .zone-bar-container {
    display: flex;
    width: 100%;
    height: 4px;
    background: #333333;
    border-radius: 2px;
    overflow: hidden;
    margin-top: 4px;
  }
  .zone-bar-fill {
    height: 100%;
  }
  .zone-bar-fill.optimal { background: #4CAF50; }
  .zone-bar-fill.attention { background: #FF9800; }
  .zone-bar-fill.problem { background: #F44336; }

  /* Labels - matches GlanceColors.Label (#AAAAAA) */
  .karoo-label {
    font-size: 10px;
    color: #AAAAAA;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    font-weight: 500;
    text-align: center;
  }
  .karoo-label-xs {
    font-size: 8px;
    color: #AAAAAA;
    text-transform: uppercase;
    letter-spacing: 0.3px;
    font-weight: 500;
  }
  .karoo-label-sm {
    font-size: 9px;
    color: #AAAAAA;
    text-transform: uppercase;
  }
  .karoo-label-dim {
    font-size: 10px;
    color: #666666;
  }

  /* Values - matches GlanceColors.White (#FFFFFF) */
  .karoo-value {
    font-size: 20px;
    font-weight: 700;
    color: #FFFFFF;
    font-variant-numeric: tabular-nums;
  }
  .karoo-value-xs {
    font-size: 10px;
    font-weight: 700;
    color: #FFFFFF;
  }
  .karoo-value-sm {
    font-size: 12px;
    font-weight: 700;
    color: #FFFFFF;
  }
  .karoo-value-md {
    font-size: 16px;
    font-weight: 700;
    color: #FFFFFF;
    font-variant-numeric: tabular-nums;
  }
  .karoo-value-lg {
    font-size: 24px;
    font-weight: 700;
    color: #FFFFFF;
    font-variant-numeric: tabular-nums;
    line-height: 1;
  }
  .karoo-value-xl {
    font-size: 36px;
    font-weight: 700;
    color: #FFFFFF;
    font-variant-numeric: tabular-nums;
    line-height: 1;
  }
  .karoo-value-dim {
    font-size: 14px;
    font-weight: 600;
    color: #AAAAAA;
    font-variant-numeric: tabular-nums;
  }

  /* Extra large values - for sprint mode power (40sp equivalent) */
  .karoo-value-xxl {
    font-size: 42px;
    font-weight: 700;
    color: #FFFFFF;
    font-variant-numeric: tabular-nums;
    line-height: 1;
  }

  /* Units - matches GlanceColors.Label */
  .karoo-unit {
    font-size: 11px;
    color: #AAAAAA;
    font-weight: 500;
    margin-left: 2px;
  }
  .karoo-unit-lg {
    font-size: 14px;
    color: #AAAAAA;
    font-weight: 500;
    margin-left: 2px;
  }
  .karoo-unit-xl {
    font-size: 16px;
    color: #AAAAAA;
    font-weight: 500;
    margin-left: 3px;
    align-self: flex-end;
    padding-bottom: 6px;
  }

  /* Separators - matches GlanceColors.Separator (#666666) */
  .karoo-sep {
    font-size: 16px;
    color: #666666;
    margin: 0 4px;
    font-weight: 400;
  }
  .karoo-sep-sm {
    font-size: 14px;
    color: #666666;
    margin: 0 3px;
    font-weight: 400;
  }
  .karoo-sep-lg {
    font-size: 20px;
    color: #666666;
    margin: 0 6px;
    font-weight: 400;
  }
  .karoo-sep-dim {
    font-size: 14px;
    color: #555555;
    margin: 0 3px;
    font-weight: 400;
  }

  /* Status colors - matches GlanceColors exactly */
  .optimal { color: #4CAF50 !important; }
  .attention { color: #FF9800 !important; }
  .problem { color: #F44336 !important; }

  .status-row {
    display: flex;
    align-items: center;
    justify-content: center;
    flex: 1;
  }
  .status-text {
    font-size: 28px;
    font-weight: 700;
  }

  /* L/R Columns - for balance displays */
  .lr-cols {
    display: flex;
    justify-content: center;
    gap: 16px;
  }
  .lr-cols-wide {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 12px;
    margin-top: 4px;
  }
  .lr-cols-sm {
    display: flex;
    justify-content: center;
    gap: 12px;
  }
  .lr-col {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
  }

  /* L/R Inline - horizontal with separator */
  .lr-inline {
    display: flex;
    align-items: baseline;
    justify-content: center;
  }
  .lr-inline-sm {
    display: flex;
    align-items: baseline;
    justify-content: center;
  }

  /* Metric Row - label column + values */
  .metric-row {
    display: flex;
    align-items: center;
    padding: 0 8px;
  }
  .metric-label-col {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 1px;
    min-width: 32px;
  }
  .metric-values {
    flex: 1;
    display: flex;
    justify-content: center;
    align-items: baseline;
    gap: 4px;
  }

  /* Vertical Divider */
  .v-divider {
    width: 1px;
    height: 16px;
    background: #666666;
  }
  .v-divider-tall {
    width: 1px;
    height: 40px;
    background: #555555;
  }

  /* TE/PS Row */
  .te-ps-row {
    display: flex;
    justify-content: space-around;
  }
  .te-ps-col {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
  }

  /* Stat Row - horizontal stats */
  .stat-row {
    display: flex;
    justify-content: center;
    gap: 16px;
    margin-top: 6px;
  }
  .stat-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
  }

  /* Score Row */
  .score-row {
    display: flex;
    justify-content: center;
    gap: 16px;
    margin-top: 8px;
  }
  .score-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
  }

  /* Zone Values */
  .zone-values {
    display: flex;
    align-items: baseline;
    justify-content: center;
    gap: 2px;
  }

  /* Balance Row */
  .balance-row {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 16px;
  }
  .balance-col {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
  }

  /* Power Row */
  .power-row {
    display: flex;
    align-items: baseline;
    justify-content: center;
    gap: 2px;
  }

  /* Metric Header */
  .metric-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 4px;
  }

  /* L/R Row */
  .lr-row {
    display: flex;
    align-items: center;
    justify-content: center;
  }
  .lr-row-sm {
    display: flex;
    align-items: center;
    justify-content: center;
  }

  /* Single Value */
  .single-value {
    display: flex;
    align-items: baseline;
    justify-content: center;
    gap: 2px;
  }

  /* Balance Bar */
  .balance-bar {
    width: 100%;
    height: 4px;
    background: #333333;
    border-radius: 2px;
    position: relative;
    margin: 6px 0;
  }
  .bar-fill {
    height: 100%;
    background: #FFFFFF;
    border-radius: 2px;
  }
  .bar-center {
    position: absolute;
    top: -2px;
    left: 50%;
    transform: translateX(-50%);
    width: 2px;
    height: 8px;
    background: #FFFFFF;
  }
  .bar-labels {
    display: flex;
    justify-content: space-between;
    font-size: 9px;
    color: #AAAAAA;
    padding: 0 2px;
  }

  /* Live Bar */
  .live-bar {
    width: 100%;
    height: 12px;
    background: #333333;
    border-radius: 2px;
    overflow: hidden;
  }
  .live-fill {
    height: 100%;
  }
  .live-fill.optimal { background: #4CAF50; }
  .live-values {
    display: flex;
    justify-content: space-between;
    padding: 4px 2px 0;
  }

  /* Zones */
  .zones-section {
    display: flex;
    flex-direction: column;
    gap: 4px;
    flex: 1;
    justify-content: center;
  }
  .zone-row {
    display: flex;
    align-items: center;
    gap: 6px;
  }
  .zone-label {
    font-size: 8px;
    width: 28px;
    text-transform: uppercase;
    font-weight: 600;
  }
  .zone-bar {
    flex: 1;
    height: 5px;
    background: #333333;
    border-radius: 2px;
    overflow: hidden;
  }
  .zone-fill {
    height: 100%;
  }
  .zone-fill.optimal { background: #4CAF50; }
  .zone-fill.attention { background: #FF9800; }
  .zone-fill.problem { background: #F44336; }
  .zone-time {
    font-size: 9px;
    color: #888888;
    width: 30px;
    text-align: right;
  }

  /* Score Breakdown */
  .score-breakdown {
    display: flex;
    gap: 16px;
    margin-top: 12px;
  }
  .score-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
  }

  /* Fatigue */
  .fatigue-graph {
    flex: 1;
    display: flex;
    align-items: center;
    padding: 4px 0;
  }
  .fatigue-graph svg {
    width: 100%;
    height: 30px;
  }
  .fatigue-stats {
    display: flex;
    justify-content: space-between;
  }
  .stat-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
  }

  /* Diff Display */
  .diff-display {
    text-align: center;
    font-size: 12px;
    font-weight: 500;
    margin-top: 4px;
  }

  /* Leg Stats */
  .leg-stats {
    display: flex;
    justify-content: center;
    gap: 24px;
  }

  .mode-stats {
    display: flex;
    justify-content: center;
    gap: 20px;
  }

  /* Symmetry Bar */
  .symmetry-bar {
    width: 100%;
    height: 6px;
    background: #333333;
    border-radius: 3px;
    overflow: hidden;
    margin: 8px 0;
  }
  .sym-fill {
    height: 100%;
    border-radius: 2px;
  }
  .sym-fill.optimal { background: #4CAF50; }

  /* HR Stats */
  .hr-stats {
    display: flex;
    justify-content: center;
    gap: 20px;
  }

  /* Power Balance Bar */
  .power-balance-bar {
    width: 100%;
    height: 6px;
    background: #333333;
    border-radius: 3px;
    overflow: hidden;
    margin: 8px 0;
  }
  .pb-fill {
    height: 100%;
    background: #888888;
  }
  .pb-labels {
    display: flex;
    justify-content: space-between;
  }

  /* Sprint */
  .sprint-peak {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
  }
  .sprint-color {
    color: #FF9800 !important;
  }

  /* Compact Multi */
  .compact-row {
    display: flex;
    justify-content: space-around;
    padding: 6px 0;
  }
  .compact-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
  }

  /* Compact Stats Row - ultra compact 3-stat row */
  .compact-stats-row {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 8px;
    width: 100%;
  }
  .compact-stat {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 1px;
  }
  .compact-stat + .compact-stat {
    border-left: 1px solid #555555;
    padding-left: 8px;
  }

  @media (max-width: 768px) {
    .section-header-block { margin-bottom: 32px; }
    .category-tabs { padding: 12px 16px; gap: 6px; justify-content: flex-start; }
    .category-pill { padding: 8px 14px; }
    .cat-name { font-size: 13px; }
    .cat-count { display: none; }
    .datafield-content { flex-direction: column; gap: 32px; padding: 32px 24px; }
    .preview-device { margin: 0 auto; }
    .device-frame { border-radius: 28px; }
    .preview-right { width: 100%; }
    .datatype-grid { justify-content: center; }
    .preview-info { text-align: center; padding: 16px; }
    .preview-info h4 { font-size: 17px; }
    .preview-info p { font-size: 13px; }
  }

  @media (max-width: 480px) {
    .category-tabs { padding: 10px 12px; }
    .category-pill { padding: 6px 12px; gap: 6px; }
    .cat-name { font-size: 12px; }
    .datafield-content { padding: 24px 16px; gap: 24px; }
    .device-frame { transform: scale(0.95); border-radius: 24px; padding: 8px; }
    .datatype-card { padding: 8px 12px; font-size: 12px; }
    .preview-info { padding: 14px; }
    .preview-info h4 { font-size: 16px; }
    .preview-info p { font-size: 12px; }
  }
</style>
