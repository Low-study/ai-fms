import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import jaJP from 'antd/locale/ja_JP';
import enUS from 'antd/locale/en_US';
import App from './App';
import './i18n'; // 初始化 i18n（必须在组件渲染前）
import './styles/global.css';

/** 同步 Ant Design 语言包与 i18n 语言。 */
const antdLocales: Record<string, typeof zhCN> = {
  'zh-CN': zhCN,
  'ja-JP': jaJP,
  'en-US': enUS,
};

const currentLang = localStorage.getItem('lang') || navigator.language;
const antdLocale = antdLocales[currentLang] || zhCN;

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ConfigProvider
      locale={antdLocale}
      theme={{
        token: {
          colorPrimary: '#1677ff',
          borderRadius: 8,
          borderRadiusLG: 12,
          fontFamily:
            '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, "Noto Sans SC", "Noto Sans JP", sans-serif',
          fontSize: 14,
          lineHeight: 1.6,
        },
        components: {
          Layout: {
            headerBg: '#fff',
            siderBg: '#001529',
          },
          Menu: {
            darkItemBg: '#001529',
            darkItemSelectedBg: '#1677ff',
            darkSubMenuItemBg: '#000c17',
          },
          Card: {
            paddingLG: 24,
          },
          Table: {
            headerBg: '#fafafa',
          },
        },
      }}
    >
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </ConfigProvider>
  </React.StrictMode>,
);
