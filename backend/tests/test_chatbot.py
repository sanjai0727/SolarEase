import os
import sys

sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from src.modules.chatbot.service import get_chatbot_response

try:
    response = get_chatbot_response("what is electricity", "student", "en")
    print(response)
except Exception as e:
    import traceback
    traceback.print_exc()
