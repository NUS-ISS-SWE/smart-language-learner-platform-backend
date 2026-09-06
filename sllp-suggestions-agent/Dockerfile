FROM python:3.12-slim

WORKDIR /app

COPY sllp-suggestions-agent/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY sllp-suggestions-agent/app ./app

EXPOSE 8091

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8091"]
