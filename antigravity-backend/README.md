# SolarEase AntiGravity Backend

This is the FastAPI backend foundation for the SolarEase project. It uses a Modular Monolith architecture.

## Tech Stack
- Python 3.13+
- FastAPI
- SQLAlchemy 2.0
- Alembic
- PostgreSQL (via psycopg)

## Setup

1. Create a virtual environment:
   ```bash
   python -m venv venv
   ```
2. Activate the virtual environment:
   - Windows: `.\venv\Scripts\activate`
   - Linux/Mac: `source venv/bin/activate`
3. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```
4. Configure environment:
   Copy `.env.example` to `.env` and update the `DATABASE_URL`.
   ```bash
   cp .env.example .env
   ```

## Running the Application

```bash
uvicorn src.main:app --reload
```

## Database Migrations

Generate a new migration (after modifying models):
```bash
alembic revision --autogenerate -m "Description of changes"
```

Apply migrations:
```bash
alembic upgrade head
```

## Architecture
- `src/main.py`: Application entry point.
- `src/core/`: Shared configuration, database setup, middleware, and logging.
- `src/api/`: Main router aggregation.
- `src/modules/`: Feature-first modular monolith directories.
