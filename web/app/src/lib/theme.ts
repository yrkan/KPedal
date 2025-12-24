import { writable } from 'svelte/store';
import { browser } from '$app/environment';

export type Theme = 'light' | 'dark' | 'system';

const STORAGE_KEY = 'kpedal_theme';

function getInitialTheme(): Theme {
  if (!browser) return 'system';

  const stored = localStorage.getItem(STORAGE_KEY) as Theme | null;
  if (stored && ['light', 'dark', 'system'].includes(stored)) {
    return stored;
  }
  return 'system';
}

function getSystemTheme(): 'light' | 'dark' {
  if (!browser) return 'dark';
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

function createThemeStore() {
  const { subscribe, set, update } = writable<Theme>(getInitialTheme());

  function applyTheme(theme: Theme) {
    if (!browser) return;

    const resolvedTheme = theme === 'system' ? getSystemTheme() : theme;
    document.documentElement.setAttribute('data-theme', resolvedTheme);
    localStorage.setItem(STORAGE_KEY, theme);
  }

  // Apply initial theme
  if (browser) {
    applyTheme(getInitialTheme());

    // Listen for system theme changes
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
      const currentTheme = localStorage.getItem(STORAGE_KEY) as Theme;
      if (currentTheme === 'system') {
        applyTheme('system');
      }
    });
  }

  return {
    subscribe,
    set: (theme: Theme) => {
      set(theme);
      applyTheme(theme);
    },
    toggle: () => {
      update(current => {
        const resolvedCurrent = current === 'system' ? getSystemTheme() : current;
        const newTheme = resolvedCurrent === 'dark' ? 'light' : 'dark';
        applyTheme(newTheme);
        return newTheme;
      });
    },
  };
}

export const theme = createThemeStore();
