import json
import os
import sys
import time
from google import genai
from google.genai import types

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "../../../")))
from src.core.config import settings

def build_index():
    data_path = r"e:\projects\SolarEase\processed_solar_qa.json"
    index_path = os.path.join(os.path.dirname(__file__), "embeddings_index.json")
    
    with open(data_path, "r", encoding="utf-8") as f:
        qa_data = json.load(f)
        
    print(f"Loaded {len(qa_data)} total items from source.")
    
    indexed_data = []
    existing_ids = set()
    if os.path.exists(index_path):
        try:
            with open(index_path, "r", encoding="utf-8") as f:
                indexed_data = json.load(f)
                existing_ids = {item.get("source_question_id") for item in indexed_data}
            print(f"Found existing index with {len(indexed_data)} items. Resuming...")
        except Exception as e:
            print(f"Warning: Could not read existing index: {e}")
            
    items_to_process = [item for item in qa_data if item.get("source_question_id") not in existing_ids]
    print(f"{len(items_to_process)} remaining items to process.")
    
    if not items_to_process:
        print("Index is already complete.")
        return

    api_key = settings.GEMINI_API_KEY
    client = genai.Client(api_key=api_key)
    
    batch_size = 100
    current_index = 0
    
    while current_index < len(items_to_process):
        chunk_items = items_to_process[current_index:current_index + batch_size]
        
        contents_payload = []
        for item in chunk_items:
            topic_en = item.get("topic", {}).get("en", "")
            topic_ta = item.get("topic", {}).get("ta", "")
            q_en = item.get("question", {}).get("en", "")
            q_ta = item.get("question", {}).get("ta", "")
            
            combined_text = f"Topic: {topic_en} | {topic_ta}. Question: {q_en} | {q_ta}"
            contents_payload.append(types.Content(parts=[types.Part.from_text(text=combined_text)]))
            
        success = False
        attempt = 0
        max_attempts = 5
        
        while not success and attempt < max_attempts:
            try:
                response = client.models.embed_content(
                    model='gemini-embedding-2',
                    contents=contents_payload,
                    config=types.EmbedContentConfig(task_type="RETRIEVAL_DOCUMENT")
                )
                
                if len(response.embeddings) != len(chunk_items):
                    raise ValueError(f"Mismatch: expected {len(chunk_items)} embeddings, got {len(response.embeddings)}")
                    
                for idx, item in enumerate(chunk_items):
                    indexed_data.append({
                        "id": item.get("id"),
                        "source_question_id": item.get("source_question_id"),
                        "topic": item.get("topic"),
                        "question": item.get("question"),
                        "answers": item.get("answers"),
                        "embedding": response.embeddings[idx].values
                    })
                    
                success = True
                print(f"Successfully processed {len(chunk_items)} items (up to index {current_index + len(chunk_items)})")
                
                with open(index_path, "w", encoding="utf-8") as f:
                    json.dump(indexed_data, f, ensure_ascii=False)
                    
                current_index += len(chunk_items)
                
            except Exception as e:
                attempt += 1
                error_str = str(e)
                print(f"Error processing batch (Attempt {attempt}): {error_str}")
                
                if "400" in error_str or "payload" in error_str.lower():
                    if batch_size > 50:
                        print("Payload or batch size rejected. Falling back to batch size 50.")
                        batch_size = 50
                        break # Break inner loop to recreate chunk with new batch size
                        
                if attempt >= max_attempts:
                    print("Max attempts reached. Stopping ingestion.")
                    sys.exit(1)
                    
                sleep_time = (2 ** attempt) + 35 
                print(f"Backing off for {sleep_time} seconds...")
                time.sleep(sleep_time)

    # Verification
    final_ids = {item.get("source_question_id") for item in indexed_data}
    print("--------------------------------------------------")
    print(f"Verification: Total records in index = {len(indexed_data)}")
    print(f"Verification: Unique source_question_ids = {len(final_ids)}")
    if len(final_ids) == 1600:
        print("SUCCESS: Index contains exactly 1600 unique records.")
    else:
        print("WARNING: Index does NOT contain exactly 1600 unique records.")

if __name__ == "__main__":
    build_index()
