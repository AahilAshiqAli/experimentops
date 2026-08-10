#!/usr/bin/env bash

set -euo pipefail

controller_manifest='https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.15.1/deploy/static/provider/kind/deploy.yaml'

kubectl apply -f "$controller_manifest"
kubectl patch deployment ingress-nginx-controller \
  --namespace ingress-nginx \
  --type merge \
  --patch '{"spec":{"template":{"spec":{"nodeSelector":{"kubernetes.io/hostname":"experimentops-control-plane"}}}}}'
kubectl rollout status deployment/ingress-nginx-controller \
  --namespace ingress-nginx \
  --timeout=120s
