import { LoginForm } from "@/features/auth/components/LoginForm";
import { Stethoscope } from "lucide-react";

export default function LoginPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-primary-50 to-slate-100 p-4">
      <div className="w-full max-w-md rounded-2xl bg-white p-8 shadow-xl">
        <div className="mb-6 flex flex-col items-center gap-2">
          <Stethoscope className="h-10 w-10 text-primary-600" />
          <h1 className="text-2xl font-bold text-slate-900">Welcome Back</h1>
          <p className="text-sm text-slate-500">Sign in to manage your appointments</p>
        </div>
        <LoginForm />
        <div className="mt-6 rounded-lg bg-slate-50 p-3 text-xs text-slate-500">
          <p className="font-medium">Test credentials:</p>
          <p>patient@healthapp.com / Password123!</p>
        </div>
      </div>
    </div>
  );
}
