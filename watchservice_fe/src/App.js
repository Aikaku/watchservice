/**
 * 파일 이름 : App.js
 * 기능 : 애플리케이션의 루트 컴포넌트. 라우팅 설정과 전체 레이아웃을 관리한다.
 * 작성 날짜 : 2025/12/17
 * 작성자 : 시스템
 */
import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import MainLayout from './layout/MainLayout';
import { ToastProvider } from './components/common/Toast';
import { ConfirmProvider } from './components/common/ConfirmModal';
import ErrorBoundary from './components/common/ErrorBoundary';
import ProtectedRoute from './components/common/ProtectedRoute';
import UserProtectedRoute from './components/common/UserProtectedRoute';
import MainBoardPage from './pages/mainboard/MainBoardPage';

import NotificationPage from './pages/notifications/NotificationPage';
import NotificationDetailPage from './pages/notifications/NotificationDetailPage';
import NotificationStatsPage from './pages/notifications/NotificationStatsPage';

import LogsPage from './pages/logs/LogsPage';
import TopFilesPage from './pages/logs/TopFilesPage';
import ExtensionStatsPage from './pages/logs/ExtensionStatsPage';

import SettingHomePage from './pages/settings/SettingHomePage';
import SettingFoldersPage from './pages/settings/SettingFoldersPage';
import SettingNotifyPage from './pages/settings/SettingNotifyPage';
import SettingExceptionsPage from './pages/settings/SettingExceptionsPage';
import SettingResetPage from './pages/settings/SettingResetPage';
import SettingUpdatePage from './pages/settings/SettingUpdatePage';
import SettingFeedbackPage from './pages/settings/SettingFeedbackPage';
import SettingGuidePage from './pages/settings/SettingGuidePage';
import SettingEmailPage from './pages/settings/SettingEmailPage';
import SettingSchedulePage from './pages/settings/SettingSchedulePage';
import SettingAuditPage from './pages/settings/SettingAuditPage';

import AdminLoginPage from './pages/admin/AdminLoginPage';
import AdminMainPage from './pages/admin/AdminMainPage';
import AdminFeedbackPage from './pages/admin/AdminFeedbackPage';
import AdminNoticePage from './pages/admin/AdminNoticePage';
import AdminLogPage from './pages/admin/AdminLogPage';
import AdminAlertPage from './pages/admin/AdminAlertPage';
import AdminSessionPage from './pages/admin/AdminSessionPage';
import AdminSystemPage from './pages/admin/AdminSystemPage';
import AdminGuidePage from './pages/admin/AdminGuidePage';
import UserNoticePage from './pages/notice/UserNoticePage';
import UserLoginPage from './pages/auth/UserLoginPage';

/**
 * 함수 이름 : App
 * 기능 : 애플리케이션의 루트 컴포넌트. 모든 라우트를 정의하고 MainLayout으로 감싼다.
 * 매개변수 : 없음
 * 반환값 : JSX.Element - 애플리케이션 루트 컴포넌트
 * 작성 날짜 : 2025/12/17
 * 작성자 : 시스템
 */
function App() {
  return (
    <ToastProvider>
    <ConfirmProvider>
    <BrowserRouter>
      <ErrorBoundary>
      <MainLayout>
        <Routes>
          {/* 일반 사용자 로그인 (USER_AUTH_ENABLED=true 시 사용) */}
          <Route path="/login" element={<UserLoginPage />} />

          {/* 메인 보드 */}
          <Route path="/" element={<UserProtectedRoute><MainBoardPage /></UserProtectedRoute>} />

          {/* 알림 */}
          <Route path="/notifications" element={<UserProtectedRoute><NotificationPage /></UserProtectedRoute>} />
          <Route path="/notifications/stats" element={<UserProtectedRoute><NotificationStatsPage /></UserProtectedRoute>} />
          <Route path="/notifications/:id" element={<UserProtectedRoute><NotificationDetailPage /></UserProtectedRoute>} />

          {/* 로그 */}
          <Route path="/logs" element={<UserProtectedRoute><LogsPage /></UserProtectedRoute>} />
          <Route path="/logs/top-files" element={<UserProtectedRoute><TopFilesPage /></UserProtectedRoute>} />
          <Route path="/logs/extension-stats" element={<UserProtectedRoute><ExtensionStatsPage /></UserProtectedRoute>} />

          {/* 사용자 공지사항 */}
          <Route path="/notice" element={<UserProtectedRoute><UserNoticePage /></UserProtectedRoute>} />

          {/* 설정 */}
          <Route path="/settings" element={<UserProtectedRoute><SettingHomePage /></UserProtectedRoute>} />
          <Route path="/settings/folders" element={<UserProtectedRoute><SettingFoldersPage /></UserProtectedRoute>} />
          <Route path="/settings/exceptions" element={<UserProtectedRoute><SettingExceptionsPage /></UserProtectedRoute>} />
          <Route path="/settings/notify" element={<UserProtectedRoute><SettingNotifyPage /></UserProtectedRoute>} />
          <Route path="/settings/reset" element={<UserProtectedRoute><SettingResetPage /></UserProtectedRoute>} />
          <Route path="/settings/update" element={<UserProtectedRoute><SettingUpdatePage /></UserProtectedRoute>} />
          <Route path="/settings/feedback" element={<UserProtectedRoute><SettingFeedbackPage /></UserProtectedRoute>} />
          <Route path="/settings/guide" element={<UserProtectedRoute><SettingGuidePage /></UserProtectedRoute>} />
          <Route path="/settings/email" element={<UserProtectedRoute><SettingEmailPage /></UserProtectedRoute>} />
          <Route path="/settings/schedule" element={<UserProtectedRoute><SettingSchedulePage /></UserProtectedRoute>} />
          <Route path="/settings/audit" element={<UserProtectedRoute><SettingAuditPage /></UserProtectedRoute>} />

          {/* 관리자 (세션 인증 필요) */}
          <Route path="/admin/login" element={<AdminLoginPage />} />
          <Route path="/admin/main" element={<ProtectedRoute><AdminMainPage /></ProtectedRoute>} />
          <Route path="/admin/feedback" element={<ProtectedRoute><AdminFeedbackPage /></ProtectedRoute>} />
          <Route path="/admin/notification" element={<ProtectedRoute><AdminNoticePage /></ProtectedRoute>} />
          <Route path="/admin/logs" element={<ProtectedRoute><AdminLogPage /></ProtectedRoute>} />
          <Route path="/admin/alerts" element={<ProtectedRoute><AdminAlertPage /></ProtectedRoute>} />
          <Route path="/admin/sessions" element={<ProtectedRoute><AdminSessionPage /></ProtectedRoute>} />
          <Route path="/admin/system" element={<ProtectedRoute><AdminSystemPage /></ProtectedRoute>} />
          <Route path="/admin/guide" element={<ProtectedRoute><AdminGuidePage /></ProtectedRoute>} />
        </Routes>
      </MainLayout>
      </ErrorBoundary>
    </BrowserRouter>
    </ConfirmProvider>
    </ToastProvider>
  );
}

export default App;
