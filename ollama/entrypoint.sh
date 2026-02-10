#!/bin/bash
set -e

# Start ollama in background
ollama serve &

# Wait for ollama to be ready
echo "Waiting for Ollama to start..."
until ollama list > /dev/null 2>&1; do
  sleep 1
done

echo "Ollama is ready."

# Ensure embedding model exists
if ! ollama list | grep -q "embeddinggemma"; then
  echo "Pulling embeddinggemma model..."
  ollama pull embeddinggemma:300m-qat-q8_0
fi

echo "Embedding model is available."

# Keep ollama running in foreground
wait
