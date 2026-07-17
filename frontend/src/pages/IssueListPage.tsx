import { useState, useEffect, useCallback } from 'react';
import { Table, Button, Input, Space, Tag, Alert, Card, Skeleton } from 'antd';
import { ReloadOutlined, InboxOutlined, UploadOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import type { ColumnsType } from 'antd/es/table';
import { findingApi } from '../api/findingApi';
import type { Finding } from '../types/finding';
import { ApiError } from '../api/client';
import PageContainer from '../components/PageContainer';
import StatusTag from '../components/StatusTag';
import EmptyState from '../components/EmptyState';

const PRIORITY_COLOR_MAP: Record<string, string> = {
  '高': 'red',
  '中': 'orange',
  '低': 'blue',
};

export default function IssueListPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [items, setItems] = useState<Finding[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const [keyword, setKeyword] = useState('');
  const [searchText, setSearchText] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const i18nError = (err: unknown): string => {
    if (err instanceof ApiError) {
      return t(`error.${err.code}`, err.message);
    }
    return t('issue.list.error');
  };

  const fetchIssues = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await findingApi.list({ keyword: searchText, page, size });
      setItems(response.data.items);
      setTotal(response.data.total);
    } catch (err) {
      setError(i18nError(err));
    } finally {
      setLoading(false);
    }
  }, [page, size, searchText]);

  useEffect(() => {
    fetchIssues();
  }, [fetchIssues]);

  const handleSearch = () => {
    setPage(1);
    setSearchText(keyword.trim());
  };

  const columns: ColumnsType<Finding> = [
    {
      title: t('issue.list.columns.title'),
      dataIndex: 'title',
      key: 'title',
      width: 200,
      ellipsis: true,
      render: (text: string, record: Finding) => (
        <a
          onClick={(e) => {
            e.stopPropagation();
            navigate(`/issues/${record.id}`);
          }}
        >
          {text}
        </a>
      ),
    },
    {
      title: t('issue.list.columns.category'),
      dataIndex: 'category',
      key: 'category',
      width: 100,
      render: (v: string) => (v ? <Tag>{v}</Tag> : '-'),
    },
    {
      title: t('issue.list.columns.priority'),
      dataIndex: 'priority',
      key: 'priority',
      width: 80,
      render: (v: string) => <StatusTag status={v} colorMap={PRIORITY_COLOR_MAP} />,
    },
    {
      title: t('issue.list.columns.severity'),
      dataIndex: 'severity',
      key: 'severity',
      width: 80,
      render: (v: string) => v || '-',
    },
    {
      title: t('issue.list.columns.status'),
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (v: string) => <StatusTag status={v} />,
    },
    {
      title: t('issue.list.columns.system'),
      dataIndex: 'system',
      key: 'system',
      width: 100,
      render: (v: string) => v || '-',
    },
    {
      title: t('issue.list.columns.suggestion'),
      dataIndex: 'reportDraft',
      key: 'reportDraft',
      width: 200,
      ellipsis: true,
      render: (v: string) => (v ? `${v.substring(0, 40)}...` : '-'),
    },
    {
      title: t('issue.list.columns.createdAt'),
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 140,
      render: (v: string) => v ? new Date(v).toLocaleDateString() : '-',
    },
  ];

  // ── Loading state (initial load) ──
  if (loading && items.length === 0) {
    return (
      <PageContainer title={t('issue.list.title')}>
        <Card>
          <Skeleton active paragraph={{ rows: 8 }} />
        </Card>
      </PageContainer>
    );
  }

  // ── Error state (no data) ──
  if (error && !loading && items.length === 0) {
    return (
      <PageContainer title={t('issue.list.title')}>
        <Card>
          <Alert
            type="error"
            message={error}
            action={
              <Button onClick={fetchIssues} icon={<ReloadOutlined />}>
                {t('issue.list.retry')}
              </Button>
            }
          />
        </Card>
      </PageContainer>
    );
  }

  // ── Empty state ──
  if (!loading && !error && items.length === 0) {
    return (
      <PageContainer title={t('issue.list.title')}>
        <EmptyState
          icon={<InboxOutlined />}
          title={t('issue.list.empty')}
          actionText={t('issue.list.importButton')}
          onAction={() => navigate('/issues/import')}
        />
      </PageContainer>
    );
  }

  // ── Success state ──
  return (
    <PageContainer
      title={t('issue.list.title')}
      extra={
        <Button type="primary" icon={<UploadOutlined />} onClick={() => navigate('/issues/import')}>
          {t('issue.list.importButton')}
        </Button>
      }
    >
      <Card>
        <Space direction="vertical" style={{ width: '100%' }}>
          <Input.Search
            placeholder={t('issue.list.searchPlaceholder')}
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            onSearch={handleSearch}
            enterButton
            allowClear
            style={{ maxWidth: 400 }}
          />
          <Table<Finding>
            columns={columns}
            dataSource={items}
            rowKey="id"
            loading={loading && items.length > 0}
            pagination={{
              current: page,
              pageSize: size,
              total,
              showSizeChanger: true,
              pageSizeOptions: ['10', '20', '50'],
              onChange: (p, s) => {
                setPage(p);
                setSize(s);
              },
            }}
            onRow={(record) => ({
              onClick: () => navigate(`/issues/${record.id}`),
              style: { cursor: 'pointer' },
            })}
          />
        </Space>
      </Card>
    </PageContainer>
  );
}
