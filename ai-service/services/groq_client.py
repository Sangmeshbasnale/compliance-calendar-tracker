# Day 2 implementation
import os
import time
import logging
from typing import Optional

from groq import Groq
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# Logging setup
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class GroqClient:
    def __init__(self):
        api_key = os.getenv("GROQ_API_KEY")
        if not api_key:
            raise ValueError("GROQ_API_KEY not found in .env")

        self.client = Groq(api_key=api_key)

    def generate_response(self, prompt: str, retries: int = 3) -> Optional[str]:
        for attempt in range(retries):
            try:
                logger.info(f"Attempt {attempt + 1}")

                response = self.client.chat.completions.create(
                    model="llama-3.1-8b-instant",
                    messages=[
                        {"role": "system", "content": "You are a helpful assistant"},
                        {"role": "user", "content": prompt}
                    ]
                )

                result = response.choices[0].message.content
                return result

            except Exception as e:
                logger.error(f"Error: {str(e)}")

                if attempt < retries - 1:
                    wait = 2 ** attempt
                    logger.info(f"Retrying in {wait} seconds...")
                    time.sleep(wait)
                else:
                    logger.critical("All retries failed")
                    return None