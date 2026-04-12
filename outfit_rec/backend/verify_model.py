from ultralytics import YOLO
import sys

try:
    model = YOLO("d:/outfit_rec/backend/yolov8n-fashionpedia.onnx", task='segment')
    print("Model loaded successfully.")
    print("Classes:")
    print(model.names)
except Exception as e:
    print(f"Error: {e}")
