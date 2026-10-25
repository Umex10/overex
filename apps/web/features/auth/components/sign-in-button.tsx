import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import Link from "next/link";

const SignInButton = ({className}: {className?: string}) => {
  return (
    <Button
      asChild
      className={cn(
        "h-9 rounded-lg bg-white text-zinc-950 transition-all duration-150 hover:bg-zinc-200 hover:shadow-md active:scale-[0.98]",
        className
      )}
    >
      <Link href="/sign-in">Anmelden</Link>
    </Button>
  );
};

export default SignInButton;
