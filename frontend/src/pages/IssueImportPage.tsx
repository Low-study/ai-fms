import { useState, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Upload,
  Button,
  Card,
  Result,
  Typography,
  Space,
  Steps,
} from 'antd';
import type { UploadProps } from 'antd';
import { InboxOutlined, LoadingOutlined, CheckCircleOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import PageContainer from '../components/PageContainer';
import { issueApi } from '../api/issueApi';

const { Dragger } = Upload;
const { Paragraph } = Typography;

type ImportState = 'idle' | 'uploading' | 'processing' | 'complete' | 'error';

interface StepInfo {
  key: string;
  label: string;
  status: 'wait' | 'process' | 'finish' | 'error';
}

const STEP_ORDER = ['ingest', 'rag', 'reportQa'];

export default function IssueImportPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const [importState, setImportState] = useState<ImportState>('idle');
  const [uploadingFileName, setUploadingFileName] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [steps, setSteps] = useState<StepInfo[]>([
    { key: 'ingest', label: t('component.sse.step.parse'), status: 'wait' },
    { key: 'rag', label: t('component.sse.step.rag'), status: 'wait' },
    { key: 'reportQa', label: t('component.sse.step.report'), status: 'wait' },
  ]);
  const [resultIssueId, setResultIssueId] = useState<string | null>(null);
  const abortRef = useRef<AbortController | null>(null);

  const updateStep = useCallback((stepName: string, percentage: number) => {
    setSteps((prev) => {
      const normalizedName = stepName === 'reportQa' ? 'reportQa' : (stepName === 'rag' ? 'rag' : 'ingest');
      const idx = STEP_ORDER.indexOf(normalizedName);
      if (idx < 0) return prev;
      return prev.map((s, i) => {
        if (i < idx) return { ...s, status: 'finish' as const };
        if (i === idx) {
          if (percentage >= 80 || (stepName === 'ingest' && percentage >= 40)) return { ...s, status: 'finish' as const };
          return { ...s, status: 'process' as const };
        }
        return s;
      });
    });
  }, []);

  const handleRetry = useCallback(() => {
    if (abortRef.current) abortRef.current.abort();
    setImportState('idle');
    setUploadingFileName(null);
    setErrorMessage(null);
    setResultIssueId(null);
    setSteps((prev) => prev.map((s) => ({ ...s, status: 'wait' as const })));
  }, []);

  const startImport = useCallback(
    async (file: File) => {
      setUploadingFileName(file.name);
      setImportState('uploading');
      setErrorMessage(null);

      try {
        const reader = await issueApi.importStream(file);
        setImportState('processing');

        let latestIssueId: string | null = null;
        for await (const event of issueApi.parseSseStream(reader)) {
          try {
            const payload = JSON.parse(event.data);
            const stepName: string = payload.stepName || payload.step_name || event.type;

            if (stepName === 'done') {
              latestIssueId = payload.issueId || payload.findingId;
              setResultIssueId(latestIssueId);
              updateStep('reportQa', 100);
              setImportState('complete');
              return;
            }

            if (stepName === 'error' || event.type === 'error') {
              throw new Error(payload.message || 'Pipeline error');
            }

            updateStep(event.type || stepName, payload.percentage || 0);
          } catch (parseErr) {
            // Skip unparseable events
          }
        }

        // Stream ended without done event — still complete if no error
        setImportState('complete');
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : t('issue.import.error');
        setErrorMessage(msg);
        setImportState('error');
      }
    },
    [t, updateStep, importState],
  );

  const beforeUpload: UploadProps['beforeUpload'] = useCallback(
    (file: File) => {
      startImport(file);
      return Upload.LIST_IGNORE;
    },
    [startImport],
  );

  // ── Render: Uploading / Processing ──
  if (importState === 'uploading' || importState === 'processing') {
    return (
      <PageContainer title={t('issue.import.title')} subtitle={t('issue.import.subtitle')}>
        <Card title={t('issue.import.processing')}>
          <div style={{ textAlign: 'center', padding: '20px 0' }}>
            {uploadingFileName && (
              <Paragraph type="secondary" style={{ marginBottom: 24 }}>
                {uploadingFileName}
              </Paragraph>
            )}
            <Steps
              direction="vertical"
              current={steps.findIndex((s) => s.status === 'process')}
              items={steps.map((s) => ({
                title: s.label,
                status: s.status === 'finish' ? 'finish' : s.status === 'process' ? 'process' : 'wait',
                icon: s.status === 'finish' ? <CheckCircleOutlined /> : s.status === 'process' ? <LoadingOutlined /> : undefined,
              }))}
            />
          </div>
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
            subTitle={uploadingFileName ?? undefined}
            extra={[
              <Space key="actions">
                <Button
                  type="primary"
                  onClick={() => navigate(resultIssueId ? `/issues/${resultIssueId}` : '/issues')}
                >
                  {resultIssueId ? t('issue.import.viewDetail') : t('issue.list.title')}
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
    <PageContainer title={t('issue.import.title')} subtitle={t('issue.import.subtitle')}>
      <Card>
        <div style={{ padding: '20px 0' }}>
          <Dragger
            name="file"
            multiple={true}
            accept=".txt,.pdf,.png,.jpg"
            beforeUpload={beforeUpload}
            showUploadList={true}
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
