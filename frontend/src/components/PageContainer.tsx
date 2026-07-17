import type { ReactNode } from 'react';
import { Typography, Space, Breadcrumb } from 'antd';
import { HomeOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

const { Title, Text } = Typography;

export interface BreadcrumbItem {
  title: string;
  path?: string;
}

export interface PageContainerProps {
  title: string;
  subtitle?: string;
  extra?: ReactNode;
  breadcrumb?: BreadcrumbItem[];
  children: ReactNode;
}

export default function PageContainer({ title, subtitle, extra, breadcrumb, children }: PageContainerProps) {
  const navigate = useNavigate();
  const { t } = useTranslation();

  const breadcrumbItems = [
    {
      title: (
        <Space size={4}>
          <HomeOutlined />
          <span>{t('breadcrumb.home')}</span>
        </Space>
      ),
      ...(breadcrumb && breadcrumb.length > 0 ? {} : { onClick: undefined }),
    },
    ...(breadcrumb ?? []).map((item) => ({
      title: <span>{item.title}</span>,
      ...(item.path ? { onClick: () => navigate(item.path!) } : {}),
    })),
  ];

  return (
    <div>
      {breadcrumb && breadcrumb.length > 0 && (
        <Breadcrumb
          items={breadcrumbItems}
          style={{ marginBottom: 16 }}
        />
      )}

      <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <Title level={2} className="page-title" style={{ margin: 0 }}>
            {title}
          </Title>
          {subtitle && (
            <Text type="secondary" style={{ display: 'block', marginTop: 4 }}>
              {subtitle}
            </Text>
          )}
        </div>
        {extra && <div>{extra}</div>}
      </div>

      {children}
    </div>
  );
}
