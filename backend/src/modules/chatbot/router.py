from fastapi import APIRouter, Depends
from src.modules.chatbot.schemas import ChatQueryRequest, ChatResponse
from src.modules.chatbot.service import get_chatbot_response
from src.modules.chatbot.retrieval import load_index
from src.modules.auth.dependencies import get_current_user, verify_app_check
from src.modules.auth.models import User
from src.core.ratelimit import rate_limit_chatbot

# Load the in-memory index on startup
load_index()

router = APIRouter(prefix="/chatbot", tags=["Chatbot"])

@router.post("/query", response_model=ChatResponse)
def query_chatbot(
    request: ChatQueryRequest,
    current_user: User = Depends(get_current_user),
    _app_check: dict | None = Depends(verify_app_check),
    _rate_limit: None = Depends(rate_limit_chatbot)
):
    result = get_chatbot_response(request.query, request.mode, request.language)
    return ChatResponse(
        answer=result["answer"],
        source_question_id=result.get("source_question_id")
    )
