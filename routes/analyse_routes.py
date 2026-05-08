from flask import Blueprint, request, jsonify
from services.groq_client import analyse_document

analyse_bp = Blueprint("analyse", __name__)

@analyse_bp.route("/analyse-document", methods=["POST"])
def analyse():
    data = request.get_json()

    
    if not isinstance(data, dict) or "text" not in data:
        return jsonify({"error": "Please enter valid input"}), 400

    user_input = data["text"]   

    
    if not isinstance(user_input, str) or not user_input.strip():
        return jsonify({"error": "Please enter valid input"}), 400

    
    result = analyse_document(user_input)

    return jsonify({
        "input": user_input,
        "result": result
    })