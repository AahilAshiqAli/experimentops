#!/usr/bin/env bash

# Encrypt the local-only secret inputs for the current Kind cluster. The output
# is safe to commit; plaintext Kubernetes Secret manifests are never written.
set -euo pipefail
umask 077

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
env_file="$repo_root/infra/docker/.env.local"
jwks_file="$repo_root/infra/docker/jwks/experimentops-jwks.json"
output_file="$repo_root/infra/helm/experimentops/sealed-secrets.values.yaml"
namespace="experimentops"
controller_name="sealed-secrets-controller"
controller_namespace="kube-system"

for command in kubectl kubeseal ruby; do
  command -v "$command" >/dev/null || { echo "Missing required command: $command" >&2; exit 1; }
done
for file in "$env_file" "$jwks_file"; do
  [[ -f "$file" ]] || { echo "Missing required local input: $file" >&2; exit 1; }
done

# .env.local is ignored and is the only plaintext configuration source.
set -a
# shellcheck disable=SC1090
source "$env_file"
set +a

require() {
  [[ -n "${!1:-}" ]] || { echo "Missing required value in $env_file or the environment: $1" >&2; exit 1; }
}

for variable in AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY AWS_REGION EXPERIMENTOPS_S3_BUCKET \
  WORKSPACE_CONFIG_SERVER_RSA_KEY EXPERIMENTOPS_DB_PASSWORD \
  KEYCLOAK_MASTER_PASSWORD KEYCLOAK_ADMIN_PASSWORD; do
  require "$variable"
done

temporary_directory="$(mktemp -d)"
trap 'rm -rf "$temporary_directory"' EXIT

seal() {
  local name="$1"
  kubeseal --format yaml \
    --controller-name "$controller_name" \
    --controller-namespace "$controller_namespace" > "$temporary_directory/$name.yaml"
}

kubectl create secret generic experimentops-runtime-secrets --namespace "$namespace" \
  --from-literal=EXPERIMENTOPS_STORAGE_S3_BUCKET="$EXPERIMENTOPS_S3_BUCKET" \
  --from-literal=EXPERIMENTOPS_STORAGE_S3_REGION="$AWS_REGION" \
  --from-literal=AWS_ACCESS_KEY_ID="$AWS_ACCESS_KEY_ID" \
  --from-literal=AWS_SECRET_ACCESS_KEY="$AWS_SECRET_ACCESS_KEY" \
  --dry-run=client -o yaml | seal experimentops-runtime-secrets

kubectl create secret generic platform-api-secrets --namespace "$namespace" \
  --from-literal=EXPERIMENTOPS_DB_PASSWORD="$EXPERIMENTOPS_DB_PASSWORD" \
  --from-literal=KEYCLOAK_MASTER_PASSWORD="$KEYCLOAK_MASTER_PASSWORD" \
  --from-literal=WORKSPACE_CONFIG_SERVER_RSA_KEY="$WORKSPACE_CONFIG_SERVER_RSA_KEY" \
  --dry-run=client -o yaml | seal platform-api-secrets

kubectl create secret generic keycloak-secrets --namespace "$namespace" \
  --from-literal=KEYCLOAK_ADMIN=admin \
  --from-literal=KEYCLOAK_ADMIN_PASSWORD="$KEYCLOAK_ADMIN_PASSWORD" \
  --dry-run=client -o yaml | seal keycloak-secrets

kubectl create secret generic api-gateway-service-secrets --namespace "$namespace" \
  --from-file=JWKS_CERTS="$jwks_file" \
  --dry-run=client -o yaml | seal api-gateway-service-secrets

ruby -r yaml - "$output_file" "$temporary_directory"/*.yaml <<'RUBY'
output, *files = ARGV
secrets = files.map do |file|
  document = YAML.load_file(file)
  {
    'name' => document.fetch('metadata').fetch('name'),
    'type' => document.fetch('spec').fetch('template').fetch('type', 'Opaque'),
    'encryptedData' => document.fetch('spec').fetch('encryptedData')
  }
end
File.write(output, YAML.dump('sealedSecrets' => secrets))
RUBY

echo "Wrote encrypted values: ${output_file#$repo_root/}"
