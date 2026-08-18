import psycopg
import sys
from psycopg.errors import DuplicateDatabase

# Connect to the default 'postgres' database to check/create 'solarease'
try:
    # Use autocommit to execute CREATE DATABASE
    with psycopg.connect("dbname=postgres user=postgres password=postgres host=localhost port=5432", autocommit=True) as conn:
        with conn.cursor() as cur:
            # Check if database exists
            cur.execute("SELECT 1 FROM pg_database WHERE datname='solarease'")
            exists = cur.fetchone()
            if not exists:
                print("Database 'solarease' does not exist. Creating it...")
                cur.execute("CREATE DATABASE solarease")
                print("Database 'solarease' created successfully.")
            else:
                print("Database 'solarease' already exists.")
except Exception as e:
    print(f"Error connecting to PostgreSQL: {e}")
    sys.exit(1)
