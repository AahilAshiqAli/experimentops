#!/usr/bin/env bash

set -euo pipefail

ARGOCD_VERSION="v3.4.2"
ARGOCD_NAMESPACE="argocd"
KIND_CONTEXT="kind-experimentops"
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPOSITORY_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
APPLICATION_MANIFEST="${REPOSITORY_ROOT}/infra/argocd/experimentops-local.yaml"
INSTALL_MANIFEST="https://raw.githubusercontent.com/argoproj/argo-cd/${ARGOCD_VERSION}/manifests/install.yaml"

require_command() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Required command not found: $1" >&2
    exit 1
  }
}

require_command kubectl

if [[ "$(kubectl config current-context)" != "${KIND_CONTEXT}" ]]; then
  echo "Refusing to install Argo CD outside ${KIND_CONTEXT}." >&2
  echo "Current context: $(kubectl config current-context)" >&2
  exit 1
fi

if [[ ! -f "${APPLICATION_MANIFEST}" ]]; then
  echo "Application manifest not found: ${APPLICATION_MANIFEST}" >&2
  exit 1
fi

kubectl create namespace "${ARGOCD_NAMESPACE}" --dry-run=client -o yaml | kubectl apply -f -
kubectl apply --server-side --force-conflicts --namespace "${ARGOCD_NAMESPACE}" -f "${INSTALL_MANIFEST}"

kubectl --namespace "${ARGOCD_NAMESPACE}" wait \
  --for=condition=Available deployment \
  --all \
  --timeout=300s
kubectl --namespace "${ARGOCD_NAMESPACE}" rollout status \
  statefulset/argocd-application-controller \
  --timeout=300s

kubectl apply -f "${APPLICATION_MANIFEST}"

echo
echo "Argo CD is installed and is now watching the ExperimentOps Helm chart."
echo "Check its state with:"
echo "  kubectl --context ${KIND_CONTEXT} -n ${ARGOCD_NAMESPACE} get application experimentops-local"
echo "Open the UI with:"
echo "  ${SCRIPT_DIR}/open-argocd-ui.sh"
