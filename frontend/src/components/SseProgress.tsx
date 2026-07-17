import { useState, useEffect, useMemo } from 'react';
import { Steps, Button, Alert, Typography } from 'antd';
import {
  LoadingOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';

const { Text } = Typography;

const BUILTIN_STEPS = ['parse', 'classify', 'rag', 'report', 'qa'] as const;

interface StepState {
  status: 'wait' | 'process' | 'finish' | 'error';
  percentage: number;
  message: string;
}

export interface SseProgressProps {
  eventSource: EventSource;
  onComplete?: () => void;
  onError?: (error: string) => void;
  onRetry?: () => void;
}

export default function SseProgress({ eventSource, onComplete, onError, onRetry }: SseProgressProps) {
  const { t } = useTranslation();
  const [stepStates, setStepStates] = useState<Record<string, StepState>>({});
  const [error, setError] = useState<string | null>(null);
  const [completed, setCompleted] = useState(false);

  const currentStep = useMemo(() => {
    const keys = Object.keys(stepStates);
    if (keys.length === 0) return 0;
    const finishedCount = keys.filter((k) => stepStates[k]?.status === 'finish').length;
    return finishedCount;
  }, [stepStates]);

  useEffect(() => {
    const handleMessage = (e: MessageEvent<string>) => {
      try {
        const data = JSON.parse(e.data) as { stepName: string; percentage: number; message: string };
        setStepStates((prev) => ({
          ...prev,
          [data.stepName]: {
            status: data.percentage >= 100 ? 'finish' : 'process',
            percentage: data.percentage,
            message: data.message,
          },
        }));
      } catch {
        // Ignore malformed messages
      }
    };

    const handleDone = () => {
      setStepStates((prev) => {
        const next: Record<string, StepState> = {};
        for (const key of Object.keys(prev)) {
          next[key] = { ...prev[key], status: 'finish', percentage: 100 };
        }
        return next;
      });
      setCompleted(true);
      onComplete?.();
    };

    const handleSseError = () => {
      if (eventSource.readyState === EventSource.CLOSED) {
        const msg = t('component.sse.error');
        setError(msg);
        onError?.(msg);
      }
    };

    const handleCustomError = (e: MessageEvent<string>) => {
      try {
        const data = JSON.parse(e.data) as { message?: string };
        const msg = data.message ?? t('component.sse.error');
        setError(msg);
        onError?.(msg);
      } catch {
        const msg = t('component.sse.error');
        setError(msg);
        onError?.(msg);
      }
    };

    const handleOpen = () => {
      setError(null);
      setCompleted(false);
      setStepStates({});
    };

    eventSource.addEventListener('message', handleMessage as EventListener);
    eventSource.addEventListener('done', handleDone as EventListener);
    eventSource.addEventListener('error', handleCustomError as EventListener);
    eventSource.addEventListener('open', handleOpen as EventListener);
    eventSource.onerror = handleSseError;

    return () => {
      eventSource.removeEventListener('message', handleMessage as EventListener);
      eventSource.removeEventListener('done', handleDone as EventListener);
      eventSource.removeEventListener('error', handleCustomError as EventListener);
      eventSource.removeEventListener('open', handleOpen as EventListener);
      eventSource.onerror = null;
    };
  }, [eventSource, onComplete, onError, t]);

  const stepsItems = BUILTIN_STEPS.map((stepKey) => {
    const state = stepStates[stepKey];
    const status = state?.status ?? 'wait';
    const percentage = state?.percentage ?? 0;
    const message = state?.message ?? '';

    let icon: React.ReactNode;
    if (status === 'process') {
      icon = <LoadingOutlined />;
    } else if (status === 'finish') {
      icon = <CheckCircleOutlined />;
    } else if (status === 'error') {
      icon = <CloseCircleOutlined />;
    } else {
      icon = undefined;
    }

    const description = (
      <span>
        {message && <Text type="secondary" style={{ fontSize: 12 }}>{message}</Text>}
        {status === 'process' && (
          <Text type="secondary" style={{ fontSize: 12, marginLeft: 8 }}>
            {percentage}%
          </Text>
        )}
      </span>
    );

    return {
      title: t(`component.sse.step.${stepKey}`, stepKey),
      status: status as 'wait' | 'process' | 'finish' | 'error',
      icon,
      description,
    };
  });

  return (
    <div>
      {error && (
        <Alert
          type="error"
          message={t('component.error.title')}
          description={error}
          showIcon
          style={{ marginBottom: 20 }}
          action={
            onRetry ? (
              <Button onClick={onRetry} icon={<ReloadOutlined />} size="small">
                {t('component.error.retry')}
              </Button>
            ) : undefined
          }
        />
      )}
      {completed && !error && (
        <Alert
          type="success"
          message={t('component.sse.complete')}
          showIcon
          style={{ marginBottom: 20 }}
        />
      )}
      <Steps
        direction="vertical"
        current={currentStep}
        size="small"
        items={stepsItems}
        status={error ? 'error' : completed ? 'finish' : 'process'}
      />
    </div>
  );
}
