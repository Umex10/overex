import { Button } from './ui/button'
import Link from 'next/link'

const SignInButton = () => {
  return (
      <Button
                asChild
                className="h-9 rounded-lg bg-white text-zinc-950 transition-all duration-150 hover:bg-zinc-200 hover:shadow-md active:scale-[0.98]"
              >
                <Link href="/sign-in">Anmelden</Link>
              </Button>
  )
}

export default SignInButton