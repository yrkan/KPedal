import { browser } from '$app/environment';
import { init, register, getLocaleFromNavigator, locale, waitLocale } from 'svelte-i18n';

// Available locales
export const locales = ['en', 'uk', 'ru', 'de', 'es', 'fr', 'it', 'pt', 'nl', 'ja', 'zh', 'he', 'ar', 'pl', 'ko', 'da', 'sv'] as const;
export type Locale = (typeof locales)[number];

export const localeNames: Record<Locale, string> = {
	en: 'English',
	uk: 'Українська',
	ru: 'Русский',
	de: 'Deutsch',
	es: 'Español',
	fr: 'Français',
	it: 'Italiano',
	pt: 'Português',
	nl: 'Nederlands',
	ja: '日本語',
	zh: '中文',
	he: 'עברית',
	ar: 'العربية',
	pl: 'Polski',
	ko: '한국어',
	da: 'Dansk',
	sv: 'Svenska'
};

// Register locales with dynamic imports
register('en', () => import('../locales/en.json'));
register('uk', () => import('../locales/uk.json'));
register('ru', () => import('../locales/ru.json'));
register('de', () => import('../locales/de.json'));
register('es', () => import('../locales/es.json'));
register('fr', () => import('../locales/fr.json'));
register('it', () => import('../locales/it.json'));
register('pt', () => import('../locales/pt.json'));
register('nl', () => import('../locales/nl.json'));
register('ja', () => import('../locales/ja.json'));
register('zh', () => import('../locales/zh.json'));
register('he', () => import('../locales/he.json'));
register('ar', () => import('../locales/ar.json'));
register('pl', () => import('../locales/pl.json'));
register('ko', () => import('../locales/ko.json'));
register('da', () => import('../locales/da.json'));
register('sv', () => import('../locales/sv.json'));

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
