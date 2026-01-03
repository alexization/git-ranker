import * as Api from './api.js';
import * as Ui from './ui.js';

// === Global State ===
let userDetailModal;

// === Event Handlers ===
async function handleLoadRankings(page) {
    try {
        const result = await Api.fetchRankings(page);
        if (result.result === 'SUCCESS') {
            Ui.renderRankingTable(result.data.rankings);
            Ui.renderPagination(result.data.pageInfo, handleLoadRankings);
        } else {
            console.error('랭킹 조회 실패');
        }
    } catch (error) {
        console.error('Ranking Load Error:', error);
    }
}

async function handleRegisterUser() {
    const usernameInput = document.getElementById('usernameInput');
    const username = usernameInput.value.trim();
    if (!username) {
        alert('GitHub Username을 입력해주세요.');
        return;
    }

    Ui.showLoading(true);
    try {
        const result = await Api.registerUser(username);
        if (result.result === 'SUCCESS') {
            Ui.renderUserResult(result.data);
        } else {
            alert(`오류 발생: ${result.error.message}`);
        }
    } catch (error) {
        alert('서버와 통신 중 오류가 발생했습니다.');
    } finally {
        Ui.showLoading(false);
    }
}

async function handleRefreshUser() {
    const username = document.getElementById('resUsername').value;
    if (!username) return;

    if (!confirm('데이터를 갱신하시겠습니까? (갱신 후 7일간 다시 갱신할 수 없습니다.)')) return;

    Ui.showLoading(true);
    try {
        const result = await Api.refreshUser(username);
        if (result.result === 'SUCCESS') {
            alert('갱신이 완료되었습니다!');
            Ui.renderUserResult(result.data);
        } else {
            alert(result.error.message);
        }
    } catch (error) {
        alert('갱신 요청 중 오류가 발생했습니다.');
    } finally {
        Ui.showLoading(false);
    }
}

async function handleUserDetailClick(e) {
    // 동적으로 생성된 요소에 대한 이벤트 위임 또는 개별 바인딩 필요
    // 여기서는 renderRankingTable에서 class='user-detail-btn'을 주었으므로 위임 방식 사용
    if (e.target.classList.contains('user-detail-btn')) {
        const username = e.target.dataset.username;
        const modalElement = document.getElementById('userDetailModal');
        // Bootstrap Modal 인스턴스 싱글톤 관리
        if (!userDetailModal) {
            userDetailModal = new bootstrap.Modal(modalElement);
        }

        try {
            const result = await Api.getUserDetail(username);
            if (result.result === 'SUCCESS') {
                Ui.renderUserDetailModal(result.data, userDetailModal);
            } else {
                alert('사용자 정보를 불러올 수 없습니다.');
            }
        } catch (error) {
            alert('상세 정보를 불러오는 중 오류가 발생했습니다.');
        }
    }
}

// === Initialization ===
document.addEventListener('DOMContentLoaded', () => {
    // Bootstrap Popover 초기화
    const popoverTriggerList = document.querySelectorAll('[data-bs-toggle="popover"]');
    [...popoverTriggerList].map(popoverTriggerEl => new bootstrap.Popover(popoverTriggerEl));

    // 이벤트 리스너 등록
    document.getElementById('usernameInput').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') handleRegisterUser();
    });

    // HTML onclick 속성 대신 JS에서 바인딩 (더 안전하고 깔끔함)
    document.querySelector('button[onclick="registerUser()"]').onclick = handleRegisterUser;
    document.getElementById('btnRefresh').onclick = handleRefreshUser;

    // 동적 테이블 이벤트 위임
    document.getElementById('rankingTableBody').addEventListener('click', handleUserDetailClick);

    // 배지 복사 기능 (간단해서 여기에 유지하거나 Utils로 이동 가능)
    window.copyBadgeMarkdown = () => {
        const nodeId = document.getElementById('resNodeId').value;
        const origin = window.location.origin;
        const markdown = `[![Git Ranker](${origin}/api/v1/badges/${nodeId})](https://www.git-ranker.com)`;

        navigator.clipboard.writeText(markdown)
            .then(() => alert('README.md 코드가 복사되었습니다!'))
            .catch(() => alert('복사에 실패했습니다.'));
    };

    // 초기 데이터 로드
    handleLoadRankings(0);
    startCountdownTimer();
});

function startCountdownTimer() {
    function update() {
        const now = new Date();
        const nextHour = new Date(now);
        nextHour.setHours(now.getHours() + 1);
        nextHour.setMinutes(0);
        nextHour.setSeconds(0);
        nextHour.setMilliseconds(0);
        const diff = nextHour - now;

        const m = Math.floor(diff / 60000);
        const s = Math.floor((diff % 60000) / 1000);

        const timerEl = document.getElementById('countdownTimer');
        if (timerEl) timerEl.innerText = `${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`;
    }
    update();
    setInterval(update, 1000);
}