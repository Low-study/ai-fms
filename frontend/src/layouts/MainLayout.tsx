import { useState, useMemo } from 'react';
import { Outlet, useNavigate, useLocation, Link } from 'react-router-dom';
import { Layout, Menu, Button, Avatar, Breadcrumb, Dropdown, Space, Radio, Tooltip, message } from 'antd';
import type { MenuProps } from 'antd';
import {
  DashboardOutlined,
  UserOutlined,
  SafetyOutlined,
  BankOutlined,
  TeamOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  GlobalOutlined,
  LogoutOutlined,
  SettingOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { SUPPORTED_LANGS, type SupportedLang } from '../i18n';

const { Header, Sider, Content, Footer } = Layout;

type MenuItem = Required<MenuProps>['items'][number];

export default function MainLayout() {
  const { t, i18n } = useTranslation();
  const [collapsed, setCollapsed] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  /** 菜单项 */
  const menuItems: MenuItem[] = [
    { key: '/', icon: <DashboardOutlined />, label: t('menu.dashboard') },
    { key: '/users', icon: <UserOutlined />, label: t('menu.users') },
    { key: '/roles', icon: <SafetyOutlined />, label: t('menu.roles'), disabled: true },
    { key: '/tenants', icon: <BankOutlined />, label: t('menu.tenants'), disabled: true },
    { key: '/teams', icon: <TeamOutlined />, label: t('menu.teams'), disabled: true },
  ];

  /** 根据当前路径动态生成面包屑 */
  const breadcrumbItems = useMemo(() => {
    const segments = location.pathname.split('/').filter(Boolean);
    const items: { title: React.ReactNode }[] = [{ title: <Link to="/">{t('breadcrumb.home')}</Link> }];

    if (segments.length >= 1 && segments[0] === 'users') {
      items.push({
        title:
          segments.length === 1 ? (
            t('breadcrumb.users')
          ) : (
            <Link to="/users">{t('breadcrumb.users')}</Link>
          ),
      });

      if (segments[1] === 'create') {
        items.push({ title: t('breadcrumb.createUser') });
      } else if (segments[1] && segments[2] === 'edit') {
        items.push({ title: t('breadcrumb.editUser') });
      }
    }

    return items;
  }, [location.pathname, t]);

  /** 切换语言 */
  const handleLangChange = (lang: SupportedLang) => {
    i18n.changeLanguage(lang);
    localStorage.setItem('lang', lang);
    window.location.reload();
  };

  /** 用户下拉菜单 — logout 可点击但提示开发中，profile 禁用 */
  const userMenuItems: MenuProps['items'] = [
    {
      key: 'profile',
      icon: <SettingOutlined />,
      label: t('header.profile'),
      disabled: true,
    },
    { type: 'divider' },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: t('header.logout'),
      onClick: () => message.info(t('header.logoutHint')),
    },
  ];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      {/* ── 侧边栏 ── */}
      <Sider
        trigger={null}
        collapsible
        collapsed={collapsed}
        width={240}
        style={{
          position: 'fixed',
          left: 0,
          top: 0,
          bottom: 0,
          zIndex: 100,
          overflow: 'auto',
        }}
      >
        {/* Logo */}
        <div className={`sider-logo${collapsed ? ' sider-logo--collapsed' : ''}`}>
          <SafetyOutlined className="logo-icon" />
          {!collapsed && <span className="logo-text">AI-FMS</span>}
        </div>

        {/* 导航菜单 */}
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          defaultOpenKeys={['/users']}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
          style={{ borderInlineEnd: 'none' }}
        />

        {/* 底部语言切换 */}
        <div className="sider-footer">
          <Tooltip title={t('header.switchLanguage')} placement="right">
            <GlobalOutlined className="lang-icon" />
          </Tooltip>
          {!collapsed && (
            <Radio.Group
              value={i18n.language as SupportedLang}
              onChange={(e) => handleLangChange(e.target.value as SupportedLang)}
              size="small"
              optionType="button"
              buttonStyle="solid"
              style={{ flex: 1 }}
            >
              {SUPPORTED_LANGS.map((l) => (
                <Radio.Button key={l.key} value={l.key} style={{ padding: '0 8px', fontSize: 12 }}>
                  {l.key === 'zh-CN' ? 'ZH' : l.key === 'ja-JP' ? 'JA' : 'EN'}
                </Radio.Button>
              ))}
            </Radio.Group>
          )}
        </div>
      </Sider>

      {/* ── 主内容区 ── */}
      <Layout style={{ marginLeft: collapsed ? 80 : 240, transition: 'margin-left 0.2s' }}>
        {/* 顶部栏 */}
        <Header className="app-header" style={{ position: 'sticky', top: 0 }}>
          <div className="app-header-left">
            <Button
              type="text"
              icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
              onClick={() => setCollapsed(!collapsed)}
              style={{ fontSize: 16, width: 40, height: 40 }}
            />

            {/* 面包屑 */}
            <span className="hide-on-mobile">
              <Breadcrumb items={breadcrumbItems} />
            </span>
          </div>

          {/* 右侧：用户头像 + 下拉 */}
          <div className="app-header-right">
            <Dropdown menu={{ items: userMenuItems }} placement="bottomRight" trigger={['click']}>
              <Space style={{ cursor: 'pointer', padding: '4px 8px', borderRadius: 8 }}>
                <Avatar
                  size={32}
                  icon={<UserOutlined />}
                  style={{ backgroundColor: '#1677ff', flexShrink: 0 }}
                />
                <span className="hide-on-mobile" style={{ fontSize: 14, fontWeight: 500, color: '#1f1f1f' }}>
                  Admin
                </span>
              </Space>
            </Dropdown>
          </div>
        </Header>

        {/* 内容区 */}
        <Content className="app-content">
          <div className="page-fade-in page-container" key={location.pathname}>
            <Outlet />
          </div>
        </Content>

        {/* 底部 */}
        <Footer className="app-footer">{t('footer.copyright')}</Footer>
      </Layout>
    </Layout>
  );
}
