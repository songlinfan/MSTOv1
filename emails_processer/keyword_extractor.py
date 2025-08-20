import os
import uuid
import re
import nltk
import json
import shutil
import time
from multiprocessing import Pool
from functools import partial
from typing import List, Tuple, Set, Optional
from nltk.tokenize import word_tokenize

msg_start_pattern = re.compile(r"\n\n", re.MULTILINE)
msg_end_pattern = re.compile(r"\n+.*\n\d+/\d+/\d+ \d+:\d+ [AP]M", re.MULTILINE)

def clear_directory(dir_path: str) -> None:
    if os.path.exists(dir_path):
        shutil.rmtree(dir_path)
    os.makedirs(dir_path, exist_ok=True)

def load_english_valid_words(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        return {line.strip().lower() for line in f if line.strip()}

def load_english_stopwords(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        return {line.strip().lower() for line in f if line.strip()}

def parse_email(pathname: str) -> Optional[str]:
    with open(pathname, 'r', encoding='Latin-1') as TextFile:
        text = TextFile.read().replace("\r", "")
    try:
        msg_start_iter = msg_start_pattern.search(text).end()
        try:
            msg_end_iter = msg_end_pattern.search(text).start()
            message = text[msg_start_iter:msg_end_iter]
        except AttributeError:
            message = text[msg_start_iter:]
        message = re.sub("[\n\r]", " ", message)
        message = re.sub("  +", " ", message)
    except AttributeError:
        return None
    return message

def process_email(file_path: str, valid_words: Set[str], stopwords: Set[str]) -> Optional[dict]:
    uuid_str = str(uuid.uuid4())
    main_body = parse_email(file_path)

    if main_body is None:
        return None

    keywords = word_tokenize(main_body)
    extracted_keywords = [
        w.lower()
        for w in keywords
        if (cleaned := w.strip().lower()) in valid_words
           and cleaned not in stopwords
    ]

    return {
        "email_path": file_path.replace("\\", "/"),
        "uuid": uuid_str,
        "keywords": extracted_keywords
    }

def process_emails(source_dir: str, output_dir: str, valid_words: Set[str], stopwords: Set[str], num_processes: int, batch_size: int = 1000) -> None:
    if not os.path.exists(source_dir):
        return

    os.makedirs(output_dir, exist_ok=True)

    file_paths = []
    for root, _, files in os.walk(source_dir):
        for filename in files:
            file_path = os.path.join(root, filename)
            if os.path.isfile(file_path):
                file_paths.append(file_path)

    if not file_paths:
        return

    for i in range(0, len(file_paths), batch_size):
        batch_files = file_paths[i:i+batch_size]

        with Pool(processes=num_processes) as pool:
            process_func = partial(process_email, valid_words=valid_words, stopwords=stopwords)
            results = pool.imap_unordered(process_func, batch_files)

            # 处理并写入结果
            for result in results:
                if result:
                    json_file_path = os.path.join(output_dir, f"{result['uuid']}.json")
                    with open(json_file_path, 'w', encoding='utf-8') as f:
                        json.dump(result, f, ensure_ascii=False, indent=2)

if __name__ == "__main__":
    nltk.data.path.append('./nltk_data')

    SOURCE_DIR = "../emails_raw"
    OUTPUT_DIR = "../emails_processed"

    english_valid_words = load_english_valid_words("./english_valid_words")
    english_stopwords = load_english_stopwords("./english_stopwords")

    clear_directory(OUTPUT_DIR)
    print("Starting email processing ...")

    start_time = time.time()
    process_emails(SOURCE_DIR, OUTPUT_DIR, english_valid_words, english_stopwords, num_processes=4, batch_size=1000)
    end_time = time.time()
    print("Email processing completed !")
    print(f"Total processing time: {(end_time - start_time):.2f} seconds")