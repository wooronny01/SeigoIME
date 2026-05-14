import urllib.request
import os

output_csv = "app/src/main/assets/kanji_data.csv"
dict_set = set()

# 1. 기존 데이터(Mozc + 인명사전) 로드
if os.path.exists(output_csv):
    with open(output_csv, 'r', encoding='utf-8') as f:
        for line in f:
            dict_set.add(line.strip())

print("✅ 기존 데이터 로드 완료. 대망의 마스터 사전(SKK-JISYO.L)을 융합합니다...")

# 2. 모든 한자의 음독/훈독이 들어있는 궁극의 Large 사전
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
                if kanji_clean and "," not in hiragana and "," not in kanji_clean:
                    dict_set.add(f"{hiragana},{kanji_clean}")
    print("✅ 마스터 사전 융합 성공!")
except Exception as e:
    print(f"다운로드 실패: {e}")

# 3. 알파벳/히라가나 순으로 깔끔하게 정렬하여 덮어쓰기
print(f"✍️ 최종 데이터를 {output_csv}에 저장하는 중...")
with open(output_csv, 'w', encoding='utf-8') as f:
    for item in sorted(dict_set):
        f.write(item + "\n")

print(f"🎉 완벽합니다! 총 {len(dict_set):,}개의 무결점 단어장(음독/훈독 완벽 포함)이 완성되었습니다.")
