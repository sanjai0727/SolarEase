import time
from collections import defaultdict
from fastapi import HTTPException, Request, status

class SimpleRateLimiter:
    """
    In-memory sliding window rate limiter per key (user ID or client IP).
    Thread-safe enough for async workers within a single process.
    """
    def __init__(self, requests_per_minute: int = 30):
        self.requests_per_minute = requests_per_minute
        self.history = defaultdict(list)

    def check(self, key: str):
        now = time.time()
        window_start = now - 60.0

        # Prune older timestamps
        self.history[key] = [ts for ts in self.history[key] if ts > window_start]

        if len(self.history[key]) >= self.requests_per_minute:
            raise HTTPException(
                status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                detail=f"Rate limit exceeded. Maximum {self.requests_per_minute} requests per minute allowed. Please try again shortly.",
                headers={"Retry-After": "60"}
            )

        self.history[key].append(now)

chatbot_limiter = SimpleRateLimiter(requests_per_minute=20)
lens_limiter = SimpleRateLimiter(requests_per_minute=10)

def rate_limit_chatbot(request: Request):
    # Extract client IP or forward header
    forwarded = request.headers.get("X-Forwarded-For")
    client_ip = forwarded.split(",")[0].strip() if forwarded else (request.client.host if request.client else "unknown")
    auth_header = request.headers.get("Authorization", "")
    key = f"chatbot:{auth_header[:32] if auth_header else client_ip}"
    chatbot_limiter.check(key)

def rate_limit_lens(request: Request):
    forwarded = request.headers.get("X-Forwarded-For")
    client_ip = forwarded.split(",")[0].strip() if forwarded else (request.client.host if request.client else "unknown")
    auth_header = request.headers.get("Authorization", "")
    key = f"lens:{auth_header[:32] if auth_header else client_ip}"
    lens_limiter.check(key)
