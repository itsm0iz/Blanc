#!/usr/bin/env python3
"""Build Blanc's bundled offline dictionary (app/src/main/assets/dictionary.db).

Source: the Wordset dictionary (https://github.com/wordset/wordset-dictionary),
licensed CC-BY-SA 4.0 and derived from Princeton WordNet 3.0. We keep only the
word and a compact set of definitions to stay lightweight.

Usage:
    python3 tools/build_dictionary.py

Requires network access to raw.githubusercontent.com. The produced dictionary.db
is committed to the repo so normal builds need no network.
"""
import json
import os
import sqlite3
import string
import urllib.request

RAW = "https://raw.githubusercontent.com/wordset/wordset-dictionary/master/data/{}.json"
OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "dictionary.db")
MAX_MEANINGS = 3
MAX_DEF_LEN = 220


def fetch(letter, cache_dir):
    path = os.path.join(cache_dir, f"{letter}.json")
    if not os.path.exists(path):
        url = RAW.format(letter)
        try:
            with urllib.request.urlopen(url, timeout=60) as resp:
                data = resp.read()
        except Exception as exc:  # noqa: BLE001
            print(f"  skip {letter}: {exc}")
            return {}
        with open(path, "wb") as handle:
            handle.write(data)
    with open(path, encoding="utf-8") as handle:
        return json.load(handle)


def compact(entry):
    meanings = entry.get("meanings") or []
    parts = []
    for meaning in meanings[:MAX_MEANINGS]:
        definition = (meaning.get("def") or "").strip()
        if not definition:
            continue
        speech = (meaning.get("speech_part") or "").strip()
        text = f"({speech}) {definition}" if speech else definition
        if len(text) > MAX_DEF_LEN:
            text = text[:MAX_DEF_LEN].rstrip() + "…"
        parts.append(text)
    return " • ".join(parts)


def main():
    cache_dir = os.path.join(os.path.dirname(__file__), "_dict_cache")
    os.makedirs(cache_dir, exist_ok=True)
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    if os.path.exists(OUT):
        os.remove(OUT)

    conn = sqlite3.connect(OUT)
    conn.execute("CREATE TABLE entries (word TEXT PRIMARY KEY, defs TEXT NOT NULL) WITHOUT ROWID")

    total = 0
    for letter in string.ascii_lowercase:
        words = fetch(letter, cache_dir)
        rows = []
        for word, entry in words.items():
            key = word.strip().lower()
            if not key:
                continue
            defs = compact(entry)
            if defs:
                rows.append((key, defs))
        if rows:
            conn.executemany("INSERT OR IGNORE INTO entries (word, defs) VALUES (?, ?)", rows)
            total += len(rows)
        print(f"  {letter}: {len(rows)} words")

    conn.commit()
    conn.execute("VACUUM")
    conn.close()
    size = os.path.getsize(OUT)
    print(f"Done: {total} words, {size / 1_000_000:.1f} MB -> {OUT}")


if __name__ == "__main__":
    main()
