<p align="center">
  <img width="2998" height="1342" alt="git-ranker-banner" src="https://github.com/user-attachments/assets/a30ef3df-1730-439f-9c47-e69bf31abb30" />
</p>

<p align="center">
  <strong>단순한 커밋 수는 의미 없습니다. 코드의 품질과 기여도로 당신의 진짜 가치를 보여주세요.</strong>
</p>

<p align="center">
  GitHub 활동 데이터를 바탕으로 개발자의 기여도를 점수화하고,<br/>
  상대적 순위에 따라 티어를 부여하는 <strong>개발자 게이미피케이션 서비스</strong>입니다.
</p>

<p align="center">
  <a href="https://www.git-ranker.com"><img src="https://img.shields.io/website?url=https%3A%2F%2Fwww.git-ranker.com&up_message=online&down_message=offline&label=Service%20Status" alt="Service Status"/></a>
  <a href="https://github.com/alexization/git-ranker"><img src="https://img.shields.io/github/stars/alexization/git-ranker?style=flat&logo=github&color=yellow" alt="GitHub Stars"/></a>
  <a href="https://github.com/alexization/git-ranker/issues"><img src="https://img.shields.io/github/issues/alexization/git-ranker?logo=github" alt="GitHub Issues"/></a>
  <a href="https://github.com/alexization/git-ranker"><img src="https://img.shields.io/github/last-commit/alexization/git-ranker?color=red&logo=github" alt="Last Commit"/></a>
</p>

<p align="center">
  <a href="#introduction">Introduction</a> •
  <a href="#key-features">Key Features</a> •
  <a href="#scoring-system">Scoring System</a> •
  <a href="#tier-system">Tier System</a> •
  <a href="#how-to-use">How to Use</a> •
  <a href="#badge">Badge</a> •
  <a href="#data-refresh">Data Refresh</a> •
  <a href="#faq">FAQ</a> •
  <a href="#contributing">Contributing</a> •
  <a href="#roadmap">Roadmap</a> •
  <a href="#license">License</a> •
  <a href="#contact">Contact</a>
</p>

---

<a id="introduction"></a>
## 📖 Introduction

### 개발자의 성장을 더 정확하게 보여줍니다

많은 개발자는 GitHub 잔디로 꾸준함을 증명합니다. Git Ranker는 여기서 한 걸음 더 나아가, 프로젝트 성장에 실질적으로 기여한 활동의 가치를 함께 반영합니다.

- **Pull Request**: 프로젝트에 실질적인 코드를 기여하는 활동
- **Code Review**: 동료와 지식을 나누고 품질을 높이는 협업 활동
- **Issue**: 문제를 정의하고 논의를 시작하는 기여 활동

활동별 가중치를 통해 기여도를 입체적으로 계산하고, 점수·순위·티어로 한눈에 보여줍니다.

---

<a id="key-features"></a>
## ✨ Key Features

### 1) GitHub 로그인으로 바로 시작

**GitHub OAuth 로그인**으로 별도 회원가입 없이 시작할 수 있습니다. 로그인 후 바로 내 프로필 점수와 티어를 확인할 수 있습니다.

### 2) 기여 중심 점수 체계

단순 활동량이 아니라 **협업과 프로젝트 기여도**에 초점을 맞춘 점수 체계를 제공합니다. PR 머지, 코드 리뷰 등 영향력이 큰 활동에 높은 가중치를 부여합니다.

### 3) 티어와 랭킹으로 현재 위치 확인

**절대 평가 + 상대 평가**를 결합한 하이브리드 티어 시스템으로 현재 위치를 명확하게 보여줍니다.

### 4) 프로필에 바로 붙이는 동적 배지

GitHub 프로필 README에 삽입할 수 있는 **SVG 배지**를 제공합니다. 티어와 점수 변화가 배지에 반영됩니다.

### 5) 전일 대비 증감 확인

프로필과 배지에서 **전날 대비 활동 증감**을 확인할 수 있어, 최근 변화 흐름을 빠르게 파악할 수 있습니다.

### 6) 계정과 세션 관리

로그인 상태 확인, 토큰 갱신, 로그아웃, 전체 로그아웃, 회원 탈퇴까지 계정 관련 기능을 제공합니다.

---

<a id="scoring-system"></a>
## 📊 Scoring System

### 활동별 가중치

Git Ranker는 GitHub의 5가지 핵심 활동에 대해 차등 가중치를 적용합니다.

$$ \text{Total Score} = \sum (Commits \times 1) + (Issues \times 2) + (Reviews \times 5) + (Pr_O \times 5) + (Pr_M \times 8) $$

| 활동 유형 | 가중치 | 점수/건 | 설명 |
|:--|--:|--:|:--|
| **PR Merged** | ×8 | 8점 | 커뮤니티 검증을 통과한 기여 |
| **PR Open** | ×5 | 5점 | 문제 정의, 구현, 테스트, 문서화를 포함한 기여 |
| **Review** | ×5 | 5점 | 타인의 코드 품질 향상에 기여하는 협업 활동 |
| **Issue** | ×2 | 2점 | 문제 발견 및 논의 주도 |
| **Commit** | ×1 | 1점 | 기본적인 코드 작성 활동 |

### 가중치 설계 근거

Git Ranker의 **1:2:5:5:8** 가중치는 학술 연구와 행동과학을 기반으로 설계되었습니다.

#### 코드 리뷰의 높은 가치 (Review: 5점)

학술 연구 메타 분석에 따르면, **코드 리뷰는 소프트웨어 결함의 60%를 탐지**합니다. 이는 유닛 테스팅(25%)이나 기능 테스팅(35%)보다 월등히 높은 수치입니다. 리뷰는 단순 활동이 아닌 **전문 지식의 적용**이므로, PR Open과 동등한 5점을 부여했습니다.

#### 피보나치 기반 스케일링 (PR Open: 5점, PR Merged: 8점)

정신물리학의 **최소 인지 차이(JND)** 원리에 따르면, 인간은 약 **60% 이상의 차이**에서 의미 있는 구분을 인지합니다. PR Merged(8점)는 PR Open(5점)보다 60% 높은 가치를 부여받으며, 이는 병합된 PR이 **커뮤니티 검증을 통과한 품질**임을 반영합니다.

---

<a id="tier-system"></a>
## 🎖️ Tier System

### 하이브리드 티어 기준

- **IRON ~ GOLD**: 점수 기반 **절대 평가**
- **PLATINUM ~ CHALLENGER**: 점수 + 백분위 기반 **상대 평가**

> [!IMPORTANT]
> 상위 티어(PLATINUM ~ CHALLENGER)는 **최소 2,000점 이상**일 때만 적용됩니다.

### 티어 분포 기준

> [!NOTE]
> 티어 분포 기준은 서비스 성장과 사용자 분포에 따라 조정될 수 있습니다.

| 티어 | 배지 | 조건 |
|:--|:--|--:|
| **CHALLENGER** | [![Git Ranker](https://www.git-ranker.com/api/v1/badges/CHALLENGER/badge)](https://git-ranker.com) | 상위 1% |
| **MASTER** | [![Git Ranker](https://www.git-ranker.com/api/v1/badges/MASTER/badge)](https://git-ranker.com) | 상위 5% |
| **DIAMOND** | [![Git Ranker](https://www.git-ranker.com/api/v1/badges/DIAMOND/badge)](https://git-ranker.com) | 상위 12% |
| **EMERALD** | [![Git Ranker](https://www.git-ranker.com/api/v1/badges/EMERALD/badge)](https://git-ranker.com) | 상위 25% |
| **PLATINUM** | [![Git Ranker](https://www.git-ranker.com/api/v1/badges/PLATINUM/badge)](https://git-ranker.com) | 상위 45% |
| **GOLD** | [![Git Ranker](https://www.git-ranker.com/api/v1/badges/GOLD/badge)](https://git-ranker.com) | 1,500점 이상 |
| **SILVER** | [![Git Ranker](https://www.git-ranker.com/api/v1/badges/SILVER/badge)](https://git-ranker.com) | 1,000점 이상 |
| **BRONZE** | [![Git Ranker](https://www.git-ranker.com/api/v1/badges/BRONZE/badge)](https://git-ranker.com) | 500점 이상 |
| **IRON** | [![Git Ranker](https://www.git-ranker.com/api/v1/badges/IRON/badge)](https://git-ranker.com) | 500점 미만 |

---

<a id="how-to-use"></a>
## 🚀 How to Use

### 1) GitHub 로그인으로 시작

1. [git-ranker.com](https://www.git-ranker.com)에 접속합니다.
2. **"GitHub로 시작하기"** 버튼을 클릭합니다.
3. GitHub OAuth 인증을 완료합니다.

### 2) 결과 확인

로그인 후 프로필 페이지에서 아래 정보를 확인할 수 있습니다.

- **Total Score**: 종합 점수
- **Tier & Rank**: 현재 티어 및 전체 순위
- **Activity Stats**: 활동별 통계와 전일 대비 증감
- **Radar Chart**: 활동 분포 시각화

<p align="center">
  <img width="1058" height="843" alt="상세 페이지" src="https://github.com/user-attachments/assets/391acffb-de8e-44c5-b0a5-bc1448e304f5" />
</p>

### 3) 공유 및 배지 활용

분석 결과를 SNS나 블로그에 공유할 수 있으며, 배지 복사 기능으로 GitHub 프로필에 바로 적용할 수 있습니다.

> [!TIP]
> 처음 로그인한 사용자는 GitHub 가입일부터 현재까지의 활동을 기준으로 초기 점수를 계산합니다.

### 4) 공개 조회와 로그인 필요 기능

| 기능 | 로그인 필요 |
|:--|:--|
| 다른 사용자의 프로필 조회 | 아니요 |
| 전체 랭킹 조회 | 아니요 |
| 본인 프로필 등록 | 예 |
| 수동 갱신 | 예 |
| 로그아웃 / 전체 로그아웃 | 예 |
| 회원 탈퇴 | 예 |

---

<a id="badge"></a>
## 🎫 Badge

GitHub 프로필(`README.md`)에 동적 배지를 삽입해, 현재 티어와 점수를 공개 프로필에서 바로 보여줄 수 있습니다.

### 배지 복사 및 적용

분석 결과 화면에서 **"배지 복사"** 버튼을 클릭하면 아래 Markdown 코드가 복사됩니다.

```markdown
[![Git Ranker](https://www.git-ranker.com/api/v1/badges/{your_node_id})](https://www.git-ranker.com)
```

복사한 코드를 GitHub 프로필 `README.md`에 붙여 넣으면 적용됩니다.

### 배지 포함 정보

| 항목 | 설명 |
|:--|:--|
| **Tier** | 현재 티어 |
| **Score** | 총 점수 |
| **Rank** | 전체 순위 및 상위 백분율 |
| **Stats** | 활동별 수치 |
| **Diff** | 전날 대비 증감 (+N 또는 -N) |

### 배지 반영 주기

> [!NOTE]
> 배지는 **1시간 단위 캐시**가 적용됩니다.

---

<a id="data-refresh"></a>
## 🔄 Data Refresh

### 1) 자동 갱신 (Daily Batch)

- **매일 자정(KST)** 에 자동으로 점수를 갱신합니다.
- 올해 활동 데이터를 다시 수집하고, 이전 연도 데이터는 유지합니다.
- 재계산된 점수를 기준으로 전체 랭킹을 재배치합니다.

### 2) 수동 갱신 (Manual Refresh)

로그인한 사용자는 **본인 프로필**을 수동으로 갱신할 수 있습니다.

> [!IMPORTANT]
> 수동 갱신 조건: GitHub 로그인 + 본인 프로필

#### 수동 갱신이 유용한 경우

| 상황 | 예시 |
|:--|:--|
| **Private → Public 전환** | 과거 Private 저장소를 Public으로 전환한 경우 |
| **즉시 반영 필요** | 최근 기여를 빠르게 확인하고 싶은 경우 |
| **과거 데이터 보정** | 과거 활동이 누락되었다고 판단되는 경우 |

#### 수동 갱신 수집 범위

```text
수동 갱신 수집 범위 = GitHub 가입일 ~ 현재 (전체 기간)
```

> [!NOTE]
> 수동 갱신에는 **5분 쿨다운**이 적용됩니다.

---

<a id="faq"></a>
## ❓ FAQ

### Q1. 로그인 없이도 사용할 수 있나요?

**A.** 다른 사용자의 프로필 조회와 전체 랭킹 확인은 로그인 없이 가능합니다. 다만 본인 프로필 등록과 수동 갱신은 GitHub 로그인이 필요합니다.

### Q2. 왜 내 커밋 수가 GitHub 잔디와 다른가요?

**A.** Git Ranker는 GitHub의 **Contributions** 기준을 따릅니다. Contribution으로 인정되지 않는 커밋(예: Fork 저장소 커밋, 등록되지 않은 이메일 커밋)은 집계되지 않을 수 있습니다.

### Q3. Private 저장소 활동도 집계되나요?

**A.** 아니요. Git Ranker는 **Public 활동**만 집계합니다.

### Q4. 티어가 갑자기 떨어질 수 있나요?

**A.** 상위 티어(PLATINUM ~ CHALLENGER)는 상대 평가를 포함하므로, 전체 사용자 분포 변화에 따라 티어가 변동될 수 있습니다. IRON ~ GOLD는 점수 기반으로 결정됩니다.

### Q5. GitHub 닉네임을 바꾸면 데이터가 사라지나요?

**A.** 사라지지 않습니다. Git Ranker는 GitHub의 고유 식별자(`Node ID`)를 기준으로 사용자 데이터를 연결합니다.

### Q6. 회원 탈퇴를 하면 데이터도 삭제되나요?

**A.** 네. 탈퇴 시 사용자 계정과 활동 로그, 관련 인증 토큰이 함께 삭제되며 복구할 수 없습니다.

### Q7. 수동 갱신은 얼마나 자주 가능한가요?

**A.** 수동 갱신은 5분 쿨다운이 적용됩니다.

---

<a id="contributing"></a>
## 🤝 Contributing

Git Ranker는 오픈소스 프로젝트입니다. 버그 제보, 기능 제안, 토론 참여를 환영합니다.

### 버그 리포트

[Bug Report](https://github.com/alexization/git-ranker/issues/new?template=bug_report.yml) 템플릿으로 제보해 주세요.

> [!TIP]
> 빠른 해결을 위해 문제 설명, 재현 방법, 기대 동작/실제 동작, 실행 환경, 스크린샷 또는 로그를 함께 남겨주세요.

### 기능 제안

[Feature Request](https://github.com/alexization/git-ranker/issues/new?template=feature_request.yml)로 아이디어를 제안해 주세요.

### 보안 취약점 제보

보안 관련 이슈는 공개 이슈 대신 아래 메일로 전달해 주세요.

📧 **alexization@gmail.com**

### 일반 문의 및 토론

질문이나 의견은 [Discussions](https://github.com/alexization/git-ranker/discussions)를 이용해 주세요.

---

<a id="roadmap"></a>
## 🗺️ Roadmap

Git Ranker는 계속 발전하고 있습니다. 다음 기능을 준비 중입니다.

- [ ] **커밋 메시지 감정/의도 분석**: 커밋 메시지 분석으로 개발 성향 태그 제공
- [ ] **개발자 페르소나 군집화**: 활동 데이터 기반 유사 개발자 그룹 매칭
- [ ] **한 줄 요약 생성**: 주 사용 언어와 커밋 키워드를 바탕으로 소개 문구 생성
- [ ] **시즌제 랭크 시스템**: 6개월/1년 단위 시즌 도입
- [ ] **티어별 배지 애니메이션 고도화**: 상위 티어 중심 시각 효과 강화

---

<a id="license"></a>
## 📜 License

This project is licensed under the **MIT License**. See [LICENSE](LICENSE) for details.

---

<a id="contact"></a>
## 📬 Contact

- **Website**: [git-ranker.com](https://www.git-ranker.com)
- **GitHub**: [@alexization](https://github.com/alexization)
- **Email**: alexization@gmail.com

---

<p align="center">
  <strong>⭐ 이 프로젝트가 마음에 드셨다면 Star를 눌러주세요!</strong>
</p>

<p align="center">
  <a href="https://github.com/alexization/git-ranker">
    <img src="https://img.shields.io/github/stars/alexization/git-ranker?style=social" alt="GitHub Stars"/>
  </a>
</p>
