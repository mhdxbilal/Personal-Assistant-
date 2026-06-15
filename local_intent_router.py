#!/usr/bin/env python3
"""
Local Intent Router Module for 100% Offline Personal Assistant.
Interprets transcribed speech commands through local LLM prompts, maintains short-term conversational context,
and routes commands to offline operational execution tools (Knowledge Graph, System Toggles, Smart Home, Security).
"""

import os
import json
import sqlite3
import logging
from typing import Dict, Any, List, Optional, Tuple

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] (LocalIntentRouter): %(message)s")
logger = logging.getLogger("LocalIntentRouter")


class LocalIntentRouter:
    """
    Routinely intercepts text queries and matches them against functional capabilities
    using a system context instruction prompt and offline structured JSON tool representations.
    """
    def __init__(self, db_path: str = "assistant_data.db"):
        self.db_path = db_path
        self._initialize_sqlite_structures()
        self.conversation_history: List[Tuple[str, str]] = []  # List of (User text, System response)
        self.max_history_tokens = 5  # Circular conversational buffer size

    def _initialize_sqlite_structures(self):
        """Creates the local air-gapped relational database structure for alarms, tasks, and knowledge queries."""
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            
            # Tasks table (Alarms, timers, calendars)
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    task_type TEXT NOT NULL,       -- 'alarm', 'timer', 'reminder'
                    details TEXT NOT NULL,
                    target_time TEXT NOT NULL,      -- HH:MM or YYYY-MM-DD HH:MM
                    is_active INTEGER DEFAULT 1
                )
            """)

            # Offline Knowledge Graph cache (Compressed local Wikipedia snippet dictionary)
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS knowledge_graph (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    keyword TEXT UNIQUE NOT NULL,
                    definition TEXT NOT NULL
                )
            """)

            # Seed simple offline knowledge to allow offline RAG answers out of the box
            sample_data = [
                ("capital of france", "Paris is the capital and most populous city of France, situated on the Seine River."),
                ("speed of light", "The speed of light in a vacuum is approximately 299,792 kilometers per second (186,282 miles per second)."),
                ("gravity", "Gravity is a natural phenomenon by which all things with mass or energy are brought toward one another."),
                ("smart home", "A smart home is a local network of connected IoT devices that communicate locally via MQTT or WebSockets."),
                ("quantum computing", "Quantum computing is a multidisciplinary field comprising aspects of computer science, physics, and mathematics that utilizes quantum mechanics.")
            ]
            
            for keyword, definition in sample_data:
                cursor.execute("INSERT OR IGNORE INTO knowledge_graph (keyword, definition) VALUES (?, ?)", (keyword, definition))

            conn.commit()
            conn.close()
            logger.info("Local SQLite & Knowledge DB schemas initialized.")
        except Exception as e:
            logger.error(f"Failed to bootstrap local database variables: {e}")

    def add_to_history(self, user_text: str, assistant_response: str):
        """Adds a multi-turn conversation step to system prompt context."""
        if len(self.conversation_history) >= self.max_history_tokens:
            self.conversation_history.pop(0)
        self.conversation_history.append((user_text, assistant_response))

    def generate_system_prompt(self, current_time: str = "12:00") -> str:
        """Constructs an intelligent offline-first system routing directive for Llama.cpp / Ollama."""
        tools_definition = [
            {"tool": "telemetry_toggle", "params": {"setting": "wifi|bluetooth|brightness|darkmode|volume", "value": "on|off|increase|decrease|float"}},
            {"tool": "media_control", "params": {"action": "play|pause|stop|skip|volume_up|volume_down"}},
            {"tool": "task_scheduling", "params": {"type": "alarm|timer|reminder", "details": "text", "time": "HH:MM|integer_seconds"}},
            {"tool": "smart_home", "params": {"device": "living_room_light|ac_switch", "action": "turn_on|turn_off|dim"}},
            {"tool": "knowledge_rag", "params": {"query": "search term"}},
            {"tool": "vision_multimodal", "params": {"query": "screenshot analysis question"}}
        ]
        
        prompt = (
            "SYSTEM: You are a locally compiled, air-gapped Personal Assistant runtime. "
            f"Inside system environment scope. Current Local Time: {current_time}. "
            "Examine the input query carefully. You must output a JSON command only. "
            "Never write natural language conversational boilerplate.\n"
            f"Available functional local tools: {json.dumps(tools_definition)}\n"
            "Format your reply exactly as a JSON object: {\"tool\": \"tool_name\", \"params\": {...}}\n"
            "Example command 'turn off living room heater' -> {\"tool\": \"smart_home\", \"params\": {\"device\": \"heater\", \"action\": \"turn_off\"}}"
        )
        return prompt

    def query_local_knowledge_graph(self, query: str) -> str:
        """Queries local SQLite RAG table to find immediate, un-networked information matches."""
        logger.info(f"Conducting local semantic-keyword match for query: '{query}'")
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            
            # Simple keyword wildcard search
            cursor.execute("SELECT definition FROM knowledge_graph WHERE ? LIKE '%' || keyword || '%'", (query.lower(),))
            row = cursor.fetchone()
            conn.close()
            
            if row:
                return f"[OFFLINE RAG CONFIRMED] {row[0]}"
            return "[OFFLINE RAG LIMIT] Concept is currently not cached in the local Wikipedia snippet base. Request setup module synchronization."
        except Exception as e:
            logger.error(f"Error checking offline database rows: {e}")
            return "[OFFLINE ERROR] SQLite access exception."

    def schedule_task_locally(self, task_type: str, details: str, time_str: str) -> str:
        """Inserts a new timer, alarm, or reminder into the local scheduler pipeline."""
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            cursor.execute(
                "INSERT INTO tasks (task_type, details, target_time) VALUES (?, ?, ?)",
                (task_type, details, time_str)
            )
            conn.commit()
            conn.close()
            return f"Successfully scheduled {task_type} ('{details}') at {time_str} locally."
        except Exception as e:
            logger.error(f"Failed to record local schedule action: {e}")
            return f"Failed to record local task due to hardware DB lock: {e}"

    def run_inference_route(self, raw_command: str) -> Dict[str, Any]:
        """
        Main routing function. Emulates the local LLM parsing logic and outputs
        structured directives. In a running Ollama system, this queries local http loopback.
        """
        # Lowercase scrubbing for fast algorithmic routing fallbacks
        cleaned = raw_command.lower()
        logger.info(f"Processing offline routing pipeline for statement: '{raw_command}'")

        # 1. HARDWARE TELEMETRY & TOGGLES
        if any(w in cleaned for w in ["wifi", "wi-fi", "bluetooth", "brightness", "dark mode", "volume"]):
            setting = "wifi"
            if "bluetooth" in cleaned: setting = "bluetooth"
            elif "brightness" in cleaned: setting = "brightness"
            elif "dark mode" in cleaned: setting = "darkmode"
            elif "volume" in cleaned: setting = "volume"

            val = "on"
            if any(w in cleaned for w in ["off", "disable", "shut"]): val = "off"
            elif "increase" in cleaned or "up" in cleaned: val = "increase"
            elif "decrease" in cleaned or "down" in cleaned: val = "decrease"

            return {"tool": "telemetry_toggle", "params": {"setting": setting, "value": val}}

        # 2. COMPLEX MEDIA CONTROLS
        if any(w in cleaned for w in ["play", "pause", "stop", "skip", "next video", "previous"]):
            action = "play"
            if "pause" in cleaned: action = "pause"
            elif "stop" in cleaned: action = "stop"
            elif "skip" in cleaned or "next" in cleaned: action = "skip"

            return {"tool": "media_control", "params": {"action": action}}

        # 3. PROACTIVE TASK ENGINE (Alarms / Reminders)
        if any(w in cleaned for w in ["alarm", "timer", "reminder", "schedule"]):
            task_type = "reminder"
            if "alarm" in cleaned: task_type = "alarm"
            elif "timer" in cleaned: task_type = "timer"

            # Parse a mock standard time or parameters
            details = raw_command
            target_time = "10:00"
            for token in cleaned.split():
                if ":" in token:
                    target_time = token
            
            return {"tool": "task_scheduling", "params": {"type": task_type, "details": details, "time": target_time}}

        # 4. LOCAL LAN / MULTICAST SMART HOME (MQTT / HASS WebSocket)
        if any(w in cleaned for w in ["light", "fan", "switch", "iot", "heater", "outlet"]):
            device = "living_room_light"
            if "fan" in cleaned: device = "bedroom_fan"
            elif "heater" in cleaned: device = "living_room_heater"

            action = "turn_on"
            if "off" in cleaned or "stop" in cleaned: action = "turn_off"

            return {"tool": "smart_home", "params": {"device": device, "action": action}}

        # 5. MULTIMODAL VISION QUERY
        if any(w in cleaned for w in ["what is this", "what am i looking at", "describe this screenshot", "analyze image"]):
            return {"tool": "vision_multimodal", "params": {"query": raw_command}}

        # 6. DEFAULT KNOWLEDGE GRAPH DATABASE (RAG)
        return {"tool": "knowledge_rag", "params": {"query": raw_command}}


if __name__ == "__main__":
    # Internal router regression validations
    router = LocalIntentRouter()
    
    # Test cases representing each suite of the Unified Feature Matrix
    test_cases = [
        "Turn off the system WiFi and Bluetooth",
        "Pause the global background media player node",
        "Set an alarm for tomorrow morning at 07:30",
        "Turn on living room light over local MQTT broker",
        "What is the speed of light in vacuum?",
        "Look at this screenshot and what is this on my active monitor layout?"
    ]

    print("Running air-gapped system intent matching cycles:")
    for text in test_cases:
        routed = router.run_inference_route(text)
        print(f"Statement: \"{text}\"")
        print(f" -> Output Route JSON: {json.dumps(routed)}")
        
        # If it matched offline RAG, simulate resolving it
        if routed["tool"] == "knowledge_rag":
            result = router.query_local_knowledge_graph(routed["params"]["query"])
            print(f"    RAG Response: {result}")
        elif routed["tool"] == "task_scheduling":
            result = router.schedule_task_locally(routed["params"]["type"], routed["params"]["details"], routed["params"]["time"])
            print(f"    Scheduler status: {result}")
        print()
