# ⚔️ Git Ranker - 개발자 전투력 측정기
> "단순 커밋 수는 의미 없다. 코드의 품질과 기여도로 당신의 진짜 티어를 증명하세요."

Git Ranker는 GitHub 활동을 분석하여 **개발자의 기여도를 점수화**하고, 개발자들과의 **상대적 순위를 통해 티어를 부여**하는 게이미피케이션 서비스입니다.

## 🚀 Key Features
### 1. 공정한 전투력 측정 (Scoring System)
> 단순히 "잔디 심기(Commit)"만 한다고 점수가 높지 않습니다. 코드 리뷰, 이슈 제기, PR 병합 등 협업과 품질에 기여하는 활동에 더 높은 가중치를 부여합니다.

| 활동 유형 | 가중치 | 설명 |
|:---:|:---:|:---|
| **PR Merged** | **10점** | 프로젝트에 코드가 실제 반영된 최고의 기여 |
| **PR Open** | **5점** | 새로운 기능 제안 및 버그 수정 요청 |
| **Review** | **3점** | 동료의 코드 품질 향상에 기여 |
| **Issue** | **2점** | 문제 발견 및 논의 주도 |
| **Commit** | **1점** | 기본적인 코드 작성 활동 |

### 2. 실시간 티어 시스템 (Tier System)
> 전체 사용자 중 나의 위치(Percentile)를 기반으로 6단계 티어가 부여됩니다.

- 💎 DIAMOND (상위 5%)
- 💿 PLATINUM (상위 10%)
- 🥇 GOLD (상위 20%)
- 🥈 SILVER (상위 40%)
- 🥉 BRONZE (상위 70%)
- 🛡️ IRON (기본)

### 3. README 배지 지원
> 자신의 티어를 GitHub 프로필에 자랑할 수 있도록 실시간 SVG 배지를 제공합니다.

<img width="1334" height="518" alt="image" src="https://github.com/user-attachments/assets/e3a8cf7a-e636-4d9c-8bb8-b1dda19fd71d" />

<div align=center>
  
[![Git Ranker](https://www.git-ranker.com/api/v1/badges/MDQ6VXNlcjgxOTU5MDAy)](https://www.git-ranker.com)

(점수와 순위가 바뀌면 배지도 자동으로 업데이트됩니다.)

</div>

```Markdown
[![Git Ranker](https://git-ranker.com/api/v1/badges/{your_node_id})](https://git-ranker.com)
```

---
### 2025.12.11 기준
> 매우 초기 버전이며, 가중치 테이블/티어 시스템/배지 디자인/서비스 안정화/README.md 등등 지금까지도 계속해서 업데이트 중에 있습니다 :) 
