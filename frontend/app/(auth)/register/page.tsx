import { RegisterForm } from "@/features/auth/components/RegisterForm";
import { Stethoscope } from "lucide-react";

export default function RegisterPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-primary-50 to-slate-100 p-4">
      <div className="w-full max-w-md rounded-2xl bg-white p-8 shadow-xl">
        <div className="mb-6 flex flex-col items-center gap-2">
          <Stethoscope className="h-10 w-10 text-primary-600" />
          <h1 className="text-2xl font-bold text-slate-900">Create Account</h1>
          <p className="text-sm text-slate-500">Register as a patient or doctor</p>
        </div>
        <RegisterForm />
      </div>
    </div>
  );
}
