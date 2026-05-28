from fastapi import FastAPI

app = FastAPI(title="Analysis API")


@app.get("/health")
async def health_check() -> dict[str, str]:
    return {"status": "ok"}


def main() -> None:
    import uvicorn

    uvicorn.run("analysis_api.main:app", host="0.0.0.0", port=8000)
