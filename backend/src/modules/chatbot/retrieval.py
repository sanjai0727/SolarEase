import json
import os
import numpy as np
from google import genai
from google.genai import types
from src.core.config import settings

INDEX_PATH = os.path.join(os.path.dirname(__file__), "embeddings_index.json")

# In-memory index
_metadata = []
_vector_matrix = None
_client = None

# Configurable thresholds
HIGH_SIMILARITY_THRESHOLD = 0.75
MEDIUM_SIMILARITY_THRESHOLD = 0.50

def get_client():
    global _client
    if _client is None:
        try:
            _client = genai.Client(vertexai=True, project="solarease-bf39c", location="asia-south1")
        except Exception:
            if settings.GEMINI_API_KEY:
                _client = genai.Client(api_key=settings.GEMINI_API_KEY)
            else:
                raise
    return _client

def load_index():
    global _metadata, _vector_matrix
    if not os.path.exists(INDEX_PATH):
        print(f"Warning: Index file not found at {INDEX_PATH}")
        return
        
    with open(INDEX_PATH, "r", encoding="utf-8") as f:
        data = json.load(f)
        
    if not data:
        print("Warning: Index file is empty.")
        return
        
    _metadata = []
    vectors = []
    
    for item in data:
        # Separate metadata from vector
        _metadata.append({
            "source_question_id": item.get("source_question_id"),
            "topic": item.get("topic"),
            "question": item.get("question"),
            "answers": item.get("answers")
        })
        vectors.append(item.get("embedding"))
        
    # Convert vectors into a NumPy float32 matrix
    raw_matrix = np.array(vectors, dtype=np.float32)
    
    # Normalize all document vectors once during startup
    norms = np.linalg.norm(raw_matrix, axis=1, keepdims=True)
    # Avoid division by zero
    norms[norms == 0] = 1.0
    _vector_matrix = raw_matrix / norms
    
    print(f"Loaded {_vector_matrix.shape[0]} concepts into in-memory normalized matrix.")

def determine_similarity_band(score: float) -> str:
    if score >= HIGH_SIMILARITY_THRESHOLD:
        return "HIGH"
    elif score >= MEDIUM_SIMILARITY_THRESHOLD:
        return "MEDIUM"
    else:
        return "VERY_LOW"

def retrieve_concepts(query: str, top_k: int = 3):
    if _vector_matrix is None or len(_metadata) == 0:
        # Fallback if index not loaded
        load_index()
        if _vector_matrix is None:
            return [], "NONE"
            
    client = get_client()
    try:
        import time
        emb_start = time.time()
        response = client.models.embed_content(
            model='text-embedding-005',
            contents=query
        )
        print(f"query embedding completed in {time.time() - emb_start:.2f}s")
        query_embedding = np.array(response.embeddings[0].values, dtype=np.float32)
    except Exception as e:
        print(f"Error embedding query: {e}")
        return [], "ERROR"
        
    # Normalize the query vector
    query_norm = np.linalg.norm(query_embedding)
    if query_norm > 0:
        query_embedding = query_embedding / query_norm
        
    # Perform cosine similarity using matrix dot product
    # Since both query_embedding and _vector_matrix are normalized, 
    # dot product equals cosine similarity
    similarity_scores = np.dot(_vector_matrix, query_embedding)
    
    # Get Top-K indices sorted in descending order
    # argpartition is faster than argsort for Top-K, but argsort is fine for 1600 elements
    top_indices = np.argsort(similarity_scores)[::-1][:top_k]
    
    top_results = []
    highest_score = similarity_scores[top_indices[0]] if len(top_indices) > 0 else 0.0
    band = determine_similarity_band(highest_score)
    
    for idx in top_indices:
        item = _metadata[idx]
        top_results.append({
            "score": float(similarity_scores[idx]),
            "source_question_id": item.get("source_question_id"),
            "topic": item.get("topic"),
            "question": item.get("question"),
            "answers": item.get("answers")
        })
        
    return top_results, band
