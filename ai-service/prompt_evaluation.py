import os
import json
from groq import Groq
from dotenv import load_dotenv

# Load environment
load_dotenv()
client = Groq(api_key=os.getenv("GROQ_API_KEY"))

# -------------------------------
# TEST INPUTS
# -------------------------------
test_inputs = [
    "I am investing in stocks",
    "I am learning machine learning",
    "Watching movies is fun",
    "I have a headache and fever",
    "Bank interest rates increased",
    "Python programming is powerful",
    "I love playing football",
    "Education is important",
    "AI is transforming industries",
    "I need medical advice"
]

# Expected outputs
expected_categories = [
    "Finance",
    "Technology",
    "Entertainment",
    "Health",
    "Finance",
    "Technology",
    "Entertainment",
    "Education",
    "Technology",
    "Health"
]

# Valid categories
VALID_CATEGORIES = ["Finance", "Health", "Technology", "Education", "Entertainment", "Other"]


# -------------------------------
# VALIDATION
# -------------------------------
def validate_category(data):
    return data.get("category") in VALID_CATEGORIES


# -------------------------------
# SCORING FUNCTION
# -------------------------------
def score_response(response_text, expected_category):
    score = 0
    format_ok = False

    try:
        data = json.loads(response_text)
        format_ok = True

        # Format correctness
        score += 4

        # Valid category
        if validate_category(data):
            score += 2

        # Correct category
        if data.get("category") == expected_category:
            score += 4

    except Exception:
        pass

    return score, format_ok


# -------------------------------
# RUN EVALUATION
# -------------------------------
results = []

for i, text in enumerate(test_inputs):

    # ✅ SAFE PROMPT (NO .format)
    prompt = f"""
You are a strict text classifier.

Classify the text into EXACTLY one category:
Finance, Health, Technology, Education, Entertainment, Other

Rules:
- Output MUST be valid JSON only
- No extra text before or after JSON
- Category must match EXACTLY one from the list
- Confidence must be between 0 and 1

Text: "{text}"

Return JSON:
{{
  "category": "...",
  "confidence": 0.0,
  "reasoning": "short explanation"
}}
"""

    try:
        # First attempt
        response = client.chat.completions.create(
            model="llama-3.1-8b-instant",
            messages=[{"role": "user", "content": prompt}]
        )

        output = response.choices[0].message.content
        score, format_ok = score_response(output, expected_categories[i])

        # -------------------------------
        # RETRY IF SCORE < 7
        # -------------------------------
        if score < 7:
            print(f"\nRetrying for input: {text}")

            retry_prompt = f"""
Your previous response was incorrect.

Follow STRICT rules:
- Return ONLY valid JSON
- Choose correct category from:
[Finance, Health, Technology, Education, Entertainment, Other]

Text: "{text}"

Return:
{{
  "category": "...",
  "confidence": 0.0-1.0,
  "reasoning": "short explanation"
}}
"""

            retry_response = client.chat.completions.create(
                model="llama-3.1-8b-instant",
                messages=[{"role": "user", "content": retry_prompt}]
            )

            output = retry_response.choices[0].message.content
            score, format_ok = score_response(output, expected_categories[i])

        results.append({
            "input": text,
            "output": output,
            "score": score,
            "format_ok": format_ok
        })

    except Exception as e:
        results.append({
            "input": text,
            "output": str(e),
            "score": 0,
            "format_ok": False
        })


# -------------------------------
# PRINT REPORT
# -------------------------------
print("\n===== PROMPT EVALUATION REPORT =====\n")

low_score_cases = []

for r in results:
    print(f"Input: {r['input']}")
    print(f"Score: {r['score']}/10")
    print(f"Format OK: {r['format_ok']}")
    print(f"Output: {r['output']}")
    print("-" * 50)

    if r["score"] < 7:
        low_score_cases.append(r)


# -------------------------------
# SUMMARY
# -------------------------------
avg_score = sum(r["score"] for r in results) / len(results)

print("\n===== SUMMARY =====")
print(f"Average Score: {avg_score:.2f}/10")
print(f"Low Score Cases (<7): {len(low_score_cases)}")