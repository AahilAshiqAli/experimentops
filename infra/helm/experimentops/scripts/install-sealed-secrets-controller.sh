#!/usr/bin/env bash

# Cluster-level prerequisite for this one local Kind cluster. This official
# pinned controller manifest installs the CRD and controller in kube-system.
set -euo pipefail

kubectl apply -f \
  https://github.com/bitnami/sealed-secrets/releases/download/v0.38.4/controller.yaml
