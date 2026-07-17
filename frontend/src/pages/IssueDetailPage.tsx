import { useState, useEffect } from 'react';
import { Card, Descriptions, Alert, Button, Skeleton, Empty, Typography, Space } from 'antd';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { findingApi } from '../api/findingApi';
import { ApiError } from '../api/client';
import StatusTag from '../components/StatusTag';
import type { Finding } from '../types/finding';

const { Title, Paragraph } = Typography;

export default function IssueDetailPage() {
  const { t } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [finding, setFinding] = useState<Finding | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const i18nError = (err: unknown, fallbackKey: string): string => {
    if (err instanceof ApiError) {
      return t(`error.${err.code}`, err.message);
    }
    return t(fallbackKey);
  };

  const fetchFinding = () => {
    if (!id) return;
    setLoading(true);
    setLoadError(null);
    findingApi
      .getById(id)
      .then((response) => {
        setFinding(response.data);
      })
      .catch((err: unknown) => {
        setLoadError(i18nError(err, 'issue.detail.error'));
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchFinding();
  }, [id]);

  // ── Loading ──
  if (loading) {
    return (
      <div>
        <div className="page-header">
          <h2 className="page-title">{t('issue.detail.loading')}</h2>
        </div>
        <Card className="form-card">
          <Skeleton active paragraph={{ rows: 8 }} />
        </Card>
      </div>
    );
  }

  // ── Not Found ──
  if (!finding && !loadError) {
    return (
      <div>
        <div className="page-header">
          <h2 className="page-title">{t('issue.detail.title')}</h2>
        </div>
        <Card className="form-card">
          <Empty description={t('issue.detail.notFound')} />
        </Card>
      </div>
    );
  }

  // ── Error ──
  if (loadError && !finding) {
    return (
      <div>
        <div className="page-header">
          <h2 className="page-title">{t('issue.detail.title')}</h2>
        </div>
        <Alert
          type="error"
          message={t('issue.detail.error')}
          description={loadError}
          showIcon
          action={
            <Space>
              <Button onClick={fetchFinding}>{t('issue.detail.retry')}</Button>
              <Button onClick={() => navigate(-1)}>{t('common.back')}</Button>
            </Space>
          }
        />
      </div>
    );
  }

  if (!finding) return null;

  // ── Success ──
  return (
    <div>
      <div className="page-header">
        <h2 className="page-title">{t('issue.detail.title')}</h2>
      </div>

      {/* Basic Info */}
      <Card className="form-card" title={t('issue.detail.basicInfo')}>
        <Title level={3}>{finding.title}</Title>
        <Paragraph>{finding.description}</Paragraph>

        <Descriptions bordered column={{ xs: 1, sm: 2 }} style={{ marginTop: 24 }}>
          <Descriptions.Item label={t('issue.detail.field.category')}>
            {finding.category}
          </Descriptions.Item>
          <Descriptions.Item label={t('issue.detail.field.priority')}>
            <StatusTag status={finding.priority} />
          </Descriptions.Item>
          <Descriptions.Item label={t('issue.detail.field.severity')}>
            {finding.severity}
          </Descriptions.Item>
          <Descriptions.Item label={t('issue.detail.field.system')}>
            {finding.system}
          </Descriptions.Item>
          <Descriptions.Item label={t('issue.detail.field.assignee')}>
            {finding.assignee || '-'}
          </Descriptions.Item>
          <Descriptions.Item label={t('issue.detail.field.tags')}>
            {finding.tags || '-'}
          </Descriptions.Item>
          <Descriptions.Item label={t('issue.detail.field.status')}>
            <StatusTag status={finding.status} />
          </Descriptions.Item>
          <Descriptions.Item label={t('issue.detail.field.createdAt')}>
            {finding.createdAt}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      {/* Similar Cases */}
      <Card
        className="form-card"
        title={t('issue.detail.similarCases')}
        style={{ marginTop: 16 }}
      >
        <Empty description={t('issue.detail.noSimilarCases')} />
      </Card>

      {/* Processing Suggestion */}
      <Card
        className="form-card"
        title={t('issue.detail.suggestion')}
        style={{ marginTop: 16 }}
      >
        {finding.reportDraft ? (
          <Paragraph>{finding.reportDraft}</Paragraph>
        ) : (
          <Empty description={t('issue.detail.noSuggestion')} />
        )}
      </Card>
    </div>
  );
}
