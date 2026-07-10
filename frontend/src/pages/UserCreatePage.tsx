import { useState } from 'react';
import { Form, Input, Button, Card, Alert, Divider, Space } from 'antd';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { userApi } from '../api/userApi';
import { ApiError } from '../api/client';
import type { CreateUserRequest } from '../types/user';

export default function UserCreatePage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [form] = Form.useForm<CreateUserRequest>();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const i18nError = (err: unknown): string => {
    if (err instanceof ApiError) {
      return t(`error.${err.code}`, err.message);
    }
    return t('error.createFailed');
  };

  const handleSubmit = async (values: CreateUserRequest) => {
    setSubmitting(true);
    setError(null);
    try {
      await userApi.create(values);
      navigate('/users');
    } catch (err) {
      setError(i18nError(err));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div>
      {/* 页头 */}
      <div className="page-header">
        <h2 className="page-title">{t('user.createUser')}</h2>
      </div>

      {/* 表单卡片 */}
      <Card className="form-card">
        {error && (
          <Alert
            type="error"
            message={error}
            closable
            onClose={() => setError(null)}
            style={{ marginBottom: 20 }}
            showIcon
          />
        )}

        <Form<CreateUserRequest>
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          autoComplete="off"
          size="large"
          requiredMark="optional"
        >
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
            <Input placeholder={t('user.usernamePlaceholder')} maxLength={50} autoFocus />
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
            <Input placeholder={t('user.emailPlaceholder')} maxLength={255} />
          </Form.Item>

          <Form.Item
            name="password"
            label={t('user.password')}
            rules={[
              { required: true, message: t('validation.passwordRequired') },
              { min: 8, max: 100, message: t('validation.passwordLength') },
            ]}
          >
            <Input.Password placeholder={t('user.newPasswordPlaceholder')} maxLength={100} />
          </Form.Item>

          <Divider style={{ margin: '24px 0' }} />

          {/* ── 个人信息（可选） ── */}
          <div className="form-section-title">{t('form.section.profile')}</div>

          <Form.Item
            name="displayName"
            label={t('user.displayName')}
            rules={[{ max: 100, message: t('validation.displayNameMax') }]}
          >
            <Input placeholder={t('user.displayNamePlaceholder')} maxLength={100} />
          </Form.Item>

          <Form.Item
            name="phone"
            label={t('user.phone')}
            rules={[{ max: 30, message: t('validation.phoneMax') }]}
          >
            <Input placeholder={t('user.phonePlaceholder')} maxLength={30} />
          </Form.Item>

          {/* ── 提交 ── */}
          <Divider style={{ margin: '24px 0' }} />

          <Form.Item style={{ marginBottom: 0 }}>
            <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
              <Button onClick={() => navigate('/users')} size="large">
                {t('common.cancel')}
              </Button>
              <Button type="primary" htmlType="submit" loading={submitting} size="large">
                {t('common.create')}
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
}
