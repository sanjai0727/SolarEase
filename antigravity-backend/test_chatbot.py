import sys
sys.path.append('e:/projects/SolarEase/antigravity-backend')

from src.modules.chatbot.service import get_chatbot_response

try:
    response = get_chatbot_response("what is electricity", "student", "en")
    print(response)
except Exception as e:
    import traceback
    traceback.print_exc()
