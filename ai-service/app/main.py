import os

import httpx
from fastapi import FastAPI
from prometheus_client import Counter as PromCounter, generate_latest
from starlette.responses import Response

app = FastAPI(title="SYNTRA Agent")
REQUESTS = PromCounter("syntra_ai_requests_total", "AI requests", ["endpoint"])

URGENT = ("urgent", "asap", "immediately", "critical", "deadline", "eod", "blocker")
FYI = ("fyi", "newsletter", "optional", "heads up", "for your information")


def complete(prompt: str) -> str | None:
    key = os.getenv("LLM_API_KEY", "").strip()
    base = os.getenv("LLM_BASE_URL", "").strip().rstrip("/")
    model = os.getenv("LLM_MODEL", "gpt-4o-mini")
    if not key or not base:
        return None
    try:
        response = httpx.post(
            f"{base}/chat/completions",
            headers={"Authorization": f"Bearer {key}"},
            json={
                "model": model,
                "temperature": 0.2,
                "messages": [
                    {"role": "system", "content": "You assist an internal messaging platform. Be brief. Never invent facts that are not in the source text."},
                    {"role": "user", "content": prompt},
                ],
            },
            timeout=8.0,
        )
        response.raise_for_status()
        return response.json()["choices"][0]["message"]["content"].strip()
    except Exception:
        return None


def clip(text: str, limit: int = 700) -> str:
    text = " ".join(text.split())
    return text if len(text) <= limit else text[:limit].rstrip() + "..."


@app.get("/health")
def health():
    return {"status": "ok"}


@app.get("/metrics")
def metrics():
    return Response(generate_latest(), media_type="text/plain; version=0.0.4")


@app.post("/priority")
def priority(payload: dict):
    REQUESTS.labels("priority").inc()
    text = f"{payload.get('subject', '')} {payload.get('body', '')}".lower()
    if any(word in text for word in URGENT):
        label, reason = "URGENT", "Time-sensitive language"
    elif any(word in text for word in FYI):
        label, reason = "FYI", "Informational language"
    else:
        label, reason = "NORMAL", "No urgency markers"
    return {"priority": label, "reason": reason}


@app.post("/summarize")
def summarize(payload: dict):
    REQUESTS.labels("summarize").inc()
    texts = [str(item) for item in payload.get("texts", []) if str(item).strip()]
    if not texts:
        return {"summary": "Nothing to summarize yet."}
    source = "\n".join(texts[-30:])
    generated = complete("Summarize this internal thread in 4 short sentences:\n" + source)
    if generated:
        return {"summary": generated}
    head = texts[0]
    tail = texts[-1] if len(texts) > 1 else ""
    summary = clip(head if head == tail else f"{head} Later: {tail}")
    return {"summary": summary}


@app.post("/draft")
def draft(payload: dict):
    REQUESTS.labels("draft").inc()
    subject = payload.get("subject") or "your note"
    instruction = payload.get("instruction") or "Write a short professional reply."
    context = payload.get("context") or ""
    generated = complete(f"Subject: {subject}\nInstruction: {instruction}\nContext:\n{context}\nWrite the message body only.")
    if generated:
        return {"draft": generated}
    return {"draft": f"Regarding {subject},\n\n{instruction}\n\nThanks."}


@app.post("/ask")
def ask(payload: dict):
    REQUESTS.labels("ask").inc()
    question = str(payload.get("question") or "").strip()
    transcripts = [str(item) for item in payload.get("transcripts", []) if str(item).strip()]
    if not question or not transcripts:
        return {"answer": "There is not enough room history to answer that."}
    generated = complete(
        "Answer only from the transcript. If it is not there, say the room history does not contain an answer.\n"
        f"Question: {question}\nTranscript:\n" + "\n".join(transcripts[-40:])
    )
    if generated:
        return {"answer": generated}
    words = [word for word in question.lower().split() if len(word) > 3]
    scores = []
    for line in transcripts:
        scores.append((sum(line.lower().count(word) for word in words), line))
    scores.sort(reverse=True)
    best = [line for score, line in scores if score > 0][:2]
    if not best:
        return {"answer": "The room history does not contain an answer to that."}
    return {"answer": "From the room: " + clip(" ".join(best), 500)}
