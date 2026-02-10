FROM ollama/ollama:latest

# Pre-pull the embedding model during build
RUN ollama serve & sleep 10 && ollama pull embeddinggemma:300m-qat-q8_0 && pkill ollama || true
