import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import zhCN from './locales/zh-CN.json';
import jaJP from './locales/ja-JP.json';
import enUS from './locales/en-US.json';

/**
 * 支持的语言列表。
 */
export const SUPPORTED_LANGS = [
  { key: 'zh-CN', label: '中文' },
  { key: 'ja-JP', label: '日本語' },
  { key: 'en-US', label: 'English' },
] as const;

export type SupportedLang = (typeof SUPPORTED_LANGS)[number]['key'];

/**
 * 获取浏览器语言偏好，回退到 zh-CN。
 */
function detectLanguage(): string {
  const stored = localStorage.getItem('lang');
  if (stored && SUPPORTED_LANGS.some((l) => l.key === stored)) {
    return stored;
  }
  const browserLang = navigator.language;
  if (browserLang.startsWith('ja')) return 'ja-JP';
  if (browserLang.startsWith('en')) return 'en-US';
  return 'zh-CN';
}

i18n.use(initReactI18next).init({
  resources: {
    'zh-CN': { translation: zhCN },
    'ja-JP': { translation: jaJP },
    'en-US': { translation: enUS },
  },
  lng: detectLanguage(),
  fallbackLng: 'zh-CN',
  interpolation: {
    escapeValue: false, // React already escapes
  },
});

export default i18n;
