from fastapi import APIRouter
from src.modules.chatbot.schemas import ChatQueryRequest, ChatResponse
from src.modules.chatbot.service import get_chatbot_response
from src.modules.chatbot.retrieval import load_index

# Load the in-memory index on startup
load_index()

router = APIRouter(prefix="/chatbot", tags=["Chatbot"])

@router.post("/query", response_model=ChatResponse)
def query_chatbot(request: ChatQueryRequest):
    result = get_chatbot_response(request.query, request.mode, request.language)
    return ChatResponse(
        answer=result["answer"],
        source_question_id=result.get("source_question_id")
    )
