<p align="center">
  <img width="2998" height="1342" alt="git-ranker-banner" src="https://github.com/user-attachments/assets/a30ef3df-1730-439f-9c47-e69bf31abb30" />
</p>

<p align="center">
  <strong>단순한 커밋 수는 의미 없다. 코드의 품질과 기여도로 당신의 진짜 가치를 증명하세요.</strong>
</p>

<p align="center">
  GitHub 활동 데이터를 분석하여 개발자의 기여도를 점수화하고,<br/>
  상대적 순위에 따라 티어를 부여하는 <strong>개발자 게이미피케이션 서비스</strong>입니다.
</p>

<p align="center">
  <a href="https://github.com/alexization/git-ranker/blob/main/LICENSE">
    <img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="License: MIT"/>
  </a>
  <a href="https://www.java.com/">
    <img src="https://img.shields.io/badge/Java-21-orange.svg" alt="Java 21"/>
  </a>
  <a href="https://spring.io/projects/spring-boot">
    <img src="https://img.shields.io/badge/Spring%20Boot-3.4.0-brightgreen.svg" alt="Spring Boot 3.4.0"/>
  </a>
  <a href="https://www.git-ranker.com">
    <img src="https://img.shields.io/badge/Website-git--ranker-blue" alt="Website"/>
  </a>
  <a href="https://github.com/alexization/git-ranker/issues">
    <img src="https://img.shields.io/github/issues/alexization/git-ranker" alt="GitHub Issues"/>
  </a>
</p>

<p align="center">
  <a href="#-introduction">Introduction</a> •
  <a href="#-key-features">Key Features</a> •
  <a href="#-scoring-system">Scoring System</a> •
  <a href="#-tier-system">Tier System</a> •
  <a href="#-how-to-use">How to Use</a> •
  <a href="#-badge">Badge</a> •
  <a href="#-data-refresh">Data Refresh</a> •
  <a href="#-contributing">Contributing</a>
</p>

---

## 📖 Introduction

### 해결하고자 하는 문제

많은 개발자들이 **"1일 1커밋"** 을 목표로 GitHub 잔디를 채우는 데 집중합니다. 하지만 과연 단순한 커밋 횟수가 개발자의 실력을 대변할 수 있을까요?

실제로 개발 실력과 협업 능력은 다음과 같은 **질적 활동**에서 드러납니다.

- **Pull Request**: 실제 프로젝트에 코드를 기여하고 머지시키는 능력
- **Code Review**: 동료의 코드를 검토하고 품질을 높이는 협업 능력
- **Issue**: 문제를 발견하고 논의를 주도하는 능력

**Git Ranker**는 이러한 질적 기여에 **차등화된 가중치**를 부여하여, 개발자의 진정한 전투력을 공정하게 측정합니다.

---

## ✨ Key Features

### 1. 공정한 전투력 측정

단순 활동량이 아닌 **협업과 프로젝트 기여도**에 초점을 맞춘 점수 산정 시스템을 제공합니다. PR 머지, 코드 리뷰 등 실질적인 기여에 높은 가중치를 부여합니다.

### 2. 실시간 티어 및 랭킹

**절대 평가와 상대 평가를 결합한 하이브리드 티어 시스템**을 제공합니다. 기본 티어는 점수로, 상위 티어는 백분위로 결정되어 공정하고 의미 있는 경쟁이 가능합니다. 랭킹은 **매 1시간마다** 자동 갱신됩니다.

### 3. 동적 프로필 배지

GitHub 프로필 README에 삽입할 수 있는 **실시간 SVG 배지**를 제공합니다. 점수와 순위 변동 시 배지도 자동으로 업데이트됩니다.

### 4. 활동 증감 추적

**전날 대비 활동 증감**을 배지와 프로필에서 실시간으로 확인할 수 있습니다. 오늘 얼마나 기여했는지 한눈에 파악하세요.

---

## 📊 Scoring System

### 활동별 가중치

Git Ranker는 GitHub의 5가지 핵심 활동에 대해 **근거 기반의 차등 가중치**를 적용합니다.

| 활동 유형 | 가중치 | 점수/건 | 설명 |
|:--------:|:------:|:------:|:-----|
| **PR Merged** | ×8 | 8점 | 커뮤니티 검증을 통과한 **품질 보증된 기여** |
| **PR Open** | ×5 | 5점 | 코드 + 테스트 + 문서화를 포함한 종합적 기여 |
| **Review** | ×5 | 5점 | 동료의 코드 품질 향상에 기여하는 **전문가 활동** |
| **Issue** | ×2 | 2점 | 문제 발견 및 논의 주도 |
| **Commit** | ×1 | 1점 | 기본적인 코드 작성 활동 |

### 점수 계산 공식

```
Total Score = (Commits × 1) + (Issues × 2) + (Reviews × 5) + (PR Open × 5) + (PR Merged × 8)
```

### 가중치 설계 근거

Git Ranker의 **1:2:5:5:8** 가중치는 학술 연구와 행동과학을 기반으로 설계되었습니다.

#### 코드 리뷰의 높은 가치 (Review: 5점)

학술 연구 메타 분석에 따르면, **코드 리뷰는 소프트웨어 결함의 60%를 탐지**합니다. 이는 유닛 테스팅(25%)이나 기능 테스팅(35%)보다 월등히 높은 수치입니다. 리뷰는 단순 활동이 아닌 **전문 지식의 적용**이므로, PR Open과 동등한 5점을 부여했습니다.

#### 피보나치 기반 스케일링 (PR Open: 5점, PR Merged: 8점)

정신물리학의 **최소 인지 차이(JND)** 원리에 따르면, 인간은 약 **60% 이상의 차이**에서 의미 있는 구분을 인지합니다. PR Merged(8점)는 PR Open(5점)보다 60% 높은 가치를 부여받으며, 이는 병합된 PR이 **커뮤니티 검증을 통과한 품질**임을 반영합니다.

---

## 🎖️ Tier System

### 하이브리드 티어 시스템

Git Ranker는 **절대 평가와 상대 평가를 결합**한 하이브리드 방식을 채택합니다

- **IRON ~ GOLD**: 점수 기반 **절대 평가**
- **PLATINUM ~ CHALLENGER**: 점수 + 백분위 기반 **상대 평가** 

### 티어 분포 기준

> [!NOTE]
> **티어 기준 조정 안내**: 티어 분포 기준은 서비스 성장과 사용자 분포에 따라 추후 조정될 수 있습니다. 변경 시 사전 공지를 통해 안내드립니다.

| 티어 | 배지 | 조건 |
|:----:|:----:|:----:|
| **CHALLENGER** | [![Git Ranker](https://www.git-ranker.com/api/v1/badges/CHALLENGER/badge)](https://git-ranker.com) | 상위 1% |
| **MASTER** | [![Git Ranker](https://www.git-ranker.com/api/v1/badges/MASTER/badge)](https://git-ranker.com) | 상위 5% |
| **DIAMOND** | [![Git Ranker](https://www.git-ranker.com/api/v1/badges/DIAMOND/badge)](https://git-ranker.com) | 상위 12% |
| **EMERALD** | [![Git Ranker](https://www.git-ranker.com/api/v1/badges/EMERALD/badge)](https://git-ranker.com) | 상위 25% |
| **PLATINUM** | [![Git Ranker](https://www.git-ranker.com/api/v1/badges/PLATINUM/badge)](https://git-ranker.com) | 상위 45% |
| **GOLD** | [![Git Ranker](https://www.git-ranker.com/api/v1/badges/GOLD/badge)](https://git-ranker.com) | 1,500점 이상 |
| **SILVER** | [![Git Ranker](https://www.git-ranker.com/api/v1/badges/SILVER/badge)](https://git-ranker.com) | 1,000점 이상 |
| **BRONZE** | [![Git Ranker](https://www.git-ranker.com/api/v1/badges/BRONZE/badge)](https://git-ranker.com) | 500점 이상 |
| **IRON** | [![Git Ranker](https://www.git-ranker.com/api/v1/badges/IRON/badge)](https://git-ranker.com) | 500점 미만 |

> [!IMPORTANT]
> Platinum 부터는 1,500점 이상을 만족해야 합니다.

---

## 🚀 How to Use

### Step 1: GitHub ID로 분석하기

1. [git-ranker.com](https://www.git-ranker.com) 접속
2. 검색창에 **GitHub Username** 입력
3. **"분석하기"** 버튼 클릭

<p align="center">
  <img src="https://github.com/user-attachments/assets/202752af-3003-4d07-bc19-a0d88e6395c4" alt="Search Example" width="600"/>
</p>

### Step 2: 결과 확인

분석이 완료되면 다음 정보를 확인할 수 있습니다.

- **Total Score**: 종합 점수
- **Tier & Rank**: 현재 티어 및 전체 순위
- **Activity Stats**: 활동별 상세 통계 (전날 대비 증감 포함)
- **Radar Chart**: 활동 분포 시각화

<p align="center">
  <img src="https://github.com/user-attachments/assets/9ec45d14-e34b-4815-97a6-2baccd8a246a" alt="Result Example" width="600"/>
</p>

### 신규 사용자 등록 시 데이터 수집 범위

Git Ranker에 **처음 등록하는 사용자**의 경우, 다음과 같이 데이터를 수집합니다.

```
수집 범위 = GitHub 가입일 ~ 현재
```

즉, GitHub에 가입한 첫날부터 현재까지의 **모든 활동 내역**을 집계하여 초기 점수를 산정합니다. 따라서 오래 전부터 GitHub를 사용해온 개발자도 정확한 점수를 받을 수 있습니다.

---

## 🎫 Badge

### 배지 복사 및 적용

분석 결과 화면에서 **"배지 복사"** 버튼을 클릭하면, 아래와 같은 Markdown 코드가 클립보드에 복사됩니다.

```markdown
[![Git Ranker](https://www.git-ranker.com/api/v1/badges/{your_node_id})](https://www.git-ranker.com)
```

이 코드를 GitHub 프로필의 **README.md**에 붙여넣으면 끝!

### 배지 정보

배지에는 다음 정보가 실시간으로 표시됩니다.

| 항목 | 설명 |
|------|------|
| **Tier** | 현재 티어 |
| **Score** | 총 점수 |
| **Rank** | 전체 순위 및 상위 백분율 |
| **Stats** | 각 활동별 수치 |
| **Diff** | 전날 대비 증감 (+N 또는 -N) |

### 배지 캐시 정책

배지는 **1시간 단위 캐시**가 적용되어 있습니다.

따라서 점수나 순위가 변경되면, **최대 1시간 이내**에 배지에 반영됩니다. 이는 GitHub의 이미지 캐시 정책과 조화를 이루면서도 최신 정보를 제공하기 위한 설정입니다.

---

## 🔄 Data Refresh

### 자동 갱신 (Daily Batch)

Git Ranker는 **매일 자정(KST)** 에 자동으로 모든 사용자의 점수를 갱신합니다.

```
자동 갱신 수집 범위 = 최근 1년간의 활동 데이터
```

#### 자동 갱신 프로세스

1. **매일 자정**: 전체 사용자의 **최근 1년 활동** 데이터를 GitHub API로 재수집
2. **증분 업데이트**: 이전 연도 데이터는 유지하고, 올해 데이터만 갱신
3. **랭킹 재계산**: 모든 사용자의 점수를 기준으로 순위 재배치

### 실시간 랭킹 갱신 (Hourly Batch)

신규 사용자가 등록되면 기존 사용자들의 순위에 영향을 줄 수 있습니다. 이를 반영하기 위해 **매 1시간마다** 랭킹을 재계산합니다.

```
조건: 지난 1시간 내 신규 사용자가 1명 이상 등록된 경우에만 실행
```

### 수동 갱신 (Manual Refresh)

특수한 상황에서 **수동 갱신** 기능을 사용할 수 있습니다.

#### 수동 갱신이 필요한 경우

| 상황 | 예시 설명 |
|------|-----------|
| **Private → Public 전환** | 2년 전의 Private 레포지토리를 Public으로 변경한 경우, 자동 갱신(최근 1년)으로는 반영되지 않음 |
| **즉시 반영이 필요한 경우** | 방금 한 기여를 바로 점수에 반영하고 싶을 때 |
| **과거 데이터 보정** | 1년 이전의 활동이 누락되었다고 판단될 때 |

#### 수동 갱신 데이터 수집 범위

```
수동 갱신 수집 범위 = GitHub 가입일 ~ 현재 (전체 기간)
```

수동 갱신은 자동 갱신과 달리 **GitHub 가입일부터 현재까지의 전체 활동**을 다시 집계합니다.

#### 수동 갱신 쿨다운

수동 갱신은 GitHub API 리소스를 많이 사용하므로, 남용을 방지하기 위해 **7일 쿨다운**이 적용됩니다.

```
다음 수동 갱신 가능 시간 = 마지막 수동 갱신 시간 + 7일
```

> [!NOTE]
> 쿨다운 기간 내에는 수동 갱신 버튼이 비활성화됩니다. 갱신 가능 시간은 프로필에서 확인할 수 있습니다.

---

## 🤝 Contributing

Git Ranker는 오픈소스 프로젝트입니다. 여러분의 기여를 환영합니다!

### 버그 리포트

버그를 발견하셨나요? [Bug Report](https://github.com/alexization/git-ranker/issues/new?template=bug_report.yml)를 통해 제보해 주세요.

제보 시 다음 정보를 포함해 주시면 빠른 해결에 도움이 됩니다.
- 발생한 문제에 대한 명확한 설명
- 재현 방법 (Steps to Reproduce)
- 기대 동작 vs 실제 동작
- 실행 환경 (OS, Browser 등)
- 스크린샷 또는 에러 로그

### 기능 제안

새로운 기능 아이디어가 있으신가요? [Feature Request](https://github.com/alexization/git-ranker/issues/new?template=feature_request.yml)를 통해 제안해 주세요.

### 보안 취약점

보안 관련 이슈는 공개 이슈로 등록하지 마시고, 아래 이메일로 직접 연락 주세요.

📧 **alexizationy@gmail.com**

### 일반 문의 및 토론

버그나 기능 요청이 아닌 일반적인 질문이나 의견은 [Discussions](https://github.com/alexization/git-ranker/discussions)를 이용해 주세요.

---

## ❓ FAQ

### Q: 왜 내 커밋이 모두 반영되지 않나요?

**A:** Git Ranker는 GitHub의 **Contributions** 기준을 따릅니다. GitHub에서 Contribution으로 인정되지 않는 커밋(예: Fork 레포의 커밋, GitHub에 등록되지 않은 이메일로 한 커밋)은 집계되지 않습니다.

### Q: Private 레포지토리의 활동도 집계되나요?

**A:** 아니요, Git Ranker는 **Public 활동만** 집계합니다. Private 레포지토리의 활동은 GitHub API를 통해 접근할 수 없습니다.

### Q: 순위가 갑자기 떨어졌어요. 왜 그런가요?

**A:** Git Ranker의 상위 티어(PLATINUM ~ CHALLENGER)는 **상대 평가** 시스템입니다. 새로운 고득점 사용자가 등록되거나, 다른 사용자들의 점수가 상승하면 내 순위가 상대적으로 하락할 수 있습니다. 단, IRON ~ GOLD 티어는 점수 기반이므로 다른 사용자의 영향을 받지 않습니다.

### Q: GitHub 닉네임을 변경했는데, 데이터가 사라졌어요.

**A:** 걱정하지 마세요! Git Ranker는 GitHub의 고유 ID(Node ID)를 기준으로 사용자를 식별합니다. 새 닉네임으로 다시 검색하면 기존 데이터가 자동으로 연결됩니다.

### Q: 배지가 업데이트되지 않아요.

**A:** 배지는 1시간 캐시가 적용되어 있습니다. 또한 GitHub 자체적으로도 이미지 캐시를 사용하므로, 최대 수 시간 후에 반영될 수 있습니다. 즉시 확인하고 싶다면 배지 URL 끝에 `?t=현재시간`을 추가해 보세요.

### Q: Review와 PR Open이 같은 점수인 이유가 뭔가요?

**A:** 학술 연구에 따르면 코드 리뷰는 결함의 60%를 탐지하며, PR 작성과 비슷하거나 더 높은 가치를 제공합니다. 리뷰는 단순 활동이 아닌 **전문 지식의 적용**이므로, PR Open과 동등한 가치를 부여했습니다.

---

## 📜 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

```
MIT License

Copyright (c) 2026 Baek Hyo Seok

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction...
```

---

## 📬 Contact

- **Website**: [git-ranker.com](https://www.git-ranker.com)
- **GitHub**: [@alexization](https://github.com/alexization)
- **Email**: alexizationy@gmail.com

---

<p align="center">
  <strong>⭐ 이 프로젝트가 마음에 드셨다면 Star를 눌러주세요!</strong>
</p>

<p align="center">
  <a href="https://github.com/alexization/git-ranker">
    <img src="https://img.shields.io/github/stars/alexization/git-ranker?style=social" alt="GitHub Stars"/>
  </a>
</p>
