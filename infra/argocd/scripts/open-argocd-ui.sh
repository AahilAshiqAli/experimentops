#!/usr/bin/env bash

set -euo pipefail

KIND_CONTEXT="kind-experimentops"
ARGOCD_NAMESPACE="argocd"

if [[ "$(kubectl config current-context)" != "${KIND_CONTEXT}" ]]; then
  echo "Refusing to port-forward outside ${KIND_CONTEXT}." >&2
  echo "Current context: $(kubectl config current-context)" >&2
  exit 1
fi

echo "Open https://localhost:8080 and sign in as admin."
exec kubectl --namespace "${ARGOCD_NAMESPACE}" port-forward svc/argocd-server 8080:443
