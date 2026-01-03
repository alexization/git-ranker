import * as Api from './api.js';
import * as Ui from './ui.js';

let userDetailModal = null;

// [신규] 테마 관리 로직
function initTheme() {
    const savedTheme = localStorage.getItem('theme');
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;

    if (savedTheme === 'dark' || (!savedTheme && prefersDark)) {
        document.documentElement.setAttribute('data-theme', 'dark');
        updateThemeIcon(true);
    } else {
        document.documentElement.setAttribute('data-theme', 'light');
        updateThemeIcon(false);
    }
}

function toggleTheme() {
    const currentTheme = document.documentElement.getAttribute('data-theme');
    const newTheme = currentTheme === 'dark' ? 'light' : 'dark';

    document.documentElement.setAttribute('data-theme', newTheme);
    localStorage.setItem('theme', newTheme);
    updateThemeIcon(newTheme === 'dark');

    // 차트 업데이트 트리거 (Ui.js에 있는 함수 호출)
    Ui.updateChartTheme();
}

function updateThemeIcon(isDark) {
    const btn = document.getElementById('themeToggle');
    if (btn) {
        btn.innerHTML = isDark ? '<i class="fas fa-sun"></i>' : '<i class="fas fa-moon"></i>';
    }
}

async function handleLoadRankings(page) {
    try {
        const result = await Api.fetchRankings(page);
        if (result.result === 'SUCCESS') {
            Ui.renderRankingTable(result.data.rankings);
            Ui.renderPagination(result.data.pageInfo, handleLoadRankings);
        }
    } catch (error) {
        console.error('Ranking Load Error:', error);
    }
}

async function handleRegisterUser() {
    const usernameInput = document.getElementById('usernameInput');
    const username = usernameInput.value.trim();
    if (!username) {
        Ui.showToast('GitHub Username을 입력해주세요.');
        return;
    }

    Ui.showLoading(true);
    try {
        const result = await Api.registerUser(username);
        if (result.result === 'SUCCESS') {
            Ui.renderUserResult(result.data);
            Ui.showResultSection();
            const resultSection = document.getElementById('resultSection');
            resultSection.scrollIntoView({behavior: 'smooth', block: 'center'});
        } else {
            Ui.showToast(`오류: ${result.error.message}`);
        }
    } catch (error) {
        Ui.showToast('서버와 통신 중 오류가 발생했습니다.');
    } finally {
        Ui.showLoading(false);
    }
}

async function handleRefreshUser() {
    const username = document.getElementById('resUsername').value;
    if (!username) return;

    if (!confirm('데이터를 갱신하시겠습니까? (갱신 후 7일간 쿨타임)')) return;

    Ui.showLoading(true);
    try {
        const result = await Api.refreshUser(username);
        if (result.result === 'SUCCESS') {
            Ui.showToast('✅ 갱신이 완료되었습니다!');
            Ui.renderUserResult(result.data);
            Ui.showResultSection();
        } else {
            Ui.showToast(result.error.message);
        }
    } catch (error) {
        Ui.showToast('갱신 요청 중 오류가 발생했습니다.');
    } finally {
        Ui.showLoading(false);
    }
}

async function handleUserDetail(username) {
    const modalElement = document.getElementById('userDetailModal');
    if (!userDetailModal) {
        userDetailModal = new bootstrap.Modal(modalElement);
    }

    try {
        const result = await Api.getUserDetail(username);
        if (result.result === 'SUCCESS') {
            Ui.renderUserDetailModal(result.data, userDetailModal);
        } else {
            Ui.showToast('사용자 정보를 불러올 수 없습니다.');
        }
    } catch (error) {
        Ui.showToast('상세 정보를 불러오는 중 오류가 발생했습니다.');
    }
}

window.copyBadgeMarkdown = () => {
    const nodeId = document.getElementById('resNodeId').value;
    const origin = window.location.origin;
    const markdown = `[![Git Ranker](${origin}/api/v1/badges/${nodeId})](https://www.git-ranker.com)`;

    navigator.clipboard.writeText(markdown)
        .then(() => Ui.showToast('README 마크다운 코드가 복사되었습니다!'))
        .catch(() => Ui.showToast('복사에 실패했습니다.'));
};

document.addEventListener('DOMContentLoaded', () => {
    // [신규] 테마 초기화
    initTheme();
    document.getElementById('themeToggle').addEventListener('click', toggleTheme);

    window.registerUser = handleRegisterUser;

    document.getElementById('usernameInput').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') handleRegisterUser();
    });

    document.addEventListener('requestRefreshUser', handleRefreshUser);
    document.addEventListener('requestUserDetail', (e) => handleUserDetail(e.detail));

    handleLoadRankings(0);
    startCountdownTimer();
});

function startCountdownTimer() {
    function update() {
        const now = new Date();
        const nextHour = new Date(now);
        nextHour.setHours(now.getHours() + 1, 0, 0, 0);
        const diff = nextHour - now;
        const m = Math.floor(diff / 60000);
        const s = Math.floor((diff % 60000) / 1000);
        const timerEl = document.getElementById('countdownTimer');
        if (timerEl) timerEl.innerText = `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
    }

    update();
    setInterval(update, 1000);
}