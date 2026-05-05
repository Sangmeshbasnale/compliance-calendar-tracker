# Frontend

React frontend for the Compliance Calendar Tracker.

## Setup

```bash
npm create vite@latest . -- --template react
npm install
npm run dev
```

## Environment Variables

Create a `.env` file in this directory:

```
VITE_API_URL=http://localhost:8080
VITE_AI_URL=http://localhost:5000
```

## Docker

The `Dockerfile` in this directory builds the React app and serves it via nginx.
The `nginx.conf` proxies `/api/` and `/auth/` to the Spring Boot backend and `/ai/` to the Flask AI service.
