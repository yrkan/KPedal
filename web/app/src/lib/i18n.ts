import { browser } from '$app/environment';
import { init, register, getLocaleFromNavigator, locale, waitLocale } from 'svelte-i18n';

// Available locales
export const locales = ['en', 'es'] as const;
export type Locale = (typeof locales)[number];

export const localeNames: Record<Locale, string> = {
	en: 'English',
	es: 'Español'
};

// Register locales with dynamic imports
register('en', () => import('../locales/en.json'));
register('es', () => import('../locales/es.json'));

// Get saved locale or detect from browser
function getInitialLocale(): string {
	if (browser) {
		const saved = localStorage.getItem('kpedal_locale');
		if (saved && locales.includes(saved as Locale)) {
			return saved;
		}
	}
	const detected = getLocaleFromNavigator()?.split('-')[0] || 'en';
	return locales.includes(detected as Locale) ? detected : 'en';
}

// Initialize i18n
init({
	fallbackLocale: 'en',
	initialLocale: getInitialLocale(),
	warnOnMissingMessages: true,
	handleMissingMessage: ({ locale: loc, id }) => {
		if (browser && import.meta.env.DEV) {
			console.warn(`[i18n] Missing translation: [${loc}] ${id}`);
		}
		return id;
	}
});

// Set locale and persist
export function setLocale(newLocale: Locale) {
	locale.set(newLocale);
	if (browser) {
		localStorage.setItem('kpedal_locale', newLocale);
	}
}

// Export utilities
export { locale, waitLocale };
export { _, t, format, time, date, number } from 'svelte-i18n';
