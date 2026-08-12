#!/usr/bin/env bash

# Create a locally trusted certificate for the Kind ingress and store it only
# in the cluster. No private key or certificate is written into the repository.
set -euo pipefail
umask 077

kind_context="kind-experimentops"
namespace="experimentops"
secret_name="experimentops-local-tls"
frontend_host="experimentops.test"
api_host="api.experimentops.test"

for command_name in kubectl mkcert; do
  command -v "$command_name" >/dev/null 2>&1 || {
    echo "Required command not found: $command_name" >&2
    exit 1
  }
done

current_context="$(kubectl config current-context)"
if [[ "$current_context" != "$kind_context" ]]; then
  echo "Refusing to create the TLS Secret outside $kind_context." >&2
  echo "Current context: $current_context" >&2
  exit 1
fi

temporary_directory="$(mktemp -d)"
trap 'rm -rf "$temporary_directory"' EXIT

certificate_file="$temporary_directory/experimentops-local.pem"
private_key_file="$temporary_directory/experimentops-local-key.pem"

mkcert -install
mkcert \
  -cert-file "$certificate_file" \
  -key-file "$private_key_file" \
  "$frontend_host" "$api_host"

kubectl create namespace "$namespace" --dry-run=client -o yaml |
  kubectl apply -f -

kubectl create secret tls "$secret_name" \
  --namespace "$namespace" \
  --cert "$certificate_file" \
  --key "$private_key_file" \
  --dry-run=client -o yaml |
  kubectl apply -f -

echo
echo "Created TLS Secret $namespace/$secret_name for:"
echo "  https://$frontend_host"
echo "  https://$api_host"
