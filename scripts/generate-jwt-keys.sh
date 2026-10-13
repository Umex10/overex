#!/usr/bin/env bash
# =============================================================================
# Overex — generate the RSA key pair that signs and verifies the JWTs (RS256)
#
# Usage:
#   scripts/generate-jwt-keys.sh              -> writes to apps/backend/keys/
#   scripts/generate-jwt-keys.sh <directory>  -> writes to <directory>/
#
# Creates two PEM files in the formats the backend reads (KeyConfig):
#   private.pem  PKCS#8 private key  -> signs tokens, keep it secret
#   public.pem   X.509 public key    -> verifies tokens, served at /.well-known/jwks.json
#
# When do you need it?
#   - "docker compose up" on a fresh clone: the container never generates keys,
#     it mounts apps/backend/keys/. Run this script once before the first start.
#   - Running the backend directly (./mvnw spring-boot:run) does not need it:
#     the backend creates a dev pair on first start if none exists.
#   - Production: generate a separate pair (pass a directory outside the repo),
#     store both files as a secret and mount them into the container. Never
#     reuse the dev keys.
#
# Needs only openssl (Linux/macOS have it; on Windows use Git Bash or WSL).
# =============================================================================
set -euo pipefail

# Default target: apps/backend/keys next to this script's repository root,
# so the script works no matter from which folder it is called.
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
key_dir="${1:-$repo_root/apps/backend/keys}"
private_key="$key_dir/private.pem"
public_key="$key_dir/public.pem"

if ! command -v openssl >/dev/null 2>&1; then
  echo "error: openssl is not installed" >&2
  exit 1
fi

# Never overwrite: a new private key would invalidate every issued token, and
# replacing only one file would leave a pair that does not match.
if [[ -e "$private_key" || -e "$public_key" ]]; then
  echo "Keys already exist in $key_dir, nothing to do."
  echo "Delete private.pem and public.pem first to create a new pair."
  exit 0
fi

mkdir -p "$key_dir"
# genpkey writes the private key as PKCS#8 ("BEGIN PRIVATE KEY"), the format
# KeyConfig expects; pkey -pubout derives the matching X.509 public key.
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$private_key" 2>/dev/null
openssl pkey -in "$private_key" -pubout -out "$public_key"

# Readable for everyone on purpose: in Docker the backend runs as another user
# (overex, uid 1001) and must read the mounted dev keys. For production keys,
# restrict access to the user that runs the backend instead.
chmod 644 "$private_key" "$public_key"

echo "Created $private_key"
echo "Created $public_key"
