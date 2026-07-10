import Link from "next/link";
import { Button } from "@/components/ui/Button";

export default function NotFound() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 p-4">
      <h2 className="text-xl font-semibold text-slate-900">Page not found</h2>
      <p className="text-sm text-slate-500">The page you are looking for does not exist.</p>
      <Link href="/login">
        <Button>Go to Login</Button>
      </Link>
    </div>
  );
}
