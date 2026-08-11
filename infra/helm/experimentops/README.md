# ExperimentOps local Helm chart

This chart is the single deployment definition for the local `kind-experimentops`
cluster. `values.yaml` contains only non-sensitive local configuration.

## Prerequisites

- Kind cluster `experimentops` created from `infra/kind-config.yaml`.
- Images built and loaded into Kind, as described below.
- NGINX Ingress controller installed with
  `infra/helm/experimentops/scripts/install-kind-ingress-nginx.sh`.
- Sealed Secrets controller installed once per Kind cluster:

```bash
bash infra/helm/experimentops/scripts/install-sealed-secrets-controller.sh
```

## Secret workflow

Do not create plaintext Kubernetes Secret YAML files and do not add secret
values to `values.yaml`. Put the required local values in ignored
`infra/docker/.env.local`; the sealing script additionally reads the ignored
JWKS file at `infra/docker/jwks/experimentops-jwks.json`.

The following values must exist in `.env.local` (or be exported for the
command): `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`,
`EXPERIMENTOPS_S3_BUCKET`, `WORKSPACE_CONFIG_SERVER_RSA_KEY`,
`EXPERIMENTOPS_DB_PASSWORD`, `KEYCLOAK_MASTER_PASSWORD`, and
`KEYCLOAK_ADMIN_PASSWORD`.

Generate the single committed encrypted values file:

```bash
bash infra/helm/experimentops/scripts/seal-secrets.sh
```

The output is `sealed-secrets.values.yaml`. It is encrypted for this Kind
cluster's controller key and is the only secret material included in the
chart. Re-run the script after any local secret changes.

## CI image promotion and GitOps

This repository is also the GitOps repository for the local Kind environment.
Argo CD watches the `latest` branch at `infra/helm/experimentops`; the Helm
values in this directory are the desired state of the cluster.

On a merged pull request, the service workflow:

1. builds the merged revision and pushes `ghcr.io/<owner>/experimentops-<service>:sha-<commit>`;
2. updates that service's image repository and tag in `values.yaml`;
3. commits `deploy(<service>): sha-<commit>` to `latest` as
   `experimentops-deploy[bot]`.

Argo CD will then detect that commit and synchronize the cluster. The
`sha-<commit>` tag is immutable deployment evidence: it identifies exactly
which source revision the cluster is meant to run. Do not use `latest` as a
deployed image tag.

Before enabling this flow in GitHub:

- Set **Settings → Actions → General → Workflow permissions** to **Read and
  write permissions**.
- If `latest` is branch-protected, allow GitHub Actions to bypass the required
  restriction or use a deployment pull-request workflow instead.
- Set the six first-party GHCR packages to public so Kind can pull them
  without an `imagePullSecret`.

The first successful publish for each service creates its corresponding Helm
promotion commit. Until then, the chart keeps its existing local image values
and can still be used with the manual Kind image-loading flow below.

Before installing Argo CD for the first time, run each of the six publish
workflows manually with **Actions → Run workflow**. This publishes a consistent
initial set of GHCR images and replaces every local image reference in the
chart. Later, only the workflow for the changed service runs after its pull
request is merged.

## Manual local build and deploy

```bash
docker compose -f infra/docker/docker-compose.local.yml build \
  platform-api scanner analysis-worker api-gateway-service
docker build --tag experimentops-frontend:local \
  services/app/frontend/experimentops-frontend
docker build --tag keycloak-realm-import:local \
  --file infra/helm/experimentops/keycloak/import/Dockerfile \
  infra/helm/experimentops/keycloak

kind load docker-image --name experimentops platform-api:local scanner:local \
  analysis-worker:local api-gateway-service:local experimentops-frontend:local \
  keycloak-realm-import:local

helm lint infra/helm/experimentops
helm upgrade --install experimentops infra/helm/experimentops \
  --namespace experimentops --create-namespace \
  --values infra/helm/experimentops/sealed-secrets.values.yaml
```

Use `helm template experimentops infra/helm/experimentops --namespace
experimentops --values infra/helm/experimentops/sealed-secrets.values.yaml`
to inspect rendered Kubernetes manifests before deploying.
