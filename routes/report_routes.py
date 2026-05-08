from flask import Blueprint, request, jsonify, Response
from services.async_service import process_report_async
from services.groq_client import stream_report
from datetime import datetime
import uuid
import threading

report_bp = Blueprint("report", __name__)

# in-memory store + lock
stored_reports = {}
lock = threading.Lock()



@report_bp.route("/generate-report-stream", methods=["GET"])
def generate_stream():
    user_input = request.args.get("text")

    if not user_input:
        return jsonify({"error": "text is required"}), 400

    def event_stream():
        buffer = ""

        try:
            for token in stream_report(user_input):
                buffer += token

                # flush on space/newline to avoid broken words
                if token.endswith(" ") or token.endswith("\n"):
                    yield f"data: {buffer}\n\n"
                    buffer = ""

            # flush remaining
            if buffer:
                yield f"data: {buffer}\n\n"

            # signal end (useful for React)
            yield "event: end\ndata: done\n\n"

        except Exception as e:
            yield f"data: ERROR: {str(e)}\n\n"

    return Response(
        event_stream(),
        mimetype="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
        },
    )



@report_bp.route("/generate-report", methods=["POST"])
def generate():
    data = request.get_json()

    if not isinstance(data, dict) or "text" not in data:
        return jsonify({"error": "Please enter valid input"}), 400

    user_input = data["text"]
    report_id = str(uuid.uuid4())

    with lock:
        stored_reports[report_id] = {
            "status": "PENDING",
            "data": None,
            "created_at": datetime.utcnow().isoformat(),
        }

   
    process_report_async(report_id, user_input, stored_reports, lock)

    return jsonify({
        "report_id": report_id,
        "status": "PENDING"
    })



@report_bp.route("/report/<report_id>", methods=["GET"])
def get_report(report_id):
    with lock:
        report = stored_reports.get(report_id)

    if not report:
        return jsonify({"error": "Report not found"}), 404

    return jsonify(report)