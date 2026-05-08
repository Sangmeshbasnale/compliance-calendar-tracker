import chromadb
from sentence_transformers import SentenceTransformer
import uuid


class ChromaService:
    def __init__(self):
        # ✅ PersistentClient actually saves data to disk
        self.client = chromadb.PersistentClient(path="./chroma_db")

        self.collection = self.client.get_or_create_collection(
            name="knowledge_base"
        )

        self.model = SentenceTransformer("all-MiniLM-L6-v2")

    def add_documents(self, documents):
        embeddings = self.model.encode(documents).tolist()
        ids = [str(uuid.uuid4()) for _ in documents]

        self.collection.add(
            documents=documents,
            embeddings=embeddings,
            ids=ids
        )

    def query(self, query_text, top_k=3):
        query_embedding = self.model.encode([query_text]).tolist()

        results = self.collection.query(
            query_embeddings=query_embedding,
            n_results=top_k
        )

        # ✅ Guard against empty collection
        docs = results.get("documents", [[]])
        return docs[0] if docs and docs[0] else []