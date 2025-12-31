<script lang="ts">
  import { t } from '$lib/i18n';
</script>

<section class="section faq-section" aria-labelledby="faq-title" itemscope itemtype="https://schema.org/FAQPage">
  <h2 id="faq-title" class="section-title">{$t('landing.faq.title')}</h2>

  <div class="faq-list" role="list">
    <details class="faq-item">
      <summary>{$t('landing.faq.q1.q')}</summary>
      <p>{@html $t('landing.faq.q1.a')}</p>
    </details>

    <details class="faq-item">
      <summary>{$t('landing.faq.q2.q')}</summary>
      <p>{@html $t('landing.faq.q2.a')}</p>
    </details>

    <details class="faq-item">
      <summary>{$t('landing.faq.q3.q')}</summary>
      <p>{@html $t('landing.faq.q3.a')}</p>
    </details>

    <details class="faq-item">
      <summary>{$t('landing.faq.q4.q')}</summary>
      <p>{@html $t('landing.faq.q4.a')}</p>
    </details>

    <details class="faq-item">
      <summary>{$t('landing.faq.q5.q')}</summary>
      <p>{@html $t('landing.faq.q5.a')}</p>
    </details>

    <details class="faq-item">
      <summary>{$t('landing.faq.q6.q')}</summary>
      <p>{@html $t('landing.faq.q6.a')}</p>
    </details>

    <details class="faq-item">
      <summary>{$t('landing.faq.q7.q')}</summary>
      <p>{@html $t('landing.faq.q7.a')}</p>
    </details>

    <details class="faq-item">
      <summary>{$t('landing.faq.q8.q')}</summary>
      <p>{@html $t('landing.faq.q8.a')}</p>
    </details>

    <details class="faq-item">
      <summary>{$t('landing.faq.q9.q')}</summary>
      <p>{@html $t('landing.faq.q9.a')}</p>
    </details>

    <details class="faq-item">
      <summary>{$t('landing.faq.q10.q')}</summary>
      <p>{@html $t('landing.faq.q10.a')}</p>
    </details>

    <details class="faq-item">
      <summary>{$t('landing.faq.q11.q')}</summary>
      <p>{@html $t('landing.faq.q11.a')}</p>
    </details>

    <details class="faq-item">
      <summary>{$t('landing.faq.q12.q')}</summary>
      <p>{@html $t('landing.faq.q12.a')}</p>
    </details>

    <details class="faq-item">
      <summary>{$t('landing.faq.q13.q')}</summary>
      <p>{@html $t('landing.faq.q13.a')}</p>
    </details>

    <details class="faq-item">
      <summary>{$t('landing.faq.q14.q')}</summary>
      <p>{@html $t('landing.faq.q14.a')}</p>
    </details>

    <details class="faq-item">
      <summary>{$t('landing.faq.q15.q')}</summary>
      <p>{@html $t('landing.faq.q15.a')}</p>
    </details>

    <details class="faq-item">
      <summary>{$t('landing.faq.q16.q')}</summary>
      <p>{@html $t('landing.faq.q16.a')}</p>
    </details>

    <details class="faq-item">
      <summary>{$t('landing.faq.q17.q')}</summary>
      <p>{@html $t('landing.faq.q17.a')}</p>
    </details>

    <details class="faq-item">
      <summary>{$t('landing.faq.q18.q')}</summary>
      <p>{@html $t('landing.faq.q18.a')}</p>
    </details>
  </div>
</section>

<style>
  .faq-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
    max-width: 800px;
    margin: 0 auto;
  }
  .faq-item {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 16px;
    overflow: hidden;
    transition: all 0.3s ease;
  }
  .faq-item:hover {
    border-color: var(--border-default);
  }
  .faq-item summary {
    padding: 18px 22px;
    font-size: 14px;
    font-weight: 500;
    color: var(--text-primary);
    cursor: pointer;
    list-style: none;
    display: flex;
    justify-content: space-between;
    align-items: center;
    letter-spacing: -0.1px;
    transition: all 0.3s ease;
  }
  .faq-item summary::-webkit-details-marker { display: none; }
  .faq-item summary::after {
    content: '+';
    font-size: 18px;
    color: var(--text-muted);
    transition: all 0.3s ease;
    font-weight: 300;
  }
  .faq-item[open] summary::after { content: '−'; }
  .faq-item[open] summary {
    border-bottom: 1px solid var(--border-subtle);
    color: var(--text-primary);
  }
  .faq-item p {
    padding: 14px 22px 18px;
    font-size: 14px;
    color: var(--text-tertiary);
    line-height: 1.7;
    margin: 0;
  }

  /* Responsive - 480px */
  @media (max-width: 480px) {
    .faq-item summary {
      padding: 16px 18px;
      font-size: 14px;
    }
    .faq-item p { padding: 12px 18px 16px; font-size: 14px; }
  }

  /* Responsive - 360px */
  @media (max-width: 360px) {
    .faq-section { margin-bottom: 40px; }
    .faq-list { gap: 8px; }
    .faq-item { border-radius: 10px; }
    .faq-item summary {
      padding: 14px 16px;
      font-size: 13px;
    }
    .faq-item summary::after {
      font-size: 16px;
    }
    .faq-item p {
      padding: 12px 16px 14px;
      font-size: 12px;
      line-height: 1.6;
    }
  }

  /* Responsive - 320px */
  @media (max-width: 320px) {
    .faq-item summary { font-size: 12px; padding: 12px 14px; }
    .faq-item p { font-size: 12px; padding: 10px 14px 12px; }
  }

  /* Touch devices */
  @media (hover: none) and (pointer: coarse) {
    .faq-item summary:active { background: var(--bg-hover); }
  }
</style>
