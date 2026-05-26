# platform-api

Run the local Docker image with the local environment file:

```bash
docker run --rm \
  -p 8080:8080 \
  --env-file services/applications/platform-api/.env.local \
  platform-api:local
```

If you run the command from this directory, use:

```bash
docker run --rm \
  -p 8080:8080 \
  --env-file .env.local \
  platform-api:local
```
