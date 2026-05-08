import threading
import time
from services.groq_client import generate_report


def process_report_async(report_id, user_input, store, lock):
    def task():
        print(f"🚀 Async started for report_id: {report_id}")

        try:
            
            time.sleep(2)

            
            result = generate_report(user_input)
            print("✅ AI result received")

            
            with lock:
                if not result or (isinstance(result, dict) and "error" in result):
                    store[report_id]["status"] = "FAILED"
                    store[report_id]["data"] = result
                    print("⚠️ Marked as FAILED")
                else:
                    store[report_id]["status"] = "COMPLETED"
                    store[report_id]["data"] = result
                    print("✅ Marked as COMPLETED")

        except Exception as e:
            print("❌ Exception in async:", str(e))

            with lock:
                store[report_id]["status"] = "FAILED"
                store[report_id]["data"] = None

  
    thread = threading.Thread(target=task, daemon=True)
    thread.start()
    