import { Tag } from 'antd';
import { useTranslation } from 'react-i18next';

const DEFAULT_COLOR_MAP: Record<string, string> = {
  OPEN: 'blue',
  ANALYZING: 'processing',
  CLASSIFIED: 'cyan',
  RESOLVED: 'green',
  CLOSED: 'default',
  ERROR: 'red',
};

export interface StatusTagProps {
  status: string;
  colorMap?: Record<string, string>;
}

export default function StatusTag({ status, colorMap }: StatusTagProps) {
  const { t } = useTranslation();
  const mergedColorMap = { ...DEFAULT_COLOR_MAP, ...colorMap };
  const color = mergedColorMap[status] ?? 'default';

  return <Tag color={color}>{t(`component.status.${status}`, status)}</Tag>;
}
