from pydantic import BaseModel, Field

class ChatQueryRequest(BaseModel):
    query: str = Field(..., description="The user's query")
    mode: str = Field(..., description="Response mode: citizen, student, professional, or engineering")
    language: str = Field(..., description="Response language: en or ta")

class ChatResponse(BaseModel):
    answer: str
    source_question_id: str | None = None
