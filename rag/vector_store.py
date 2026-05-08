import chromadb
from chromadb.config import Settings

client = chromadb.Client(Settings(persist_directory="chroma_db"))

collection = client.get_or_create_collection(name="docs")

def store_embeddings(chunks, embeddings):
    for i in range(len(chunks)):
        collection.add(
            documents=[chunks[i]],
            embeddings=[embeddings[i]],
            ids=[str(i)]
        )