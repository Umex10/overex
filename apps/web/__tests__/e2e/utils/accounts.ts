/**
 * Test accounts for the e2e run. The backend starts with an empty schema on
 * every run (Spring profile `e2e`), so fixed values are safe here.
 */
export interface TestAccount {
  name: string;
  email: string;
  phone: string;
  password: string;
}

/**
 * Registered once by auth.setup.ts; the signed-in specs start as this user.
 * Other specs use it to provoke "already taken" and to sign in.
 */
export const bootstrapAccount: TestAccount = {
  name: "Bootstrap User",
  email: "bootstrap@overex.test",
  phone: "+4366000000001",
  password: "bootstrap-pass-1",
};

let counter = 0;

/**
 * A brand-new account for a spec that registers someone. Unique per call (and
 * per retry), so specs never collide with each other or with a retried run.
 */
export function newAccount(): TestAccount {
  const id = `${Date.now()}${process.pid}${counter++}`;
  return {
    name: "New User",
    email: `new-${id}@overex.test`,
    // The end of the id holds the process id and counter, so it stays unique.
    phone: `+43${id.slice(-12)}`,
    password: "new-user-pass-1",
  };
}
