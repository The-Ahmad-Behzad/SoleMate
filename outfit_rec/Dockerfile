FROM python:3.10-slim

# Install system dependencies required by opencv-python
RUN apt-get update && apt-get install -y \
    libgl1 \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

# Set the working directory in the container
WORKDIR /app

# Copy the requirements file
# This assumes the Dockerfile is built from the outfit_rec directory
COPY reqs.txt .

# Install the Python dependencies
RUN pip install --no-cache-dir -r reqs.txt

# Copy the backend code into the container
COPY backend/ ./backend/

# Expose the API port
EXPOSE 8000

# Set the working directory to the backend folder where the API code lives
WORKDIR /app/backend

# Command to run the Uvicorn server
CMD ["uvicorn", "api:app", "--host", "0.0.0.0", "--port", "8000"]
