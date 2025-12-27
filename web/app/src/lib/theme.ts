import { writable } from 'svelte/store';
import { browser } from '$app/environment';

export type Theme = 'light' | 'dark' | 'auto';

const STORAGE_KEY = 'kpedal_theme';

// Time-based theme: light from 7:00 to 19:00, dark otherwise
function getTimeBasedTheme(): 'light' | 'dark' {
  const hour = new Date().getHours();
  return hour >= 7 && hour < 19 ? 'light' : 'dark';
}

function getInitialTheme(): Theme {
  if (!browser) return 'auto';

  const stored = localStorage.getItem(STORAGE_KEY) as Theme | null;
  if (stored && ['light', 'dark', 'auto'].includes(stored)) {
    return stored;
  }
  return 'auto';
}

function resolveTheme(theme: Theme): 'light' | 'dark' {
  if (theme === 'auto') return getTimeBasedTheme();
  return theme;
}

function createThemeStore() {
  const { subscribe, set, update } = writable<Theme>(getInitialTheme());
  let timeCheckInterval: ReturnType<typeof setInterval> | null = null;

  function applyTheme(theme: Theme, instant = false) {
    if (!browser) return;

    // Disable transitions for instant theme switch (no flicker)
    if (instant) {
      document.documentElement.classList.add('no-transitions');
    }

    const resolvedTheme = resolveTheme(theme);
    document.documentElement.setAttribute('data-theme', resolvedTheme);
    localStorage.setItem(STORAGE_KEY, theme);

    // Re-enable transitions after paint
    if (instant) {
      requestAnimationFrame(() => {
        requestAnimationFrame(() => {
          document.documentElement.classList.remove('no-transitions');
        });
      });
    }

    // Start/stop time-based checking
    if (theme === 'auto') {
      startTimeCheck();
    } else {
      stopTimeCheck();
    }
  }

  function startTimeCheck() {
    if (timeCheckInterval) return;
    // Check every minute for time-based theme changes
    timeCheckInterval = setInterval(() => {
      const currentTheme = localStorage.getItem(STORAGE_KEY) as Theme;
      if (currentTheme === 'auto') {
        const resolved = getTimeBasedTheme();
        document.documentElement.setAttribute('data-theme', resolved);
      }
    }, 60000);
  }

  function stopTimeCheck() {
    if (timeCheckInterval) {
      clearInterval(timeCheckInterval);
      timeCheckInterval = null;
    }
  }

  // Apply initial theme
  if (browser) {
    applyTheme(getInitialTheme());
  }

  return {
    subscribe,
    set: (theme: Theme) => {
      set(theme);
      applyTheme(theme, true);
    },
    toggle: () => {
      update(current => {
        // Cycle: auto → light → dark → auto
        const order: Theme[] = ['auto', 'light', 'dark'];
        const currentIndex = order.indexOf(current);
        const newTheme = order[(currentIndex + 1) % order.length];
        applyTheme(newTheme, true);
        return newTheme;
      });
    },
    // Get the resolved theme (for icon display)
    getResolved: (): 'light' | 'dark' => {
      if (!browser) return 'dark';
      const current = localStorage.getItem(STORAGE_KEY) as Theme || 'auto';
      return resolveTheme(current);
    },
  };
}

export const theme = createThemeStore();
