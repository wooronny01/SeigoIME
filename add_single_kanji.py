import urllib.request
import os

output_csv = "app/src/main/assets/kanji_data.csv"
dict_set = set()

# 1. 기존에 만들어둔 구글 Mozc 단어장(108만개)을 읽어서 기억 (중복 방지)
if os.path.exists(output_csv):
    with open(output_csv, 'r', encoding='utf-8') as f:
        for line in f:
            dict_set.add(line.strip())

print("✅ 기존 Mozc 데이터 로드 완료. 단일 한자 및 인명 사전을 추가로 다운로드합니다...")

# 2. 추가할 단일 한자 및 인명(이름) 사전 URL (SKK 오픈소스)
extra_urls = [
    "https://raw.githubusercontent.com/skk-dev/dict/master/SKK-JISYO.jinmei", # 인명 (叡, 恩 등 수만 개)
    "https://raw.githubusercontent.com/skk-dev/dict/master/SKK-JISYO.JIS2",   # 잘 안 쓰는 단일 한자까지 전부
    "https://raw.githubusercontent.com/skk-dev/dict/master/SKK-JISYO.JIS3_4"  # 초희귀 단일 한자
]

for url in extra_urls:
    print(f"다운로드 중... {url.split('/')[-1]}")
    try:
        req = urllib.request.urlopen(url)
        # SKK 사전은 EUC-JP 인코딩이므로 변환하여 읽음
        lines = req.read().decode('euc-jp', errors='ignore').splitlines()
        for line in lines:
            if line.startswith(';;'): continue # 주석 패스
            parts = line.split(' ', 1)
            if len(parts) >= 2:
                hiragana = parts[0].strip()
                # 한자들은 / 로 구분되어 있음 (/叡/英/...)
                kanjis = parts[1].strip('/').split('/')
                for kanji in kanjis:
                    kanji_clean = kanji.split(';')[0].strip() # 주석 설명 제거
                    if kanji_clean and "," not in hiragana and "," not in kanji_clean:
                        dict_set.add(f"{hiragana},{kanji_clean}")
    except Exception as e:
        print(f"다운로드 실패: {url} - {e}")

print(f"✍️ 모든 데이터를 융합하여 {output_csv}에 덮어쓰는 중...")

# 3. 알파벳/히라가나 순으로 깔끔하게 정렬하여 다시 저장
with open(output_csv, 'w', encoding='utf-8') as f:
    for item in sorted(dict_set):
        f.write(item + "\n")

print(f"🎉 성공! 총 {len(dict_set):,}개의 단어로 무한 확장되었습니다. (단일 한자 및 인명 완벽 장착)")
