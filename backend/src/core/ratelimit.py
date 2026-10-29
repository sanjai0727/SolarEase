import time
import hashlib
from collections import defaultdict
from fastapi import HTTPException, Request, status

class SimpleRateLimiter:
    """
    In-memory sliding window rate limiter per key (user ID or client IP).
    Thread-safe enough for async workers within a single process.
    Features periodic cleanup and empty key eviction to prevent memory leaks.
    """
    def __init__(self, requests_per_minute: int = 30, max_tracked_keys: int = 10000):
        self.requests_per_minute = requests_per_minute
        self.max_tracked_keys = max_tracked_keys
        self.history = defaultdict(list)
        self.last_cleanup = time.time()

    def _cleanup_stale_keys(self, now: float, window_start: float):
        """Periodically purge all empty or expired keys across the whole table."""
        if now - self.last_cleanup < 60.0:
            return
        self.last_cleanup = now
        stale_keys = [k for k, timestamps in self.history.items() if not timestamps or timestamps[-1] <= window_start]
        for k in stale_keys:
            self.history.pop(k, None)

    def check(self, key: str):
        now = time.time()
        window_start = now - 60.0

        # Periodic cleanup of global history
        self._cleanup_stale_keys(now, window_start)

        # Prune older timestamps for the requested key
        active_timestamps = [ts for ts in self.history[key] if ts > window_start]
        if not active_timestamps:
            self.history.pop(key, None)
        else:
            self.history[key] = active_timestamps

        current_count = len(self.history.get(key, []))
        if current_count >= self.requests_per_minute:
            raise HTTPException(
                status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                detail=f"Rate limit exceeded. Maximum {self.requests_per_minute} requests per minute allowed. Please try again shortly.",
                headers={"Retry-After": "60"}
            )

        # Bound maximum tracked keys to prevent DoS via infinite unique keys
        if len(self.history) >= self.max_tracked_keys and key not in self.history:
            raise HTTPException(
                status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                detail="System busy. Please try again shortly.",
                headers={"Retry-After": "60"}
            )

        self.history[key].append(now)

chatbot_limiter = SimpleRateLimiter(requests_per_minute=20)
lens_limiter = SimpleRateLimiter(requests_per_minute=10)

def _extract_client_key(prefix: str, request: Request) -> str:
    auth_header = request.headers.get("Authorization", "").strip()
    if auth_header:
        # Secure: Full cryptographic digest ensures unique buckets per user/token
        token_hash = hashlib.sha256(auth_header.encode("utf-8")).hexdigest()[:32]
        return f"{prefix}:token:{token_hash}"

    # Unauthenticated client IP resolution
    forwarded = request.headers.get("X-Forwarded-For")
    if forwarded:
        client_ip = forwarded.split(",")[0].strip()
    elif request.client:
        client_ip = request.client.host
    else:
        client_ip = "unknown"

    return f"{prefix}:ip:{client_ip}"

def rate_limit_chatbot(request: Request):
    key = _extract_client_key("chatbot", request)
    chatbot_limiter.check(key)

def rate_limit_lens(request: Request):
    key = _extract_client_key("lens", request)
    lens_limiter.check(key)
