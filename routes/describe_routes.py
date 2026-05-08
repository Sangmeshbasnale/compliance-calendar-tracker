from flask import Blueprint, request, jsonify
from services.groq_client import get_ai_response, get_recommendations
from datetime import datetime


describe_bp = Blueprint("describe", __name__)

@describe_bp.route("/describe", methods=["POST"])
def describe():
    data = request.get_json()

    if not isinstance(data, dict):
        return jsonify({"error": "Please enter valid input"}), 400

    if "text" not in data:
        return jsonify({"error": "Please enter valid input"}), 400

    user_input = data.get("text")

    if not isinstance(user_input, str) or not user_input.strip():
        return jsonify({"error": "Please enter valid input"}), 400

    result = get_ai_response(user_input)

    return jsonify({
        "input": user_input,
        "response": result,
        "generated_at": datetime.utcnow().isoformat()
    })



recommend_bp = Blueprint("recommend", __name__)

@recommend_bp.route("/recommend", methods=["POST"])
def recommend():
    data = request.get_json()

    if not isinstance(data, dict):
        return jsonify({"error": "Please enter valid input"}), 400

    if "text" not in data:
        return jsonify({"error": "Please enter valid input"}), 400

    user_input = data.get("text")

    if not isinstance(user_input, str) or not user_input.strip():
        return jsonify({"error": "Please enter valid input"}), 400

    result = get_recommendations(user_input)

    return jsonify({
        "input": user_input,
        "recommendations": result,
        "generated_at": datetime.utcnow().isoformat()
    })