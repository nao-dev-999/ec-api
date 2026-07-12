"use client";

import { useRouter } from "next/navigation";
import { adminLogin } from "@/lib/api/auth";
import LoginForm from "@/app/LoginForm";

export default function AdminLoginPage() {
  const router = useRouter();

  async function handleLogin(email: string, password: string) {
    await adminLogin({ email, password });
    router.push("/admin");
  }

  return <LoginForm title="管理者ログイン" onLogin={handleLogin} />;
}
