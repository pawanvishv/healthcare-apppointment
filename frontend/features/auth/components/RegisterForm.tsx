"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useRouter } from "next/navigation";
import { register as registerApi } from "@/features/auth/api";
import { useAuthStore } from "@/store/auth-store";
import { useToast } from "@/components/ui/Toast";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Select } from "@/components/ui/Select";
import { HttpError } from "@/lib/http/client";
import Link from "next/link";

const registerSchema = z
  .object({
    email: z.string().email("Invalid email"),
    password: z.string().min(8, "Password must be at least 8 characters"),
    role: z.enum(["PATIENT", "DOCTOR"]),
    specialization: z.string().optional(),
    phone: z.string().optional(),
  })
  .refine(
    (data) => data.role !== "DOCTOR" || !!data.specialization?.trim(),
    { message: "Specialization is required for doctors", path: ["specialization"] }
  );

type RegisterFormData = z.infer<typeof registerSchema>;

export function RegisterForm() {
  const router = useRouter();
  const setSession = useAuthStore((s) => s.setSession);
  const { showToast } = useToast();

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
    defaultValues: { role: "PATIENT" },
  });

  const role = watch("role");

  const onSubmit = async (data: RegisterFormData) => {
    try {
      const tokens = await registerApi(data);
      setSession({
        accessToken: tokens.accessToken,
        refreshToken: tokens.refreshToken,
        email: data.email,
        role: data.role,
      });
      showToast("Account created successfully!", "success");
      router.push("/appointments");
    } catch (err) {
      const message =
        err instanceof HttpError ? err.message : "Registration failed.";
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
      <Select
        label="Role"
        options={[
          { value: "PATIENT", label: "Patient" },
          { value: "DOCTOR", label: "Doctor" },
        ]}
        {...register("role")}
        error={errors.role?.message}
      />
      {role === "DOCTOR" && (
        <Input
          label="Specialization"
          {...register("specialization")}
          error={errors.specialization?.message}
        />
      )}
      {role === "PATIENT" && (
        <Input
          label="Phone (optional)"
          {...register("phone")}
          error={errors.phone?.message}
        />
      )}
      <Button type="submit" className="w-full" loading={isSubmitting}>
        Create Account
      </Button>
      <p className="text-center text-sm text-slate-600">
        Already have an account?{" "}
        <Link href="/login" className="text-primary-600 hover:underline">
          Sign In
        </Link>
      </p>
    </form>
  );
}
