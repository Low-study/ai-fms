import { useState, useEffect } from 'react';
import { Card, Descriptions, Alert, Button, Skeleton, Empty, Typography, Space } from 'antd';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { findingApi, type SimilarItem } from '../api/findingApi';
import { ApiError } from '../api/client';
import StatusTag from '../components/StatusTag';
import type { Finding } from '../types/finding';

const { Title } = Typography;
const { Paragraph } = Typography;

export default function IssueDetailPage() {
  const { t } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [finding, setFinding] = useState<Finding | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [similarItems, setSimilarItems] = useState<SimilarItem[]>([]);

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
        // 加载相似案例
        return findingApi.getSimilar(id);
      })
      .then((similarRes) => {
        if (similarRes) setSimilarItems(similarRes.data.items ?? []);
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
            {finding.createdAt ? new Date(finding.createdAt).toLocaleString() : '-'}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      {/* Similar Cases */}
      <Card
        className="form-card"
        title={t('issue.detail.similarCases')}
        style={{ marginTop: 16 }}
      >
        {similarItems.length > 0 ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12, maxHeight: 320, overflowY: 'auto', paddingRight: 4 }}>
              {similarItems.map((item) => (
                <Card
                  key={item.id}
                  size="small"
                  hoverable
                  onClick={() => navigate(`/issues/${item.id}`)}
                  style={{ cursor: 'pointer' }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <strong>{item.title}</strong>
                    <span style={{
                      color: item.similarity >= 0.7 ? '#52c41a' : item.similarity >= 0.4 ? '#fa8c16' : '#999',
                      fontSize: 12,
                      fontWeight: 600,
                    }}>
                      {Math.round(item.similarity * 100)}%
                    </span>
                  </div>
                  {item.resolution && (
                    <Paragraph
                      type="secondary"
                      style={{ marginTop: 4, marginBottom: 0, fontSize: 13 }}
                      ellipsis={{ rows: 2 }}
                    >
                      {item.resolution}
                    </Paragraph>
                  )}
                </Card>
              ))}
            </div>
        ) : (
          <Empty description={t('issue.detail.noSimilarCases')} />
        )}
      </Card>

      {/* Processing Suggestion */}
      <Card
        className="form-card"
        title={t('issue.detail.suggestion')}
        style={{ marginTop: 16 }}
      >
        {finding.reportDraft ? (
          <div className="markdown-body" style={{ maxHeight: 400, overflow: 'auto' }}>
            <ReactMarkdown remarkPlugins={[remarkGfm]}>
              {finding.reportDraft}
            </ReactMarkdown>
          </div>
        ) : (
          <Empty description={t('issue.detail.noSuggestion')} />
        )}
      </Card>

      {/* QA Reply */}
      {finding.qaReply && (
        <Card
          className="form-card"
          title={t('issue.detail.qaReply')}
          style={{ marginTop: 16 }}
        >
          <div className="markdown-body" style={{ maxHeight: 300, overflow: 'auto' }}>
            <ReactMarkdown remarkPlugins={[remarkGfm]}>
              {finding.qaReply}
            </ReactMarkdown>
          </div>
        </Card>
      )}
    </div>
  );
}
