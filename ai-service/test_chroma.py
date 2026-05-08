from services.chroma_service import ChromaService

chroma = ChromaService()

docs = [
    "Artificial Intelligence is transforming the world",
    "Machine learning is a subset of AI",
    "Finance deals with money and investments",
    "Health is important for a good life"
]

# Add documents
chroma.add_documents(docs)

# Query test
results = chroma.query("What is AI?")

print("Top Results:")
for r in results:
    print("-", r)