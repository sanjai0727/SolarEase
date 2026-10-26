from pydantic import BaseModel, Field

class ChatQueryRequest(BaseModel):
    query: str = Field(..., min_length=1, max_length=1000, description="The user's query (max 1000 characters)")
    mode: str = Field(..., pattern=r"^(citizen|student|professional|engineering)$", description="Response mode: citizen, student, professional, or engineering")
    language: str = Field(..., pattern=r"^(en|ta|tamil)$", description="Response language: en or ta")

class ChatResponse(BaseModel):
    answer: str
    source_question_id: str | None = None
