FROM ghcr.io/astral-sh/uv:0.11.16 AS uv

FROM python:3.13-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    UV_COMPILE_BYTECODE=1 \
    UV_LINK_MODE=copy

RUN groupadd --system app && useradd --system --gid app --create-home --shell /usr/sbin/nologin app

COPY --from=uv /uv /uvx /bin/

WORKDIR /app

COPY . .

RUN uv sync --frozen --no-dev --package analysis-worker \
    && chown -R app:app /app

USER app

CMD ["/app/.venv/bin/analysis-worker"]
