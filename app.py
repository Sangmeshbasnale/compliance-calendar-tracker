from flask import Flask, jsonify
from routes.health_routes import health_bp
from routes.describe_routes import describe_bp
from routes.recommend_routes import recommend_bp
from routes.report_routes import report_bp
from routes.analyse_routes import analyse_bp



app = Flask(__name__)

app.register_blueprint(analyse_bp)
app.register_blueprint(health_bp)
app.register_blueprint(describe_bp)
app.register_blueprint(recommend_bp)
app.register_blueprint(report_bp)

@app.route("/")
def home():
    return jsonify({"message": "AI Service Running"})

if __name__ == "__main__":
    app.run(port=5000, debug=True)