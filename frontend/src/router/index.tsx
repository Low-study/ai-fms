import { Routes, Route, Navigate } from 'react-router-dom';
import MainLayout from '../layouts/MainLayout';
import HomePage from '../pages/HomePage';
import UserListPage from '../pages/UserListPage';
import UserCreatePage from '../pages/UserCreatePage';
import UserEditPage from '../pages/UserEditPage';
import IssueListPage from '../pages/IssueListPage';
import IssueImportPage from '../pages/IssueImportPage';
import IssueDetailPage from '../pages/IssueDetailPage';

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<MainLayout />}>
        <Route index element={<HomePage />} />
        <Route path="users" element={<UserListPage />} />
        <Route path="users/create" element={<UserCreatePage />} />
        <Route path="users/:id/edit" element={<UserEditPage />} />
        <Route path="issues" element={<IssueListPage />} />
        <Route path="issues/import" element={<IssueImportPage />} />
        <Route path="issues/:id" element={<IssueDetailPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
