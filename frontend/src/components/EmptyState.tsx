import type { ReactNode } from 'react';
import { Result, Button } from 'antd';
import { InboxOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';

export interface EmptyStateProps {
  icon?: ReactNode;
  title?: string;
  description?: string;
  actionText?: string;
  onAction?: () => void;
}

export default function EmptyState({
  icon,
  title,
  description,
  actionText,
  onAction,
}: EmptyStateProps) {
  const { t } = useTranslation();

  return (
    <Result
      icon={icon ?? <InboxOutlined />}
      title={title ?? t('component.empty.title')}
      subTitle={description ?? t('component.empty.description')}
      extra={
        onAction ? (
          <Button type="primary" onClick={onAction} size="large">
            {actionText ?? t('common.create')}
          </Button>
        ) : undefined
      }
    />
  );
}
