import { Table, Spin, Empty, Alert, Button, Typography } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import type { TableRowSelection } from 'antd/es/table/interface';

const { Text } = Typography;

export interface DataTablePagination {
  current: number;
  pageSize: number;
  total: number;
  onChange: (page: number, size: number) => void;
}

export interface DataTableProps<T extends object> {
  columns: ColumnsType<T>;
  dataSource: T[];
  rowKey?: string | ((record: T) => string);
  loading?: boolean;
  error?: string | null;
  emptyText?: string;
  errorText?: string;
  onRetry?: () => void;
  pagination?: DataTablePagination;
  rowSelection?: TableRowSelection<T>;
  scroll?: { x?: number | string; y?: number | string };
}

export default function DataTable<T extends object>({
  columns,
  dataSource,
  rowKey = 'id' as string,
  loading = false,
  error = null,
  emptyText,
  errorText,
  onRetry,
  pagination,
  rowSelection,
  scroll,
}: DataTableProps<T>) {
  const { t } = useTranslation();

  const paginationConfig: TablePaginationConfig | false = pagination
    ? {
        current: pagination.current,
        pageSize: pagination.pageSize,
        total: pagination.total,
        showSizeChanger: true,
        showQuickJumper: true,
        pageSizeOptions: ['10', '20', '50'],
        showTotal: (total: number) => t('user.userCount', { count: total }),
        onChange: pagination.onChange,
      }
    : false;

  const emptyRender = () => (
    <Empty
      image={Empty.PRESENTED_IMAGE_SIMPLE}
      description={
        <span>
          <Text style={{ display: 'block', marginBottom: 4 }}>
            {emptyText ?? t('component.table.empty')}
          </Text>
        </span>
      }
    />
  );

  if (loading && dataSource.length === 0) {
    return (
      <div style={{ padding: '60px 0', textAlign: 'center' }}>
        <Spin size="large" tip={t('component.table.loading')}>
          <div style={{ minHeight: 200 }} />
        </Spin>
      </div>
    );
  }

  if (error && dataSource.length === 0) {
    return (
      <Alert
        type="error"
        message={errorText ?? t('component.table.error')}
        description={error}
        showIcon
        action={
          onRetry ? (
            <Button onClick={onRetry} icon={<ReloadOutlined />} size="small">
              {t('component.error.retry')}
            </Button>
          ) : undefined
        }
      />
    );
  }

  return (
    <>
      {error && dataSource.length > 0 && (
        <Alert
          type="error"
          message={error}
          closable
          showIcon
          style={{ marginBottom: 16 }}
        />
      )}

      <Table<T>
        columns={columns}
        dataSource={dataSource}
        rowKey={rowKey}
        loading={loading && dataSource.length > 0}
        size="middle"
        pagination={paginationConfig}
        rowSelection={rowSelection}
        locale={{ emptyText: emptyRender() }}
        scroll={scroll}
      />
    </>
  );
}
