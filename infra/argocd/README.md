# ExperimentOps local Argo CD

Argo CD is installed inside the local `kind-experimentops` cluster in the
`argocd` namespace. It watches this same repository's `latest` branch and
renders `infra/helm/experimentops` into the `experimentops` namespace.

## Bootstrap

After the five application images have been published to public GHCR packages
and their SHA tags are present in Helm values, install Argo CD once:

```bash
bash infra/argocd/scripts/install-argocd.sh
```

The script is deliberately pinned to Argo CD `v3.4.2` and refuses to run unless
the active kubeconfig context is `kind-experimentops`.

## Local UI

```bash
bash infra/argocd/scripts/open-argocd-ui.sh
```

Open <https://localhost:8080>. The initial username is `admin`; retrieve its
generated password with:

```bash
kubectl -n argocd get secret argocd-initial-admin-secret \
  -o jsonpath="{.data.password}" | base64 -d; echo
```

Change that password after logging in, then remove the initial secret:

```bash
kubectl -n argocd delete secret argocd-initial-admin-secret
```

## Observe synchronization

```bash
kubectl -n argocd get application experimentops-local
kubectl -n argocd describe application experimentops-local
kubectl -n experimentops get pods
```

Argo CD has automated synchronization, pruning, and self-healing enabled for
this local-only environment. Once it is managing the application, do not run
manual `helm upgrade` commands for `experimentops`.
