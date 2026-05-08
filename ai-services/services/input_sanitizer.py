import re
from flask import request, jsonify

# Patterns for malicious input
SUSPICIOUS_PATTERNS = [
    r"<script.*?>.*?</script>",   # XSS
    r"ignore instructions",       # prompt injection
    r"reveal.*secret",            # data leak attempts
    r"drop table",                # SQL injection
    r"--",                        # SQL comment
]

def is_malicious(text):
    text = text.lower()
    for pattern in SUSPICIOUS_PATTERNS:
        if re.search(pattern, text):
            return True
    return False

def sanitize_input():

    # ✅ Allow GET requests (ZAP uses GET)
    if request.method == "GET":
        return None

    data = request.get_json(silent=True)

    if not data:
        return jsonify({"error": "Invalid input"}), 400

    for key, value in data.items():
        if isinstance(value, str):

            if is_malicious(value):
                return jsonify({
                    "error": "Malicious input detected"
                }), 400

            if len(value.strip()) < 5:
                return jsonify({
                    "error": "Invalid input",
                    "message": "Input too short"
                }), 400

            clean_value = re.sub(r"<.*?>", "", value)
            data[key] = clean_value

    return None