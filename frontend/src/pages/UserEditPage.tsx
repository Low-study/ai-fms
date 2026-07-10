import { useState, useEffect } from 'react';
import { Form, Input, Button, Card, Alert, Select, Space, Divider, Skeleton } from 'antd';
import { useNavigate, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { userApi } from '../api/userApi';
import { ApiError } from '../api/client';
import type { User } from '../types/user';

export default function UserEditPage() {
  const { t } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [form] = Form.useForm();
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const i18nError = (err: unknown, fallbackKey: string): string => {
    if (err instanceof ApiError) {
      return t(`error.${err.code}`, err.message);
    }
    return t(fallbackKey);
  };

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    setLoadError(null);
    userApi
      .getById(id)
      .then((response) => {
        setUser(response.data);
        form.setFieldsValue({
          username: response.data.username,
          email: response.data.email,
          displayName: response.data.displayName || '',
          phone: response.data.phone || '',
          status: response.data.status,
        });
      })
      .catch((err) => {
        setLoadError(i18nError(err, 'error.loadFailed'));
      })
      .finally(() => setLoading(false));
  }, [id, form]);

  const handleSubmit = async (values: Record<string, unknown>) => {
    if (!id || !user) return;
    setSubmitting(true);
    setSubmitError(null);
    try {
      const payload: Record<string, unknown> = {};
      if (values.username !== user.username) payload.username = values.username;
      if (values.email !== user.email) payload.email = values.email;
      if (values.displayName !== (user.displayName || '')) payload.displayName = values.displayName;
      if (values.phone !== (user.phone || '')) payload.phone = values.phone;
      if (values.password) payload.password = values.password;
      if (values.status !== user.status) payload.status = values.status;

      if (Object.keys(payload).length > 0) {
        await userApi.update(id, payload);
      }
      navigate('/users');
    } catch (err) {
      setSubmitError(i18nError(err, 'error.updateFailed'));
    } finally {
      setSubmitting(false);
    }
  };

  // ── Loading ──
  if (loading) {
    return (
      <div>
        <div className="page-header">
          <h2 className="page-title">{t('user.editUser', { username: '…' })}</h2>
        </div>
        <Card className="form-card">
          <Skeleton active paragraph={{ rows: 6 }} />
        </Card>
      </div>
    );
  }

  // ── Load Error ──
  if (loadError && !user) {
    return (
      <div>
        <div className="page-header">
          <h2 className="page-title">{t('user.editUser', { username: '…' })}</h2>
        </div>
        <Alert
          type="error"
          message={loadError}
          showIcon
          action={
            <Button onClick={() => navigate('/users')}>{t('common.back')}</Button>
          }
        />
      </div>
    );
  }

  if (!user) return null;

  return (
    <div>
      {/* 页头 */}
      <div className="page-header">
        <h2 className="page-title">{t('user.editUser', { username: user.username })}</h2>
      </div>

      {/* 表单卡片 */}
      <Card className="form-card">
        {submitError && (
          <Alert
            type="error"
            message={submitError}
            closable
            onClose={() => setSubmitError(null)}
            style={{ marginBottom: 20 }}
            showIcon
          />
        )}

        <Form form={form} layout="vertical" onFinish={handleSubmit} autoComplete="off" size="large" requiredMark="optional">
          {/* ── 账户信息 ── */}
          <div className="form-section-title">{t('form.section.account')}</div>

          <Form.Item
            name="username"
            label={t('user.username')}
            rules={[
              { required: true, message: t('validation.usernameRequired') },
              { min: 3, max: 50, message: t('validation.usernameLength') },
              { pattern: /^[a-zA-Z0-9_]+$/, message: t('validation.usernamePattern') },
            ]}
          >
            <Input maxLength={50} />
          </Form.Item>

          <Form.Item
            name="email"
            label={t('user.email')}
            rules={[
              { required: true, message: t('validation.emailRequired') },
              { type: 'email', message: t('validation.emailInvalid') },
              { max: 255, message: t('validation.emailMax') },
            ]}
          >
            <Input maxLength={255} />
          </Form.Item>

          <Form.Item
            name="password"
            label={t('user.newPassword')}
            rules={[{ min: 8, max: 100, message: t('validation.passwordLength') }]}
            tooltip={t('user.passwordLeaveBlank')}
          >
            <Input.Password placeholder={t('user.passwordLeaveBlank')} maxLength={100} />
          </Form.Item>

          <Divider style={{ margin: '24px 0' }} />

          {/* ── 个人信息 ── */}
          <div className="form-section-title">{t('form.section.profile')}</div>

          <Form.Item
            name="displayName"
            label={t('user.displayName')}
            rules={[{ max: 100, message: t('validation.displayNameMax') }]}
          >
            <Input maxLength={100} />
          </Form.Item>

          <Form.Item
            name="phone"
            label={t('user.phone')}
            rules={[{ max: 30, message: t('validation.phoneMax') }]}
          >
            <Input maxLength={30} />
          </Form.Item>

          <Divider style={{ margin: '24px 0' }} />

          {/* ── 状态 ── */}
          <div className="form-section-title">{t('form.section.status')}</div>

          <Form.Item name="status" label={t('user.status')}>
            <Select
              options={[
                { value: 'ACTIVE', label: t('userStatus.ACTIVE') },
                { value: 'LOCKED', label: t('userStatus.LOCKED') },
                { value: 'DISABLED', label: t('userStatus.DISABLED') },
              ]}
            />
          </Form.Item>

          <Divider style={{ margin: '24px 0' }} />

          {/* ── 提交 ── */}
          <Form.Item style={{ marginBottom: 0 }}>
            <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
              <Button onClick={() => navigate('/users')} size="large">
                {t('common.cancel')}
              </Button>
              <Button type="primary" htmlType="submit" loading={submitting} size="large">
                {t('user.saveChanges')}
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
}
