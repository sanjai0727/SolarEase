import json
import os
import sys
from google import genai
import time

# Add the project root to sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "../../../")))
from src.core.config import settings

def build_mini_index():
    print("Building mini embedding index for tests...")
    index_path = os.path.join(os.path.dirname(__file__), "embeddings_index.json")
    
    # Selected items based on the required tests
    mini_data = [
        {
            "id": 1,
            "source_question_id": "Q1",
            "topic": {"en": "Electricity Fundamentals", "ta": "மின்சார அடிப்படைகள்"},
            "question": {"en": "What is electricity?", "ta": "electricity என்றால் என்ன?"},
            "answers": {"citizen": {"en": "Explain electricity simply.", "ta": "எளிய விளக்கம்."}}
        },
        {
            "id": 10,
            "source_question_id": "Q10",
            "topic": {"en": "Electricity Fundamentals", "ta": "மின்சார அடிப்படைகள்"},
            "question": {"en": "Why has my electricity bill increased?", "ta": "என் மின் கட்டணம் ஏன் அதிகரித்துள்ளது?"},
            "answers": {"citizen": {"en": "Explain bills.", "ta": "கட்டணம் விளக்கம்."}}
        },
        {
            "id": 100,
            "source_question_id": "Q100",
            "topic": {"en": "Solar+BESS+EV Integration", "ta": "Solar+BESS+EV ஒருங்கிணைப்பு"},
            "question": {"en": "Can solar panels charge an EV using battery storage?", "ta": "சோலார் பேனல்கள் பேட்டரி சேமிப்பகத்தைப் பயன்படுத்தி EV-ஐ சார்ஜ் செய்ய முடியுமா?"},
            "answers": {"professional": {"en": "Evaluate Solar-EV-BESS integration.", "ta": "ஒருங்கிணைப்பு மதிப்பீடு."}}
        }
    ]
    
    api_key = settings.GEMINI_API_KEY
    client = genai.Client(api_key=api_key)
    
    indexed_data = []
    for item in mini_data:
        topic_en = item.get("topic", {}).get("en", "")
        topic_ta = item.get("topic", {}).get("ta", "")
        q_en = item.get("question", {}).get("en", "")
        q_ta = item.get("question", {}).get("ta", "")
        
        combined_text = f"Topic: {topic_en} | {topic_ta}. Question: {q_en} | {q_ta}"
        
        try:
            response = client.models.embed_content(
                model='gemini-embedding-2',
                contents=combined_text
            )
            embedding_vector = response.embeddings[0].values
            indexed_data.append({
                "id": item.get("id"),
                "source_question_id": item.get("source_question_id"),
                "topic": item.get("topic"),
                "question": item.get("question"),
                "answers": item.get("answers"),
                "embedding": embedding_vector
            })
            time.sleep(1)
        except Exception as e:
            print(f"Error: {e}")
            
    with open(index_path, "w", encoding="utf-8") as f:
        json.dump(indexed_data, f, ensure_ascii=False)
        
    print(f"Successfully generated mini index with {len(indexed_data)} items.")

if __name__ == "__main__":
    build_mini_index()
