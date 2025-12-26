<script lang="ts">
  /**
   * InfoTip - Accessible tooltip component for explaining metrics
   *
   * Features:
   * - Hover to show (desktop)
   * - Click to toggle (mobile)
   * - Fixed positioning (escapes overflow:hidden)
   * - Smart positioning to stay in viewport
   * - Auto-adjusts when going off-screen
   */
  import { onMount } from 'svelte';

  export let text: string;
  export let title: string = '';
  export let position: 'top' | 'bottom' | 'left' | 'right' = 'top';
  export let size: 'sm' | 'md' = 'sm';

  let isOpen = false;
  let triggerEl: HTMLButtonElement;
  let popupEl: HTMLDivElement;
  let hoverTimeout: ReturnType<typeof setTimeout>;
  let popupStyle = '';
  let actualPosition = position;
  let isMobile = false;

  onMount(() => {
    isMobile = window.innerWidth <= 480;
    const handleResize = () => {
      isMobile = window.innerWidth <= 480;
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  });

  function updatePosition() {
    if (!triggerEl || !isOpen) return;

    // On mobile, use bottom sheet style (CSS handles this)
    if (isMobile) {
      popupStyle = '';
      actualPosition = position;
      return;
    }

    const rect = triggerEl.getBoundingClientRect();
    const gap = 10;
    const tooltipWidth = 240;
    const tooltipHeight = 80;
    const padding = 12;

    const vw = window.innerWidth;
    const vh = window.innerHeight;

    let top = 0;
    let left = 0;
    let finalPosition = position;

    // Calculate initial position
    switch (position) {
      case 'top':
        top = rect.top - gap;
        left = rect.left + rect.width / 2;
        // Check if goes off top
        if (top - tooltipHeight < padding) {
          finalPosition = 'bottom';
          top = rect.bottom + gap;
        }
        break;
      case 'bottom':
        top = rect.bottom + gap;
        left = rect.left + rect.width / 2;
        // Check if goes off bottom
        if (top + tooltipHeight > vh - padding) {
          finalPosition = 'top';
          top = rect.top - gap;
        }
        break;
      case 'left':
        top = rect.top + rect.height / 2;
        left = rect.left - gap;
        // Check if goes off left
        if (left - tooltipWidth < padding) {
          finalPosition = 'right';
          left = rect.right + gap;
        }
        break;
      case 'right':
        top = rect.top + rect.height / 2;
        left = rect.right + gap;
        // Check if goes off right
        if (left + tooltipWidth > vw - padding) {
          finalPosition = 'left';
          left = rect.left - gap;
        }
        break;
    }

    // Horizontal bounds check for top/bottom positions
    if (finalPosition === 'top' || finalPosition === 'bottom') {
      const halfWidth = tooltipWidth / 2;
      if (left - halfWidth < padding) {
        left = padding + halfWidth;
      } else if (left + halfWidth > vw - padding) {
        left = vw - padding - halfWidth;
      }
    }

    // Vertical bounds check for left/right positions
    if (finalPosition === 'left' || finalPosition === 'right') {
      const halfHeight = tooltipHeight / 2;
      if (top - halfHeight < padding) {
        top = padding + halfHeight;
      } else if (top + halfHeight > vh - padding) {
        top = vh - padding - halfHeight;
      }
    }

    actualPosition = finalPosition;
    popupStyle = `top: ${top}px; left: ${left}px;`;
  }

  function open() {
    clearTimeout(hoverTimeout);
    isOpen = true;
    // Update position after state change
    requestAnimationFrame(updatePosition);
  }

  function delayedClose() {
    hoverTimeout = setTimeout(() => {
      isOpen = false;
    }, 150);
  }

  function handleMouseEnter() {
    // Only use hover on desktop
    if (!isMobile) {
      open();
    }
  }

  function handleMouseLeave() {
    // Only use hover on desktop
    if (!isMobile) {
      delayedClose();
    }
  }

  function toggle(e: MouseEvent) {
    e.preventDefault();
    e.stopPropagation();
    if (isOpen) {
      isOpen = false;
    } else {
      open();
    }
  }

  function handleClickOutside(e: MouseEvent) {
    if (isOpen && triggerEl && !triggerEl.contains(e.target as Node) &&
        popupEl && !popupEl.contains(e.target as Node)) {
      isOpen = false;
    }
  }

  function handleKeydown(e: KeyboardEvent) {
    if (e.key === 'Escape' && isOpen) {
      isOpen = false;
    }
  }

  function handleScroll() {
    if (isOpen) {
      updatePosition();
    }
  }
</script>

<svelte:window
  on:click={handleClickOutside}
  on:keydown={handleKeydown}
  on:scroll={handleScroll}
  on:resize={updatePosition}
/>

<span class="info-tip">
  <button
    bind:this={triggerEl}
    class="info-tip-trigger {size}"
    class:active={isOpen}
    on:click={toggle}
    on:mouseenter={handleMouseEnter}
    on:mouseleave={handleMouseLeave}
    on:focus={handleMouseEnter}
    on:blur={handleMouseLeave}
    aria-label={title || 'More information'}
    aria-expanded={isOpen}
    type="button"
  >
    <svg viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
      <circle cx="8" cy="8" r="6.5" />
      <path d="M8 11V8M8 5.5V5" stroke-linecap="round" />
    </svg>
  </button>
</span>

{#if isOpen}
  <div
    bind:this={popupEl}
    class="info-tip-popup {actualPosition}"
    style={popupStyle}
    role="tooltip"
    on:mouseenter={open}
    on:mouseleave={delayedClose}
  >
    <div class="info-tip-content">
      {#if title}
        <span class="info-tip-title">{title}</span>
      {/if}
      <span class="info-tip-text">{@html text}</span>
    </div>
  </div>
{/if}

<style>
  .info-tip {
    position: relative;
    display: inline-flex;
    align-items: center;
    vertical-align: middle;
    margin-left: 3px;
  }

  .info-tip-trigger {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 1px;
    border: none;
    background: transparent;
    color: var(--text-muted);
    opacity: 0.5;
    cursor: pointer;
    border-radius: 50%;
    transition: color 0.2s ease, background-color 0.2s ease, opacity 0.2s ease;
    -webkit-tap-highlight-color: transparent;
  }

  .info-tip-trigger.sm {
    width: 14px;
    height: 14px;
  }

  .info-tip-trigger.sm svg {
    width: 11px;
    height: 11px;
  }

  .info-tip-trigger.md {
    width: 18px;
    height: 18px;
  }

  .info-tip-trigger.md svg {
    width: 14px;
    height: 14px;
  }

  .info-tip-trigger:hover {
    color: var(--text-secondary);
    opacity: 1;
    background: var(--bg-hover);
  }

  .info-tip-trigger.active {
    color: var(--text-primary);
    opacity: 1;
    background: var(--bg-hover);
  }

  .info-tip-trigger:focus-visible {
    outline: 2px solid var(--color-primary, #3b82f6);
    outline-offset: 1px;
  }

  /* Fixed positioned popup - escapes overflow:hidden */
  .info-tip-popup {
    position: fixed;
    z-index: 9999;
    width: max-content;
    min-width: 180px;
    max-width: 260px;
    pointer-events: auto;
    animation: tooltip-in 0.15s ease-out;
    white-space: normal;
  }

  @keyframes tooltip-in {
    from {
      opacity: 0;
      transform: translate(var(--tx, -50%), var(--ty, 0)) scale(0.96);
    }
    to {
      opacity: 1;
      transform: translate(var(--tx, -50%), var(--ty, 0)) scale(1);
    }
  }

  /* Position transforms */
  .info-tip-popup.top {
    --tx: -50%;
    --ty: -100%;
    transform: translate(-50%, -100%);
  }

  .info-tip-popup.bottom {
    --tx: -50%;
    --ty: 0%;
    transform: translate(-50%, 0%);
  }

  .info-tip-popup.left {
    --tx: -100%;
    --ty: -50%;
    transform: translate(-100%, -50%);
  }

  .info-tip-popup.right {
    --tx: 0%;
    --ty: -50%;
    transform: translate(0%, -50%);
  }

  .info-tip-content {
    padding: 12px 14px;
    background: var(--bg-elevated);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    box-shadow:
      0 4px 24px rgba(0, 0, 0, 0.15),
      0 0 0 1px rgba(255, 255, 255, 0.05) inset;
    backdrop-filter: blur(8px);
    white-space: normal;
    text-align: left;
    text-transform: none;
    letter-spacing: normal;
    font-weight: 400;
  }

  .info-tip-title {
    display: block;
    margin-bottom: 6px;
    font-size: 13px;
    font-weight: 600;
    color: var(--text-primary);
    letter-spacing: -0.02em;
  }

  .info-tip-text {
    display: block;
    font-size: 12px;
    line-height: 1.55;
    color: var(--text-secondary);
    white-space: normal;
    word-wrap: break-word;
    overflow-wrap: break-word;
  }

  /* Text formatting */
  .info-tip-text :global(strong),
  .info-tip-text :global(b) {
    color: var(--text-primary);
    font-weight: 600;
  }

  .info-tip-text :global(em),
  .info-tip-text :global(i) {
    font-style: italic;
    color: var(--text-primary);
  }

  .info-tip-text :global(code) {
    display: inline-block;
    padding: 1px 5px;
    font-family: ui-monospace, monospace;
    font-size: 11px;
    background: var(--bg-base);
    border-radius: 4px;
    color: var(--color-primary, var(--text-primary));
  }

  .info-tip-text :global(.tip-value) {
    display: inline-block;
    padding: 2px 6px;
    font-weight: 600;
    font-size: 11px;
    background: var(--bg-base);
    border-radius: 4px;
    color: var(--text-primary);
  }

  .info-tip-text :global(.tip-good) {
    color: var(--color-optimal-text, #22c55e);
  }

  .info-tip-text :global(.tip-warn) {
    color: var(--color-attention-text, #f59e0b);
  }

  .info-tip-text :global(.tip-bad) {
    color: var(--color-problem-text, #ef4444);
  }

  .info-tip-text :global(br) {
    display: block;
    content: "";
    margin-top: 6px;
  }

  /* Mobile: bottom sheet style */
  @media (max-width: 480px) {
    .info-tip-popup {
      left: 16px !important;
      right: 16px !important;
      bottom: 16px !important;
      top: auto !important;
      max-width: none;
      width: auto;
      transform: none !important;
      animation: tooltip-slide-up 0.2s ease-out;
    }

    @keyframes tooltip-slide-up {
      from {
        opacity: 0;
        transform: translateY(16px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    .info-tip-content {
      padding: 16px 18px;
      border-radius: 16px;
      box-shadow: 0 -4px 32px rgba(0, 0, 0, 0.25);
    }

    .info-tip-title {
      font-size: 14px;
    }

    .info-tip-text {
      font-size: 13px;
    }
  }
</style>
