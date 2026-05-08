from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer
from typing import List
import torch
import logging
import time

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI()

device = "cuda" if torch.cuda.is_available() else "cpu"

model = SentenceTransformer(
    "BAAI/bge-large-zh-v1.5",
    device=device
)

if device == "cuda":
    model.half()


class EmbedRequest(BaseModel):
    texts: List[str]


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/embed")
def embed(req: EmbedRequest):
    start_time = time.time()
    logger.info(f"Request received: {len(req.texts)} texts")

    if len(req.texts) > 128:
        logger.error(f"Validation failed: too many texts ({len(req.texts)})")
        raise HTTPException(status_code=400, detail="Too many texts")

    embeddings = model.encode(
        req.texts,
        batch_size=32,
        normalize_embeddings=True,
        convert_to_numpy=True
    )

    elapsed = time.time() - start_time
    logger.info(f"Response sent: {len(embeddings)} embeddings, dimensions={embeddings.shape[1]}, elapsed={elapsed:.3f}s")

    return {
        "embeddings": embeddings.tolist(),
        "dimensions": embeddings.shape[1]
    }
