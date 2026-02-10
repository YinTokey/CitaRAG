FROM ollama/ollama:latest

# Start server, pull model, and ensure the process is terminated so the build can finish
RUN ollama serve & sleep 10 && ollama pull embeddinggemma:300m-qat-q8_0 && pkill ollama || true
