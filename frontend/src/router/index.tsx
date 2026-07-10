import { Routes, Route, Navigate } from 'react-router-dom';
import MainLayout from '../layouts/MainLayout';
import HomePage from '../pages/HomePage';
import UserListPage from '../pages/UserListPage';
import UserCreatePage from '../pages/UserCreatePage';
import UserEditPage from '../pages/UserEditPage';

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<MainLayout />}>
        <Route index element={<HomePage />} />
        <Route path="users" element={<UserListPage />} />
        <Route path="users/create" element={<UserCreatePage />} />
        <Route path="users/:id/edit" element={<UserEditPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
