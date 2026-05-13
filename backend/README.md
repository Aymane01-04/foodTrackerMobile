# FoodTracker Backend

A clean FastAPI + MongoDB backend for Android apps. This server verifies Firebase Authentication tokens, stores users and user data in MongoDB, and returns a short-lived JWT for app authorization.

## Project Structure

- `app/main.py` — FastAPI application entrypoint
- `app/database.py` — MongoDB connection setup
- `app/models/` — Pydantic models for MongoDB documents
- `app/schemas/` — Request and response validation schemas
- `app/routes/` — API routers for auth and data
- `app/services/` — Business logic and Firebase / JWT utilities

## Requirements

- Python 3.11+
- MongoDB
- Firebase Authentication

## Setup

1. Create a virtual environment:

```powershell
cd c:\Users\lenovo b\AndroidStudioProjects\FoodTracker_ayman\backend
python -m venv venv
venv\Scripts\Activate.ps1
```

2. Install dependencies:

```powershell
python -m pip install -r requirements.txt
```

3. Create `.env` from `.env.example` and update values:

```powershell
copy .env.example .env
```

4. Run the app:

```powershell
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

## API Endpoints

### Auth

#### POST /auth/login

Request body:

```json
{
  "firebase_token": "<firebase_id_token>"
}
```

Response:

```json
{
  "access_token": "eyJhbGciOi...",
  "token_type": "bearer"
}
```

#### GET /auth/me

Headers:

```http
Authorization: Bearer <access_token>
```

Response:

```json
{
  "id": "647c6cbf...",
  "firebase_uid": "abc123",
  "email": "user@example.com",
  "display_name": "Example User",
  "photo_url": "https://...",
  "created_at": "2026-05-06T12:00:00"
}
```

### Data

#### POST /data

Create a new data record:

```json
{
  "title": "Morning run",
  "description": "Ran 5 km near the park",
  "latitude": 40.7128,
  "longitude": -74.0060,
  "metadata": {"pace": "5:20"}
}
```

#### GET /data/user/{user_id}

Retrieve all records for the authenticated user.

#### GET /data/{data_id}

Retrieve a single record by its MongoDB id.

#### PUT /data/{data_id}

Update fields:

```json
{
  "description": "Updated description",
  "metadata": {"weather": "sunny"}
}
```

#### DELETE /data/{data_id}

Delete a record by id.

## Notes

- The backend is intentionally simple and easy to adapt for custom Android projects.
- Use the Firebase ID token from your Android client to log in and obtain the backend JWT.
- Data endpoints require `Authorization: Bearer <access_token>`.
