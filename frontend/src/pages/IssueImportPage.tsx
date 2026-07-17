import { useState, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Upload,
  Button,
  Card,
  Result,
  Spin,
  Typography,
  Space,
} from 'antd';
import type { UploadProps } from 'antd';
import { InboxOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import PageContainer from '../components/PageContainer';
import SseProgress from '../components/SseProgress';
import { issueApi } from '../api/issueApi';
import type { ImportTicketResponse } from '../api/issueApi';

const { Dragger } = Upload;
const { Text, Paragraph } = Typography;

type ImportState = 'idle' | 'uploading' | 'processing' | 'complete' | 'error';

export default function IssueImportPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const [importState, setImportState] = useState<ImportState>('idle');
  const [issueId, setIssueId] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [uploadingFileName, setUploadingFileName] = useState<string | null>(null);

  const eventSourceRef = useRef<EventSource | null>(null);

  const closeEventSource = useCallback(() => {
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
      eventSourceRef.current = null;
    }
  }, []);

  const startProcessing = useCallback(
    (ticketResponse: ImportTicketResponse) => {
      closeEventSource();

      setImportState('processing');
      setErrorMessage(null);

      const es = new EventSource(
        `/api/v1/agents/import/stream?ticket=${ticketResponse.ticketId}`,
      );
      eventSourceRef.current = es;
    },
    [closeEventSource],
  );

  const handleComplete = useCallback(
    (issueIdFromEvent?: string) => {
      closeEventSource();

      // Try to extract issueId from SSE done event data
      const resolvedIssueId = issueIdFromEvent ?? 'unknown';
      setIssueId(resolvedIssueId);
      setImportState('complete');
    },
    [closeEventSource],
  );

  const handleError = useCallback(
    (errMsg: string) => {
      closeEventSource();
      setErrorMessage(errMsg);
      setImportState('error');
    },
    [closeEventSource],
  );

  const handleRetry = useCallback(() => {
    setImportState('idle');
    setIssueId(null);
    setErrorMessage(null);
    setUploadingFileName(null);
  }, []);

  /**
   * Prevent default Ant Design upload — we handle it manually via issueApi.
   */
  const beforeUpload: UploadProps['beforeUpload'] = useCallback(
    async (file: File) => {
      setUploadingFileName(file.name);
      setImportState('uploading');
      setErrorMessage(null);

      try {
        const ticketResponse = await issueApi.import(file);
        startProcessing(ticketResponse);
      } catch (err: unknown) {
        const msg =
          err instanceof Error ? err.message : t('issue.import.error');
        setErrorMessage(msg);
        setImportState('error');
      }

      // Always return false to prevent Ant Design default upload
      return Upload.LIST_IGNORE;
    },
    [startProcessing, t],
  );

  // ── Render: Loading / Uploading ──
  if (importState === 'uploading') {
    return (
      <PageContainer title={t('issue.import.title')} subtitle={t('issue.import.subtitle')}>
        <Card>
          <div style={{ textAlign: 'center', padding: '60px 0' }}>
            <Spin size="large" />
            <Paragraph style={{ marginTop: 24 }}>
              <Text type="secondary">
                {t('common.loading')}
              </Text>
            </Paragraph>
            {uploadingFileName && (
              <Text type="secondary" style={{ fontSize: 12 }}>
                {uploadingFileName}
              </Text>
            )}
          </div>
        </Card>
      </PageContainer>
    );
  }

  // ── Render: SSE Processing ──
  if (importState === 'processing' && eventSourceRef.current) {
    return (
      <PageContainer title={t('issue.import.title')} subtitle={t('issue.import.subtitle')}>
        <Card title={t('issue.import.processing')}>
          <SseProgress
            eventSource={eventSourceRef.current}
            onComplete={() => handleComplete()}
            onError={(err) => handleError(err)}
            onRetry={handleRetry}
          />
        </Card>
      </PageContainer>
    );
  }

  // ── Render: Error ──
  if (importState === 'error') {
    return (
      <PageContainer title={t('issue.import.title')} subtitle={t('issue.import.subtitle')}>
        <Card>
          <Result
            status="error"
            title={t('issue.import.error')}
            subTitle={errorMessage ?? t('error.unknown')}
            extra={[
              <Space key="actions">
                <Button type="primary" onClick={handleRetry}>
                  {t('issue.import.retry')}
                </Button>
                <Button onClick={() => navigate('/')}>
                  {t('common.back')}
                </Button>
              </Space>,
            ]}
          />
        </Card>
      </PageContainer>
    );
  }

  // ── Render: Complete ──
  if (importState === 'complete') {
    return (
      <PageContainer title={t('issue.import.title')} subtitle={t('issue.import.subtitle')}>
        <Card>
          <Result
            status="success"
            title={t('issue.import.complete')}
            subTitle={uploadingFileName ? `${uploadingFileName}` : undefined}
            extra={[
              <Space key="actions">
                <Button
                  type="primary"
                  onClick={() => navigate(`/issues/${issueId}`)}
                >
                  {t('issue.import.viewDetail')}
                </Button>
                <Button onClick={handleRetry}>
                  {t('issue.import.retry')}
                </Button>
              </Space>,
            ]}
          />
        </Card>
      </PageContainer>
    );
  }

  // ── Render: Idle / Empty — Upload area ──
  return (
    <PageContainer
      title={t('issue.import.title')}
      subtitle={t('issue.import.subtitle')}
    >
      <Card>
        <div style={{ padding: '20px 0' }}>
          <Dragger
            name="file"
            multiple={false}
            accept=".txt,.pdf,.png,.jpg"
            beforeUpload={beforeUpload}
            showUploadList={false}
          >
            <p className="ant-upload-drag-icon">
              <InboxOutlined />
            </p>
            <p className="ant-upload-text">{t('issue.import.uploadHint')}</p>
            <p className="ant-upload-hint">{t('issue.import.uploadFormat')}</p>
          </Dragger>
        </div>
      </Card>
    </PageContainer>
  );
}
