"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useRouter } from "next/navigation";
import { login } from "@/features/auth/api";
import { useAuthStore } from "@/store/auth-store";
import { useToast } from "@/components/ui/Toast";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { HttpError } from "@/lib/http/client";
import Link from "next/link";

const loginSchema = z.object({
  email: z.string().email("Invalid email"),
  password: z.string().min(1, "Password is required"),
});

type LoginFormData = z.infer<typeof loginSchema>;

export function LoginForm() {
  const router = useRouter();
  const setSession = useAuthStore((s) => s.setSession);
  const { showToast } = useToast();

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: "patient@healthapp.com", password: "Password123!" },
  });

  const onSubmit = async (data: LoginFormData) => {
    try {
      const tokens = await login(data);
      setSession({
        accessToken: tokens.accessToken,
        refreshToken: tokens.refreshToken,
        email: data.email,
      });
      showToast("Logged in successfully!", "success");
      router.push("/appointments");
    } catch (err) {
      const message =
        err instanceof HttpError ? err.message : "Login failed. Please try again.";
      showToast(message, "error");
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <Input
        label="Email"
        type="email"
        {...register("email")}
        error={errors.email?.message}
      />
      <Input
        label="Password"
        type="password"
        {...register("password")}
        error={errors.password?.message}
      />
      <Button type="submit" className="w-full" loading={isSubmitting}>
        Sign In
      </Button>
      <p className="text-center text-sm text-slate-600">
        Don&apos;t have an account?{" "}
        <Link href="/register" className="text-primary-600 hover:underline">
          Register
        </Link>
      </p>
    </form>
  );
}
