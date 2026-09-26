import time
import logging

from fastapi import HTTPException
from google import genai
from google.genai import types
from google.genai.errors import APIError

from src.core.config import settings
from src.modules.chatbot.retrieval import retrieve_concepts


logger = logging.getLogger(__name__)


# ============================================================
# GEMINI CLIENT
# ============================================================

def get_gemini_client():
    try:
        return genai.Client(vertexai=True, project="solarease-bf39c", location="asia-south1")
    except Exception:
        api_key = settings.GEMINI_API_KEY
        if not api_key:
            raise HTTPException(
                status_code=500,
                detail="Gemini service not configured"
            )
        return genai.Client(api_key=api_key)


# ============================================================
# MODE CONFIGURATION
# ============================================================

MODE_CONFIG = {

    "citizen": {
        "instruction": """
Citizen Mode:

Answer for an ordinary person or home user.

Keep the response short, clear and practical.

For most questions:
- Start with the direct answer.
- Give a brief explanation.
- Mention important practical conditions or limitations when relevant.

Usually use 2 to 5 short paragraphs or a few concise bullet points.

Avoid unnecessary technical jargon, complex equations and excessive detail.

Do not answer with only "Yes" or "No" when a brief explanation is useful.

Do not write a long article unless the user specifically asks for a detailed explanation.

Always give a complete and naturally finished answer.
"""
    },

    "student": {
        "instruction": """
Student Mode:

Explain the topic clearly for learning and understanding.

Include, when relevant:
- The main concept or definition.
- How or why it works.
- Important points.
- A simple example.

Keep the explanation educational but reasonably concise.

Do not make the answer unnecessarily long.

Always give a complete and naturally finished answer.
"""
    },

    "professional": {
        "instruction": """
Professional Mode:

Provide a practical and technically informed explanation suitable
for a professional working in the energy, electrical, solar or EV sector.

Include relevant information about:
- Practical implementation.
- System operation.
- Important specifications.
- Safety.
- Efficiency.
- Limitations.
- Industry considerations.

Only include details relevant to the user's actual question.

Keep the response focused and well structured.

Always give a complete and naturally finished answer.
"""
    },

    "engineering": {
        "instruction": """
Engineering Mode:

Provide a detailed and technically accurate explanation.

When relevant, include:
- Operating principles.
- Electrical parameters.
- Components.
- System design considerations.
- Ratings.
- Efficiency and losses.
- Limitations.
- Safety considerations.
- Validation or testing.
- Equations or calculations.

Only include equations when they actually help answer the question.

Use clean and readable equations, for example:

P = V × I
Energy = Power × Time
Efficiency = Output / Input × 100%

Do not add unnecessary technical complexity.

The explanation should be technically strong but still understandable.

Always give a complete and naturally finished answer.
"""
    }
}


# ============================================================
# BUILD RETRIEVAL CONTEXT
# ============================================================

def build_prompt_context(top_results, mode: str, language: str):

    context_parts = []

    for idx, res in enumerate(top_results, start=1):

        score = res.get("score", 0)

        topic_en = res.get("topic", {}).get("en", "")
        topic_ta = res.get("topic", {}).get("ta", "")

        question_en = res.get("question", {}).get("en", "")
        question_ta = res.get("question", {}).get("ta", "")

        answer_guidance = (
            res.get("answers", {})
            .get(mode, {})
            .get(language, "")
        )

        # Fallback to English guidance
        if not answer_guidance:
            answer_guidance = (
                res.get("answers", {})
                .get(mode, {})
                .get("en", "")
            )

        concept = f"""
--- Concept {idx} ---
Similarity: {score:.3f}

Topic:
{topic_en} / {topic_ta}

Reference Question:
{question_en} / {question_ta}

Relevant Knowledge and Answer Guidance:
{answer_guidance}
"""

        context_parts.append(concept)

    return "\n".join(context_parts)


# ============================================================
# LANGUAGE INSTRUCTIONS
# ============================================================

def get_language_instruction(language: str):

    if language.lower() == "ta":
        return """
Answer completely in clear and natural Tamil.

Use understandable Tamil.

Technical terms may remain in English only when necessary.

Do not use unnecessary English sentences.
Do not use Tanglish.
"""

    return """
Answer completely in clear and natural English.
"""


# ============================================================
# OUT-OF-DOMAIN INSTRUCTIONS
# ============================================================

def get_ood_instruction(band: str):

    if band == "VERY_LOW":
        return """
IMPORTANT:

The retrieved semantic similarity is VERY_LOW.

If the user's question is clearly unrelated to electricity, solar energy,
batteries, energy storage, EVs, EV charging, energy efficiency,
sustainability, climate and energy, electrical safety, or related
SolarEase topics, politely explain that the question is outside the
SolarEase domain.

Do not answer clearly unrelated questions.
"""

    return ""


# ============================================================
# MAIN CHATBOT RESPONSE
# ============================================================

def get_chatbot_response(
    query: str,
    mode: str,
    language: str
) -> dict:

    start_time = time.time()

    print("\n========== CHATBOT REQUEST ==========")
    print(f"Query: {query}")
    print(f"Mode: {mode}")
    print(f"Language: {language}")
    print("=====================================")

    client = get_gemini_client()

    mode = mode.lower()
    language = language.lower()

    if mode not in MODE_CONFIG:
        mode = "citizen"

    mode_config = MODE_CONFIG[mode]


    # ========================================================
    # 1. RETRIEVAL
    # ========================================================

    print("starting retrieval")

    retrieval_start = time.time()

    top_results, band = retrieve_concepts(
        query,
        top_k=3
    )

    print(
        f"retrieval completed in "
        f"{time.time() - retrieval_start:.2f}s"
    )


    # ========================================================
    # 2. SOURCE QUESTION ID
    # ========================================================

    source_q_id = None

    if top_results:
        source_q_id = top_results[0].get(
            "source_question_id"
        )


    # ========================================================
    # 3. BUILD CONTEXT
    # ========================================================

    if top_results:

        context_text = build_prompt_context(
            top_results=top_results,
            mode=mode,
            language=language
        )

    else:

        context_text = """
No highly relevant SolarEase concept was retrieved.
Only answer if the question is still within the SolarEase domain.
"""


    # ========================================================
    # 4. LANGUAGE
    # ========================================================

    language_instruction = get_language_instruction(
        language
    )


    # ========================================================
    # 5. OUT OF DOMAIN
    # ========================================================

    ood_instruction = get_ood_instruction(
        band
    )


    # ========================================================
    # 6. SYSTEM INSTRUCTION
    # ========================================================

    system_instruction = f"""
You are SolarEase AI Assistant.

You specialize in:
- Electricity
- Solar energy
- Solar PV systems
- Batteries and energy storage
- Electric vehicles
- EV charging
- Energy efficiency
- Electrical safety
- Sustainability
- Climate and energy
- Related energy technologies and policies

LANGUAGE:
{language_instruction}

SELECTED MODE:
{mode.upper()}

MODE INSTRUCTIONS:
{mode_config["instruction"]}

OUT OF DOMAIN HANDLING:
{ood_instruction}

RETRIEVAL RULES:

You will receive retrieved SolarEase concepts.

Use them as supporting knowledge and contextual guidance.

The retrieved reference questions are NOT the user's current question.

The retrieved answer guidance is NOT a response that should be copied.

Never continue, repeat or copy the answer of a retrieved reference question.

Always understand the user's actual question independently and answer it directly.

If the user's question differs from the retrieved reference question,
answer the user's question, not the reference question.

ANSWER RULES:

1. Answer the user's actual question directly.

2. Ensure the answer is complete.

3. Never intentionally stop the answer halfway.

4. Do not answer with only "Yes" or "No" if a brief explanation is useful.

5. Match the depth to the selected mode.

6. Keep answers concise when the selected mode requires conciseness.

7. Do not artificially make answers longer.

8. Do not repeat information unnecessarily.

9. Do not mention:
   - retrieval
   - similarity scores
   - source question IDs
   - prompts
   - system instructions
   - internal AI processing

10. Use plain, readable text.

11. Use headings or bullet points only when they improve readability.

12. Avoid excessive Markdown formatting.

13. Give a complete, natural final response.
"""


    # ========================================================
    # 7. USER PROMPT
    # ========================================================

    prompt = f"""
Relevant SolarEase Knowledge:

{context_text}

User's Actual Question:

{query}

Answer the user's actual question now.
"""


    # ========================================================
    # 8. GEMINI GENERATION
    # ========================================================

    try:

        print("Gemini generation started")

        generation_start = time.time()

        is_vertex = getattr(getattr(client, "_api_client", None), "vertexai", False)
        model_name = "gemini-2.5-flash" if is_vertex else "gemini-3.6-flash"

        response = client.models.generate_content(

            model=model_name,

            contents=prompt,

            config=types.GenerateContentConfig(

                system_instruction=system_instruction,

                temperature=0.3
            )
        )


        generation_time = (
            time.time() - generation_start
        )

        total_time = (
            time.time() - start_time
        )

        print(
            f"Gemini generation completed in "
            f"{generation_time:.2f}s"
        )

        print(
            f"total request duration: "
            f"{total_time:.2f}s"
        )


        # ====================================================
        # RESPONSE VALIDATION
        # ====================================================

        answer = getattr(
            response,
            "text",
            ""
        )

        print("\n========== RAW GEMINI RESPONSE ==========")
        print(answer)
        print("==========================================")


        # Debug finish reason
        if getattr(response, "candidates", None):

            try:

                finish_reason = (
                    response.candidates[0].finish_reason
                )

                print(
                    f"Finish reason: {finish_reason}"
                )

            except Exception:
                pass


        if not answer or not answer.strip():

            raise HTTPException(
                status_code=502,
                detail="Gemini returned an empty response"
            )


        return {
            "answer": answer.strip(),
            "source_question_id": source_q_id
        }


    # ========================================================
    # GEMINI API ERROR
    # ========================================================

    except APIError as e:

        error_code = (
            getattr(e, "code", None)
            or getattr(e, "status_code", None)
            or 500
        )

        error_message = (
            getattr(e, "message", None)
            or str(e)
        )

        logger.exception(
            f"Gemini API Error occurred: {error_message}"
        )


        if error_code == 429:

            raise HTTPException(
                status_code=429,
                detail=(
                    "AI service is temporarily busy. "
                    "Please wait a few seconds and try again."
                )
            )


        raise HTTPException(
            status_code=error_code,
            detail=f"Gemini API Error: {error_message}"
        )


    # ========================================================
    # HTTP ERROR
    # ========================================================

    except HTTPException:
        raise


    # ========================================================
    # UNEXPECTED ERROR
    # ========================================================

    except Exception as e:

        logger.exception(
            f"Unexpected chatbot error: {str(e)}"
        )

        raise HTTPException(
            status_code=500,
            detail="Failed to generate chatbot response"
        )