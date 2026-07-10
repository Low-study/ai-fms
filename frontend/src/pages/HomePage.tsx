import { useState, useEffect } from 'react';
import { Card, Col, Row, Statistic, Button, Space, Skeleton, Tag } from 'antd';
import {
  UserOutlined,
  SafetyOutlined,
  BankOutlined,
  TeamOutlined,
  PlusOutlined,
  ArrowRightOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { userApi } from '../api/userApi';

interface StatItem {
  key: string;
  title: string;
  value: number | null;
  icon: React.ReactNode;
  colorClass: string;
  loading: boolean;
  comingSoon: boolean;
}

export default function HomePage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [userCount, setUserCount] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    userApi
      .list({ keyword: '', page: 1, size: 1 })
      .then((res) => setUserCount(res.data.total))
      .catch(() => setUserCount(null))
      .finally(() => setLoading(false));
  }, []);

  const stats: StatItem[] = [
    {
      key: 'users',
      title: t('dashboard.totalUsers'),
      value: userCount,
      icon: <UserOutlined />,
      colorClass: 'stat-icon--blue',
      loading,
      comingSoon: false,
    },
    {
      key: 'roles',
      title: t('dashboard.activeRoles'),
      value: 0,
      icon: <SafetyOutlined />,
      colorClass: 'stat-icon--green',
      loading: false,
      comingSoon: true,
    },
    {
      key: 'tenants',
      title: t('dashboard.tenants'),
      value: 0,
      icon: <BankOutlined />,
      colorClass: 'stat-icon--purple',
      loading: false,
      comingSoon: true,
    },
    {
      key: 'teams',
      title: t('dashboard.teams'),
      value: 0,
      icon: <TeamOutlined />,
      colorClass: 'stat-icon--orange',
      loading: false,
      comingSoon: true,
    },
  ];

  return (
    <div>
      {/* 页头 */}
      <div className="page-header">
        <h2 className="page-title">{t('dashboard.title')}</h2>
        <p style={{ color: '#8c8c8c', margin: 0, fontSize: 14 }}>{t('dashboard.subtitle')}</p>
      </div>

      {/* 统计卡片行 */}
      <Row gutter={[16, 16]}>
        {stats.map((stat) => (
          <Col xs={24} sm={12} lg={6} key={stat.key}>
            <Card className="stat-card" bodyStyle={{ padding: '20px 24px' }}>
              <div style={{ display: 'flex', alignItems: 'flex-start', gap: 16 }}>
                <div
                  className={`stat-icon ${stat.colorClass}`}
                  style={stat.comingSoon ? { opacity: 0.65 } : undefined}
                >
                  {stat.icon}
                </div>
                <div style={{ flex: 1 }}>
                  {stat.loading ? (
                    <Skeleton.Input active size="small" style={{ width: 80, marginTop: 4 }} />
                  ) : (
                    <>
                      <Statistic
                        title={stat.title}
                        value={stat.value ?? '-'}
                        valueStyle={{ fontSize: 28, fontWeight: 700 }}
                      />
                      {stat.comingSoon && (
                        <Tag
                          color="default"
                          style={{ marginTop: 4, fontSize: 11, lineHeight: '18px' }}
                        >
                          {t('dashboard.comingSoon')}
                        </Tag>
                      )}
                    </>
                  )}
                </div>
              </div>
            </Card>
          </Col>
        ))}
      </Row>

      {/* 欢迎卡片 + 快捷操作 */}
      <Card className="welcome-card" style={{ marginTop: 20 }}>
        <Row align="middle" justify="space-between">
          <Col xs={24} md={16}>
            <h3 className="welcome-title">{t('dashboard.welcome')}</h3>
            <p className="welcome-desc">{t('dashboard.description')}</p>
          </Col>
          <Col xs={24} md={8}>
            <Space direction="vertical" style={{ width: '100%' }} size={12}>
              <Button
                type="primary"
                icon={<ArrowRightOutlined />}
                block
                onClick={() => navigate('/users')}
                style={{ height: 40 }}
              >
                {t('dashboard.viewUsers')}
              </Button>
              <Button
                icon={<PlusOutlined />}
                block
                onClick={() => navigate('/users/create')}
                style={{ height: 40 }}
              >
                {t('dashboard.createUser')}
              </Button>
            </Space>
          </Col>
        </Row>
      </Card>
    </div>
  );
}
