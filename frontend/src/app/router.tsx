import { createBrowserRouter, Navigate, Outlet } from "react-router-dom";
import { AppLayout } from "@/components/layout/AppLayout";
import { AuthLayout } from "@/components/layout/AuthLayout";
import { DashboardPage } from "@/features/dashboard/pages/DashboardPage";
import { ForgotPasswordPage } from "@/features/auth/pages/ForgotPasswordPage";
import { LoginPage } from "@/features/auth/pages/LoginPage";
import { RegisterPage } from "@/features/auth/pages/RegisterPage";
import { RegisterSuccessPage } from "@/features/auth/pages/RegisterSuccessPage";
import { CompositionDetailPage } from "@/features/compositions/pages/CompositionDetailPage";
import { CompositionEditorPage } from "@/features/compositions/pages/CompositionEditorPage";
import { CompositionListPage } from "@/features/compositions/pages/CompositionListPage";
import { LandingPage } from "@/features/marketing/pages/LandingPage";
import { useAuthStore } from "@/store/auth-store";

function RequireAuth() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
}

function RedirectIfAuthenticated() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  return <Outlet />;
}

export const router = createBrowserRouter([
  { path: "/", element: <LandingPage /> },
  {
    element: <RedirectIfAuthenticated />,
    children: [
      {
        element: <AuthLayout />,
        children: [
          { path: "/login", element: <LoginPage /> },
          { path: "/register", element: <RegisterPage /> },
          { path: "/register/success", element: <RegisterSuccessPage /> },
          { path: "/forgot-password", element: <ForgotPasswordPage /> },
        ],
      },
    ],
  },
  {
    element: <RequireAuth />,
    children: [
      {
        element: <AppLayout />,
        children: [
          { path: "/dashboard", element: <DashboardPage /> },
          { path: "/compositions", element: <CompositionListPage /> },
          { path: "/compositions/new", element: <CompositionEditorPage /> },
          { path: "/compositions/:id", element: <CompositionDetailPage /> },
          { path: "/compositions/:id/edit", element: <CompositionEditorPage /> },
        ],
      },
    ],
  },
  { path: "*", element: <Navigate to="/" replace /> },
]);
