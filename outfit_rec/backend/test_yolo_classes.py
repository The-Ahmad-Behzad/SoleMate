from ultralytics import YOLO

model = YOLO("c:/Users/Muhammad Umer Quresh/Desktop/outfit_rec/backend/yolov8n-fashionpedia.onnx", task='detect')
print("Model Classes:")
print(model.names)
