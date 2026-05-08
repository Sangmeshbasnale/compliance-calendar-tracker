from groq import Groq
import os
from dotenv import load_dotenv
import json

load_dotenv()

client = Groq(api_key=os.getenv("GROQ_API_KEY"))



def clean_json(content):
    if content.startswith("```"):
        content = content.replace("```json", "").replace("```", "").strip()
    return content



def get_ai_response(user_input):
    try:
        with open("prompts/describe_prompt.txt", "r") as f:
            template = f.read()

        prompt = template.replace("{user_input}", user_input)

        response = client.chat.completions.create(
            model="llama-3.1-8b-instant",
            messages=[{"role": "user", "content": prompt}]
        )

        return response.choices[0].message.content

    except Exception as e:
        return {"error": str(e)}



def get_recommendations(user_input):
    try:
        with open("prompts/recommend_prompt.txt", "r") as f:
            template = f.read()

        prompt = template.replace("{user_input}", user_input)

        response = client.chat.completions.create(
            model="llama-3.1-8b-instant",
            messages=[{"role": "user", "content": prompt}]
        )

        content = clean_json(response.choices[0].message.content.strip())

        return json.loads(content)

    except Exception as e:
        return {
            "error": "Invalid JSON from AI",
            "details": str(e)
        }



def generate_report(user_input):
    try:
        with open("prompts/report_prompt.txt", "r") as f:
            template = f.read()

        prompt = template.replace("{user_input}", user_input)

        response = client.chat.completions.create(
            model="llama-3.1-8b-instant",
            messages=[{"role": "user", "content": prompt}]
        )

        content = clean_json(response.choices[0].message.content.strip())

        return json.loads(content)

    except Exception as e:
        return {
            "error": "Invalid JSON",
            "details": str(e)
        }



def stream_report(user_input):
    try:
        with open("prompts/report_prompt.txt", "r") as f:
            template = f.read()

        prompt = template.replace("{user_input}", user_input)

        response = client.chat.completions.create(
            model="llama-3.1-8b-instant",
            messages=[{"role": "user", "content": prompt}],
            stream=True
        )

        for chunk in response:
            try:
                delta = chunk.choices[0].delta

               
                if hasattr(delta, "content") and delta.content:
                    yield delta.content

            except Exception:
                continue

    except Exception as e:
        yield f"ERROR: {str(e)}"

def analyse_document(user_input):
    try:
        prompt = f"""
You are an AI document analyst.

Strict Instructions:
- Return ONLY valid JSON
- No extra text
- Output must be:
{{
  "findings": [
    {{
      "type": "insight",
      "description": "..."
    }},
    {{
      "type": "risk",
      "description": "..."
    }}
  ]
}}

Rules:
- Identify key insights and risks from the text
- At least 3 insights and 3 risks
- Keep descriptions clear and professional

Input:
{user_input}
"""

        response = client.chat.completions.create(
            model="llama-3.1-8b-instant",
            messages=[{"role": "user", "content": prompt}]
        )

        content = response.choices[0].message.content.strip()

        if content.startswith("```"):
            content = content.replace("```json", "").replace("```", "").strip()

        return json.loads(content)

    except Exception as e:
        return {
            "error": "Invalid JSON",
            "details": str(e)
        }