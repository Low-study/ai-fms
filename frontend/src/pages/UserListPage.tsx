import { useState, useEffect, useCallback } from 'react';
import { Table, Button, Input, Space, Tag, Popconfirm, Alert, Card, Empty } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import type { ColumnsType } from 'antd/es/table';
import { userApi } from '../api/userApi';
import type { User } from '../types/user';
import { ApiError } from '../api/client';

const STATUS_COLOR: Record<string, string> = {
  ACTIVE: 'green',
  LOCKED: 'red',
  DISABLED: 'orange',
  DELETED: 'default',
};

export default function UserListPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [users, setUsers] = useState<User[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const [keyword, setKeyword] = useState('');
  const [searchText, setSearchText] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [deleting, setDeleting] = useState<string | null>(null);

  const i18nError = (err: unknown): string => {
    if (err instanceof ApiError) {
      return t(`error.${err.code}`, err.message);
    }
    return t('error.loadFailed');
  };

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await userApi.list({ keyword: searchText, page, size });
      setUsers(response.data.items);
      setTotal(response.data.total);
    } catch (err) {
      setError(i18nError(err));
    } finally {
      setLoading(false);
    }
  }, [page, size, searchText]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const handleSearch = () => {
    setPage(1);
    setSearchText(keyword.trim());
  };

  const handleDelete = async (id: string) => {
    setDeleting(id);
    try {
      await userApi.delete(id);
      fetchUsers();
    } catch {
      // error handled by interceptor
    } finally {
      setDeleting(null);
    }
  };

  const columns: ColumnsType<User> = [
    {
      title: t('user.username'),
      dataIndex: 'username',
      key: 'username',
      width: 140,
      ellipsis: true,
    },
    {
      title: t('user.email'),
      dataIndex: 'email',
      key: 'email',
      width: 240,
      ellipsis: true,
    },
    {
      title: t('user.displayName'),
      dataIndex: 'displayName',
      key: 'displayName',
      width: 140,
      ellipsis: true,
      render: (v) => v || <span style={{ color: '#bfbfbf' }}>-</span>,
    },
    {
      title: t('user.status'),
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => (
        <Tag color={STATUS_COLOR[status] || 'default'}>{t(`userStatus.${status}`, status)}</Tag>
      ),
    },
    {
      title: t('user.created'),
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (v: string) => (v ? new Date(v).toLocaleString() : '-'),
    },
    {
      title: t('user.actions'),
      key: 'actions',
      width: 160,
      fixed: 'right',
      render: (_: unknown, record: User) => (
        <Space size={4}>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => navigate(`/users/${record.id}/edit`)}
          >
            {t('common.edit')}
          </Button>
          <Popconfirm
            title={t('user.deleteConfirm')}
            description={t('user.deleteDesc')}
            onConfirm={() => handleDelete(record.id)}
            okText={t('common.delete')}
            cancelText={t('common.cancel')}
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />} loading={deleting === record.id}>
              {t('common.delete')}
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  // ── Error state (no data) ──
  if (error && users.length === 0) {
    return (
      <div>
        <div className="page-header">
          <h2 className="page-title">{t('user.title')}</h2>
        </div>
        <Alert
          type="error"
          message={t('error.loadFailed')}
          description={error}
          showIcon
          action={
            <Button onClick={fetchUsers} icon={<ReloadOutlined />} size="small">
              {t('common.retry')}
            </Button>
          }
        />
      </div>
    );
  }

  return (
    <div>
      {/* 页头 */}
      <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <h2 className="page-title">{t('user.title')}</h2>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/users/create')} size="large">
          {t('user.createUser')}
        </Button>
      </div>

      {/* 搜索 + 表格卡片 */}
      <Card bodyStyle={{ padding: 20 }}>
        {error && (
          <Alert
            type="error"
            message={error}
            closable
            onClose={() => setError(null)}
            style={{ marginBottom: 16 }}
            showIcon
          />
        )}

        <div className="search-bar" style={{ marginBottom: 16 }}>
          <Input.Search
            placeholder={t('user.searchPlaceholder')}
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            onSearch={handleSearch}
            onPressEnter={handleSearch}
            style={{ maxWidth: 400 }}
            allowClear
            loading={loading}
            size="middle"
          />
        </div>

        <Table<User>
          columns={columns}
          dataSource={users}
          rowKey="id"
          loading={loading}
          size="middle"
          locale={{
            emptyText: (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description={
                  <span>
                    <p style={{ marginBottom: 4 }}>{t('user.noUsers')}</p>
                    <p style={{ color: '#8c8c8c', fontSize: 13, margin: 0 }}>
                      {t('user.noUsersDesc')}
                    </p>
                  </span>
                }
              />
            ),
          }}
          pagination={{
            current: page,
            pageSize: size,
            total,
            showSizeChanger: true,
            showQuickJumper: true,
            pageSizeOptions: ['10', '20', '50'],
            showTotal: (n) => t('user.userCount', { count: n }),
            onChange: (p, s) => {
              setPage(p);
              setSize(s);
            },
          }}
          scroll={{ x: 860 }}
        />
      </Card>
    </div>
  );
}
