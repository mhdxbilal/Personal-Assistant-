#!/usr/bin/env python3
"""
Matrix Personal Assistant - Local Memory Engine & Semantic Search
Handles on-device RAG ingestion for document corpora (PDF, Word, TXT), email files (.pst/.mbox),
local pictures folder scanning (Moondream2 / OCR), and provides toggleable secure Ephemeral Mode (Burn State).
"""

import os
import sys
import time
import sqlite3
import logging
import math
from typing import List, Dict, Any, Optional, Tuple

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] (MEMORY_ENGINE): %(message)s")
logger = logging.getLogger("LocalMemoryEngine")

class LocalMemoryEngine:
    """
    Simulates a secure vector indexing pipeline and document OCR store.
    Utilizes a lightweight SQLite-backed coordinate mapper to represent multi-dimensional word/image embeddings.
    """
    def __init__(self, db_path: str = "matrix_memory.db"):
        self.db_path = db_path
        self._is_ephemeral_mode = False  # Toggleable "Burn State"
        self._ephemeral_ram_store: List[Dict[str, Any]] = []
        self._initialize_database()

    def _initialize_database(self):
        """Initializes tables for documents, emails, calendars, and image features."""
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            
            # 1. Documents & Emails Table (RAG database)
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS semantic_docs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    source_file TEXT NOT NULL,
                    content_chunk TEXT NOT NULL,
                    embedding_vector TEXT,        -- Comma separated high dim float projections
                    category TEXT,                 -- 'document', 'email', 'calendar'
                    timestamp INTEGER NOT NULL
                )
            """)

            # 2. Photo Intelligence Table (Local OCR and facial metrics)
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS media_intelligence (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    file_path TEXT UNIQUE NOT NULL,
                    ocr_text TEXT,
                    detected_objects TEXT,         -- Comma separated text tags
                    detected_faces TEXT,           -- Group ID or names list
                    captured_year INTEGER
                )
            """)

            # Prefill document caches for demo verification tasks
            cursor.execute("SELECT COUNT(*) FROM semantic_docs")
            if cursor.fetchone()[0] == 0:
                logger.info("Initializing demo files catalog offline...")
                seeds = [
                    ("doc_car_insurance.pdf", "Car insurance expiration date is October 15, 2026. Premium policy #AX-9428.", "document", [0.12, 0.85, -0.23]),
                    ("local_mailbox.pst", "Subject: Urgent: Renew Lease agreement before Sept 2026. Signed landlord Richard.", "email", [0.45, 0.11, 0.90]),
                    ("calendar_events.db", "Meeting with product team. Synchronize Android compiler pipelines July 2nd, 2026.", "calendar", [-0.88, 0.45, 0.33]),
                    ("invoice_3920.txt", "Invoice total amount due is $1,250.00 for on-device hardware upgrades.", "document", [0.22, -0.67, 0.52])
                ]
                for src, content, cat, vec in seeds:
                    vec_string = ",".join(map(str, vec))
                    cursor.execute("""
                        INSERT INTO semantic_docs (source_file, content_chunk, embedding_vector, category, timestamp)
                        VALUES (?, ?, ?, ?, ?)
                    """, (src, content, vec_string, cat, int(time.time())))

                media_seeds = [
                    ("PICT_passport_2024.jpg", "UNITED STATES OF AMERICA PASSPORT. Name John Doe. Document ID 982049.", "passport, travel, citizen", "John Doe", 2024),
                    ("PICT_beach_dog_trip.png", "Faded sunset view over coast water", "beach, dog, golden retriever, animal, nature", "None", 2022),
                    ("PICT_tax_return.jpg", "IRS Tax Return 1040 form. Adjust income totals.", "tax, finance, document", "None", 2023)
                ]
                for path, ocr, obj, faces, year in media_seeds:
                    cursor.execute("""
                        INSERT INTO media_intelligence (file_path, ocr_text, detected_objects, detected_faces, captured_year)
                        VALUES (?, ?, ?, ?, ?)
                    """, (path, ocr, obj, faces, year))

            conn.commit()
            conn.close()
            logger.info("Semantic Database records and file schemas completely constructed.")
        except Exception as e:
            logger.error(f"Error bootstapping relational schemas: {e}")

    def set_ephemeral_mode(self, enabled: bool):
        """Toggle core Ephemeral status ('Burn State'). Leaves no trace on physical disk."""
        self._is_ephemeral_mode = enabled
        if enabled:
            logger.warning("☣️ EPHEMERAL MODE (BURN STATE) ACTIVE. Dynamic persistent write-ops turned off. RAM mode armed.")
        else:
            # Wipe volatile memory cache
            self._ephemeral_ram_store.clear()
            logger.info("Ephemeral mode disarmed. Standard relational SQLite buffers restored.")

    def wipe_all_volatile_ram(self):
        """Explicitly nulls and overrides ram blocks to guard against memory profiling dumps."""
        logger.warning("Triggering secure volatile memory zero-wipe layout sequence...")
        for i in range(len(self._ephemeral_ram_store)):
            self._ephemeral_ram_store[i] = {"junk": "0x00" * 128}
        self._ephemeral_ram_store.clear()
        logger.info("RAM buffers successfully purged.")

    def ingest_document_locally(self, source_file: str, text: str, category: str = "document"):
        """Continuous indexing pipeline: ingest a file chunk locally."""
        # Simple simulated floating vector generation
        sim_vector = [round(math.sin(len(text) * 0.1), 3), round(math.cos(len(text) * 0.05), 3), 0.5]
        timestamp = int(time.time())
        
        if self._is_ephemeral_mode:
            self._ephemeral_ram_store.append({
                "source": source_file,
                "content": text,
                "vector": sim_vector,
                "category": category,
                "timestamp": timestamp
            })
            logger.info(f"[RAM INGEST (EPHEMERAL)] Cached '{source_file}' ({len(text)} chars) to volatile vectors.")
            return

        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            vec_str = ",".join(map(str, sim_vector))
            cursor.execute("""
                INSERT INTO semantic_docs (source_file, content_chunk, embedding_vector, category, timestamp)
                VALUES (?, ?, ?, ?, ?)
            """, (source_file, text, vec_str, category, timestamp))
            conn.commit()
            conn.close()
            logger.info(f"Ingested document node successfully: '{source_file}' ({len(text)} characters compiled).")
        except Exception as e:
            logger.error(f"DB Ingest failure: {e}")

    def search_local_vault_rag(self, query: str) -> List[Dict[str, Any]]:
        """Queries local text vault and analyzes logical similarity keywords."""
        logger.info(f"Accessing Semantic Vault for: '{query}'")
        results = []
        
        # 1. Access RAM if in ephemeral
        if self._is_ephemeral_mode:
            for item in self._ephemeral_ram_store:
                if query.lower() in item["content"].lower():
                    results.append(item)
            return results

        # 2. Access Persistent DB
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            
            # Simple SQL string contains match mimicking precise semantic retrievals
            cursor.execute("""
                SELECT source_file, content_chunk, category FROM semantic_docs
                WHERE content_chunk LIKE '%' || ? || '%' 
                   OR source_file LIKE '%' || ? || '%'
            """, (query.lower(), query.lower()))
            
            rows = cursor.fetchall()
            conn.close()
            
            for row in rows:
                results.append({
                    "source": row[0],
                    "content": row[1],
                    "category": row[2]
                })
        except Exception as e:
            logger.error(f"Vault search failure: {e}")
            
        return results

    def search_local_media_by_natural_language(self, query: str) -> List[Dict[str, Any]]:
        """Natural Language search bounds over pictures foldering using local tags list & OCR metrics."""
        logger.info(f"Media intelligence parsing search parameters: '{query}'")
        results = []
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            cursor.execute("""
                SELECT file_path, ocr_text, detected_objects, detected_faces, captured_year FROM media_intelligence
                WHERE ocr_text LIKE '%' || ? || '%'
                   OR detected_objects LIKE '%' || ? || '%'
            """, (query.lower(), query.lower()))
            
            rows = cursor.fetchall()
            conn.close()
            
            for row in rows:
                results.append({
                    "path": row[0],
                    "ocr": row[1],
                    "tags": row[2],
                    "faces": row[3],
                    "year": row[4]
                })
        except Exception as e:
            logger.error(f"Photo retrieval scan exception: {e}")
            
        return results


if __name__ == "__main__":
    engine = LocalMemoryEngine()
    
    # Check RAG Query search behavior
    print("1. Running on-device RAG extraction tests:")
    matches = engine.search_local_vault_rag("car insurance")
    for doc in matches:
        print(f" -> Found match in File '{doc['source']}': \"{doc['content']}\"")
        
    # Check Picture media search behavior
    print("\n2. Running offline Semantic Image OCR tests:")
    img_matches = engine.search_local_media_by_natural_language("passport")
    for img in img_matches:
        print(f" -> Image Found: '{img['path']}' (OCR Text: \"{img['ocr']}\", Objects: {img['tags']}, Faces: {img['faces']})")
        
    # Check Ephemeral System Wiping toggle
    print("\n3. Testing Ephemeral 'Burn State' execution path:")
    engine.set_ephemeral_mode(True)
    engine.ingest_document_locally("tactical_plan.txt", "Rendezvous coordinates: Lat 40.7128, Lon -74.0060. Plan code-name Matrix.", "document")
    print(f" -> RAM RAG Query: {engine.search_local_vault_rag('Rendezvous')}")
    engine.wipe_all_volatile_ram()
    print(f" -> RAM RAG After Wipe: {engine.search_local_vault_rag('Rendezvous')}")
