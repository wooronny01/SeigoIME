import urllib.request
import zlib

# 구글 Mozc 사전 데이터 URL (오픈소스 버전)
urls = [
    "https://raw.githubusercontent.com/google/mozc/master/src/data/dictionary_oss/dictionary00.txt",
    "https://raw.githubusercontent.com/google/mozc/master/src/data/dictionary_oss/dictionary01.txt",
    "https://raw.githubusercontent.com/google/mozc/master/src/data/dictionary_oss/dictionary02.txt",
    "https://raw.githubusercontent.com/google/mozc/master/src/data/dictionary_oss/dictionary03.txt",
    "https://raw.githubusercontent.com/google/mozc/master/src/data/dictionary_oss/dictionary04.txt",
    "https://raw.githubusercontent.com/google/mozc/master/src/data/dictionary_oss/dictionary05.txt",
    "https://raw.githubusercontent.com/google/mozc/master/src/data/dictionary_oss/dictionary06.txt",
    "https://raw.githubusercontent.com/google/mozc/master/src/data/dictionary_oss/dictionary07.txt",
    "https://raw.githubusercontent.com/google/mozc/master/src/data/dictionary_oss/dictionary08.txt",
    "https://raw.githubusercontent.com/google/mozc/master/src/data/dictionary_oss/dictionary09.txt",
]

output_file = "app/src/main/assets/kanji_data.csv"
word_set = set()

print("🚀 구글 Mozc 순수 데이터를 다운로드하여 초기화합니다...")

for url in urls:
    print(f"다운로드 중: {url.split('/')[-1]}")
    try:
        response = urllib.request.urlopen(url)
        data = response.read().decode('utf-8')
        for line in data.splitlines():
            parts = line.split('\t')
            if len(parts) >= 5:
                hiragana = parts[0].strip()
                kanji = parts[4].strip()
                # 불필요한 기호나 숫자 제외, 순수 한자/단어만 추출
                if hiragana and kanji and hiragana != kanji:
                    word_set.add(f"{hiragana},{kanji}")
    except Exception as e:
        print(f"오류 발생: {e}")

# 정렬 후 저장
with open(output_file, 'w', encoding='utf-8') as f:
    for item in sorted(word_set):
        f.write(item + "\n")

print(f"✅ 초기화 완료! {output_file}에 {len(word_set):,}개의 순수 Mozc 데이터가 저장되었습니다.")
