"use client";

import { useRouter } from "next/navigation";
import { customerLogin } from "@/lib/api/customerAuth";
import LoginForm from "@/app/LoginForm";

export default function CustomerLoginPage() {
  const router = useRouter();

  async function handleLogin(email: string, password: string) {
    await customerLogin({ email, password });
    router.push("/products");
  }

  return (
    <LoginForm
      title="ログイン"
      onLogin={handleLogin}
      style={{ padding: 24, maxWidth: 360 }}
    />
  );
}
