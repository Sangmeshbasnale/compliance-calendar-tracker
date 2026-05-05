# Slim Python 3.12 image
FROM python:3.12-slim

# Non-root user
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

WORKDIR /app

# Install dependencies before copying source — better layer caching
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app.py .
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 5000

# gunicorn: 2 workers, bind all interfaces, 120 s timeout
CMD ["gunicorn", "--workers", "2", "--bind", "0.0.0.0:5000", "--timeout", "120", "app:app"]
