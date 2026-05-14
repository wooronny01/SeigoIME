import urllib.request
import os

# Mozc 사전 파일 원본 URL 목록 (00번부터 09번까지 총 10개)
base_url = "https://raw.githubusercontent.com/google/mozc/master/src/data/dictionary_oss/dictionary{:02d}.txt"

# [수정됨] Kotlin 코드와 이름을 맞추기 위해 kanji_data.csv로 변경했습니다.
output_csv = "app/src/main/assets/kanji_data.csv"

# 중복 매핑을 제거하여 DB 용량을 최적화하기 위한 Set
dict_set = set()

print("🌐 구글 Mozc 오픈소스 사전 다운로드를 시작합니다...")

# 폴더가 없으면 생성
os.makedirs(os.path.dirname(output_csv), exist_ok=True)

# 0번부터 9번 파일까지 순회하며 다운로드 및 파싱
for i in range(10):
    url = base_url.format(i)
    print(f"다운로드 및 변환 중... dictionary{i:02d}.txt")
    try:
        req = urllib.request.urlopen(url)
        lines = req.read().decode('utf-8').splitlines()
        
        for line in lines:
            parts = line.split('\t')
            if len(parts) >= 5:
                hiragana = parts[0].strip()
                kanji = parts[4].strip()
                
                # 콤마(,)가 포함된 단어는 CSV 형식을 깨뜨리므로 안전하게 필터링
                if "," not in hiragana and "," not in kanji:
                    dict_set.add(f"{hiragana},{kanji}")
    except Exception as e:
        print(f"다운로드 실패: dictionary{i:02d}.txt - {e}")

print(f"✍️ 정제된 데이터를 {output_csv} 파일로 저장하는 중...")

# 알파벳/히라가나 순으로 깔끔하게 정렬하여 저장
with open(output_csv, 'w', encoding='utf-8') as f:
    for item in sorted(dict_set):
        f.write(item + "\n")

print(f"🎉 성공! 총 {len(dict_set):,}개의 단어가 추출되어 스마트폰 내장 사전으로 준비되었습니다.")
