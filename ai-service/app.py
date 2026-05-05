import os
from flask import Flask, request, jsonify

app = Flask(__name__)


@app.get("/health")
def health():
    return jsonify({"status": "UP"}), 200


@app.post("/analyze")
def analyze():
    """
    Accepts: { "text": "compliance description..." }
    Returns: { "risk_level": "HIGH|MEDIUM|LOW", "summary": "..." }
    """
    body = request.get_json(silent=True) or {}
    text = body.get("text", "").strip()

    if not text:
        return jsonify({"error": "text field is required"}), 400

    text_lower = text.lower()
    if any(w in text_lower for w in ("overdue", "breach", "violation", "critical")):
        risk = "HIGH"
    elif any(w in text_lower for w in ("pending", "review", "upcoming")):
        risk = "MEDIUM"
    else:
        risk = "LOW"

    return jsonify({
        "risk_level": risk,
        "summary": f"Analyzed {len(text.split())} words. Risk assessed as {risk}."
    }), 200


if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5000))
    app.run(host="127.0.0.1", port=port)
