import { describe, expect, it, vi } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import LoginForm from "./LoginForm";

describe("LoginForm", () => {
  it("calls onLogin with the entered credentials", async () => {
    const onLogin = vi.fn().mockResolvedValue(undefined);
    render(<LoginForm title="ログイン" onLogin={onLogin} />);

    fireEvent.change(screen.getByLabelText("メールアドレス"), {
      target: { value: "user@example.com" },
    });
    fireEvent.change(screen.getByLabelText("パスワード"), {
      target: { value: "password" },
    });
    fireEvent.click(screen.getByRole("button", { name: "ログイン" }));

    await waitFor(() =>
      expect(onLogin).toHaveBeenCalledWith("user@example.com", "password"),
    );
  });

  it("shows an error message when onLogin rejects", async () => {
    const onLogin = vi.fn().mockRejectedValue(new Error("boom"));
    render(<LoginForm title="ログイン" onLogin={onLogin} />);

    fireEvent.change(screen.getByLabelText("メールアドレス"), {
      target: { value: "user@example.com" },
    });
    fireEvent.change(screen.getByLabelText("パスワード"), {
      target: { value: "password" },
    });
    fireEvent.click(screen.getByRole("button", { name: "ログイン" }));

    expect(
      await screen.findByText("ログインに失敗しました"),
    ).toBeInTheDocument();
  });
});
