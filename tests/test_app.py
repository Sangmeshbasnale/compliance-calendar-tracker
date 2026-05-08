import sys
import os

# Fix import path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

import pytest
from app import app


@pytest.fixture
def client():
    app.config["TESTING"] = True
    return app.test_client()



def mock_ai_response(*args, **kwargs):
    return {
        "findings": [
            {"type": "insight", "description": "Test insight"},
            {"type": "risk", "description": "Test risk"}
        ]
    }



def test_describe_success(client, monkeypatch):
    monkeypatch.setattr(
        "routes.describe_routes.get_ai_response",
        lambda x: "Test response"
    )

    res = client.post("/describe", json={"text": "hello"})
    assert res.status_code == 200
    assert "Test response" in res.get_data(as_text=True)



def test_describe_invalid(client):
    res = client.post("/describe", json={})
    assert res.status_code == 400



def test_recommend_success(client, monkeypatch):
    monkeypatch.setattr(
        "routes.recommend_routes.get_recommendations",
        lambda x: {"data": "ok"}
    )

    res = client.post("/recommend", json={"text": "test"})
    assert res.status_code == 200



def test_generate_report(client):
    res = client.post("/generate-report", json={"text": "test"})
    data = res.get_json()

    assert res.status_code == 200
    assert data["status"] == "PENDING"
    assert "report_id" in data


def test_get_report_not_found(client):
    res = client.get("/report/invalid-id")
    assert res.status_code == 404



def test_analyse_success(client, monkeypatch):
    monkeypatch.setattr(
        "routes.analyse_routes.analyse_document",
        lambda x: mock_ai_response()
    )

    res = client.post("/analyse-document", json={"text": "test"})
    data = res.get_json()

    assert res.status_code == 200
    assert "findings" in data["result"]



def test_analyse_invalid(client):
    res = client.post("/analyse-document", json={})
    assert res.status_code == 400



def test_empty_input(client):
    res = client.post("/analyse-document", json={"text": ""})
    assert res.status_code == 400



def test_groq_error(client, monkeypatch):
    monkeypatch.setattr(
        "routes.analyse_routes.analyse_document",
        lambda x: {"error": "fail"}
    )

    res = client.post("/analyse-document", json={"text": "test"})
    data = res.get_json()

    assert "error" in data["result"]



def test_report_flow(client):
    res = client.post("/generate-report", json={"text": "test"})
    report_id = res.get_json()["report_id"]

    res2 = client.get(f"/report/{report_id}")
    assert res2.status_code == 200