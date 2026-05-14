import urllib.request
import os

output_csv = "app/src/main/assets/kanji_data.csv"
dict_set = set()

# 1. 롤백된 순수 Mozc 데이터 읽기
if os.path.exists(output_csv):
    with open(output_csv, 'r', encoding='utf-8') as f:
        for line in f:
            dict_set.add(line.strip())

print("✅ 순수 Mozc 데이터 로드 완료. 마스터 사전(SKK-JISYO.L)만 깔끔하게 융합합니다...")

# 2. 오직 기본/필수 한자음이 다 들어있는 마스터 사전만 다운로드
master_url = "https://raw.githubusercontent.com/skk-dev/dict/master/SKK-JISYO.L"

try:
    req = urllib.request.urlopen(master_url)
    lines = req.read().decode('euc-jp', errors='ignore').splitlines()
    for line in lines:
        if line.startswith(';;'): continue
        parts = line.split(' ', 1)
        if len(parts) >= 2:
            hiragana = parts[0].strip()
            kanjis = parts[1].strip('/').split('/')
            for kanji in kanjis:
                kanji_clean = kanji.split(';')[0].strip()
                # 불필요한 주석 제거 및 추가
                if kanji_clean and "," not in hiragana and "," not in kanji_clean:
                    dict_set.add(f"{hiragana},{kanji_clean}")
    print("✅ 마스터 사전 융합 성공!")
except Exception as e:
    print(f"다운로드 실패: {e}")

# 3. 알파벳/히라가나 순 정렬 후 덮어쓰기
print(f"✍️ 다이어트된 최적화 데이터를 {output_csv}에 저장하는 중...")
with open(output_csv, 'w', encoding='utf-8') as f:
    for item in sorted(dict_set):
        f.write(item + "\n")

print(f"🎉 성공! 가장 가볍고 강력한 황금 조합(Mozc + L)으로 앱이 최적화되었습니다. (총 {len(dict_set):,}개)")
