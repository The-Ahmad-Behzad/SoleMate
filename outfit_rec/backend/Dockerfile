FROM python:3.10-slim

# Install system dependencies required by opencv-python
RUN apt-get update && apt-get install -y \
    libgl1 \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

# Set the working directory in the container
WORKDIR /app

# Copy the requirements file into the container
# Both reqs.txt and Dockerfile are now side-by-side in the backend folder
COPY reqs.txt .

# Install the Python dependencies (using CPU-only PyTorch to save ~3.5GB)
RUN pip install --no-cache-dir -r reqs.txt --extra-index-url https://download.pytorch.org/whl/cpu

# Copy all the backend code into the app directory
COPY . .

# Expose the API port
EXPOSE 8000

# Command to run the Uvicorn server
CMD ["uvicorn", "api:app", "--host", "0.0.0.0", "--port", "8000"]
