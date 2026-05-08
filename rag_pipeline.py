from rag.loader import load_documents
from rag.chunker import chunk_text
from rag.embedder import embed_chunks
from rag.vector_store import store_embeddings

def run_pipeline():
    text = load_documents("data/sample.txt")

    chunks = chunk_text(text)

    print("\n===== CHUNK DETAILS =====")
    print("Total chunks:", len(chunks))

    for i, chunk in enumerate(chunks):
        print(f"\nChunk {i+1}")
        print("Length:", len(chunk))
        print("Preview:", chunk[:100])
        print("-" * 50)

   
    print("\n===== OVERLAP CHECK =====")
    for i in range(len(chunks) - 1):
        end_part = chunks[i][-50:]
        start_part = chunks[i+1][:50]

        print(f"\nChunk {i+1} -> Chunk {i+2}")
        print("Match:", end_part == start_part)

    
    embeddings = embed_chunks(chunks)
    print("\nEmbeddings created")

    store_embeddings(chunks, embeddings)
    print("Stored in ChromaDB")


if __name__ == "__main__":
    run_pipeline()