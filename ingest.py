import sys, os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from services.chroma_service import ChromaService

chroma = ChromaService()

documents = [
    "Artificial Intelligence (AI) is the simulation of human intelligence by machines.",
    "Machine Learning is a subset of AI where systems learn from data without being explicitly programmed.",
    "Deep Learning uses neural networks with many layers to analyze data and make decisions.",
    "Natural Language Processing (NLP) allows computers to understand and generate human language.",
    "Computer Vision enables machines to interpret and understand visual information from images.",
]

chroma.add_documents(documents)
print(f"✅ Ingested {len(documents)} documents into ChromaDB.")