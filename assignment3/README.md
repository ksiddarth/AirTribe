# Teamboard API (Assignment 3)

Django REST API for company signup/login, knowledge-base search, and admin usage statistics.

## Prerequisites

- Python 3.12+
- PostgreSQL 18+ (or Docker for the bundled database)
- `pip` and a virtual environment

## Initial setup

From the `assignment3` directory:

```bash
python -m venv .venv

# Windows
.venv\Scripts\activate

# macOS / Linux
source .venv/bin/activate

pip install -r requirements.txt
```

Create a `.env` file in the project root (same folder as `manage.py`):

```env
DB_PASSWORD=your_database_password
```

`settings.py` reads the database password from this file. Do not commit `.env` to version control.

---

## Database setup

The app expects a PostgreSQL database with these values (configured in `teamboard/settings.py`):

| Setting  | Value            |
|----------|------------------|
| Database | `teamboard_db`   |
| User     | `teamboard_user` |
| Host     | `localhost`      |
| Port     | `5432`           |
| Password | from `.env`      |

### Option A: Docker (recommended)

A `docker-compose.yml` file is included. Start PostgreSQL with:

```bash
docker compose up -d
```

The compose file sets `POSTGRES_PASSWORD=teamboard_password`, so your `.env` should contain:

```env
DB_PASSWORD=teamboard_password
```

Check that the database is healthy:

```bash
docker compose ps
```

### Option B: Local PostgreSQL

If PostgreSQL is installed locally, create the database and user manually:

```sql
CREATE USER teamboard_user WITH PASSWORD 'your_database_password';
CREATE DATABASE teamboard_db OWNER teamboard_user;
GRANT ALL PRIVILEGES ON DATABASE teamboard_db TO teamboard_user;
```

Set the same password in `.env`:

```env
DB_PASSWORD=your_database_password
```

---

## Apply migrations

With the database running and `.env` configured, create the tables:

```bash
python manage.py migrate
```

To inspect migration status:

```bash
python manage.py showmigrations
```

To generate new migrations after model changes:

```bash
python manage.py makemigrations
python manage.py migrate
```

---

## Run the server

```bash
python manage.py runserver
```

The API is available at `http://localhost:8000/`.

| Endpoint                    | Method | Description              |
|-----------------------------|--------|--------------------------|
| `/api/auth/signup/`         | POST   | Register a company user  |
| `/api/auth/login/`          | POST   | Log in and get JWT       |
| `/api/kb/query/`            | POST   | Search the knowledge base|
| `/api/admin/usage-summary/` | GET    | Admin usage statistics   |

Use `Assignment3.postman_collection.json` for example requests.

---

## Seed KB entries

Knowledge-base entries (`KBEntry`) power the search endpoint. They are not created automatically — load sample data after migrations.

### Using Django shell

```bash
python manage.py shell
```

Then run:

```python
from teamboard.models import KBEntry

entries = [
    {
        "question": "How do I authenticate API requests?",
        "answer": "Send a Bearer JWT in the Authorization header.",
        "category": "api",
    },
    {
        "question": "What database does Teamboard use?",
        "answer": "Teamboard uses PostgreSQL.",
        "category": "database",
    },
    {
        "question": "How do I deploy to the cloud?",
        "answer": "Package the app and deploy it to your cloud provider.",
        "category": "cloud",
    },
    {
        "question": "Which framework is this project built with?",
        "answer": "This API is built with Django and Django REST Framework.",
        "category": "framework",
    },
    {
        "question": "What is a knowledge base?",
        "answer": "A searchable collection of questions and answers.",
        "category": "general",
    },
]

KBEntry.objects.bulk_create([KBEntry(**entry) for entry in entries])
print(f"Seeded {KBEntry.objects.count()} KB entries")
```

Valid `category` values: `api`, `database`, `cloud`, `framework`, `general`.

### Verify seeded data

```bash
python manage.py shell -c "from teamboard.models import KBEntry; print(KBEntry.objects.count())"
```

Or search via the API after signing up and logging in:

```http
POST /api/kb/query/
Authorization: Bearer <your_jwt_token>
Content-Type: application/json

{
  "search": "authenticate API"
}
```
