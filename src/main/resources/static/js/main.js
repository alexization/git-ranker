import * as Api from './api.js';
import * as Ui from './ui.js';
import * as Utils from './utils.js';

let userDetailModal = null;
let currentFocus = -1;

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

    Ui.updateChartTheme();
}

function updateThemeIcon(isDark) {
    const btn = document.getElementById('themeToggle');
    if (btn) {
        btn.innerHTML = isDark ? '<i class="fas fa-sun"></i>' : '<i class="fas fa-moon"></i>';
    }
}

function renderRecentSearches() {
    const listEl = document.getElementById('recentList');
    const boxEl = document.getElementById('recentSearchBox');
    const searches = Utils.getRecentSearches();

    if (searches.length === 0) {
        boxEl.classList.add('hidden');
        return;
    }

    listEl.innerHTML = '';
    searches.forEach((username, index) => {
        const li = document.createElement('li');
        li.className = 'recent-item';
        li.setAttribute('data-index', index);

        li.innerHTML = `
            <span class="recent-name">${username}</span>
            <button class="btn-delete-item" data-user="${username}" tabindex="-1">
                <i class="fas fa-times"></i>
            </button>
        `;

        li.onclick = () => {
            document.getElementById('usernameInput').value = username;
            handleRegisterUser();
            boxEl.classList.add('hidden');
        };

        const delBtn = li.querySelector('.btn-delete-item');
        delBtn.onclick = (e) => {
            e.stopPropagation();
            Utils.removeRecentSearch(username);
            renderRecentSearches();
            document.getElementById('usernameInput').focus();
        };

        listEl.appendChild(li);
    });

    boxEl.classList.remove('hidden');
}

function addActive(items) {
    if (!items) return false;
    removeActive(items);
    if (currentFocus >= items.length) currentFocus = 0;
    if (currentFocus < 0) currentFocus = items.length - 1;
    items[currentFocus].classList.add('active');
}

function removeActive(items) {
    for (let i = 0; i < items.length; i++) {
        items[i].classList.remove('active');
    }
}

function triggerShake() {
    const wrapper = document.querySelector('.toss-input-group');
    wrapper.classList.remove('shake');
    void wrapper.offsetWidth;
    wrapper.classList.add('shake');
    setTimeout(() => {
        wrapper.classList.remove('shake');
    }, 400);
}

window.clearAllRecentSearches = () => {
    Utils.clearAllRecentSearches();
    renderRecentSearches();
};

function updateUrlState(username) {
    const currentUrlParams = new URLSearchParams(window.location.search);
    if (currentUrlParams.get('username') !== username) {
        const newUrl = `${window.location.pathname}?username=${username}`;
        window.history.pushState({username: username}, '', newUrl);
    }
}

function checkUrlParams() {
    const params = new URLSearchParams(window.location.search);
    const username = params.get('username');
    if (username) {
        document.getElementById('usernameInput').value = username;
        handleRegisterUser(false);
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

async function handleRegisterUser(pushHistory = true) {
    const usernameInput = document.getElementById('usernameInput');
    const username = usernameInput.value.trim();

    if (!username) {
        triggerShake();
        usernameInput.focus();
        return;
    }

    document.getElementById('recentSearchBox').classList.add('hidden');
    currentFocus = -1;

    Ui.showLoading(true);
    try {
        const result = await Api.registerUser(username);
        if (result.result === 'SUCCESS') {
            Utils.saveRecentSearch(username);
            if (pushHistory) updateUrlState(username);

            Ui.renderUserResult(result.data);
            Ui.showResultSection();

            // [핵심 수정] 화면이 확실히 뜬 뒤에 차트를 그려야 애니메이션이 동작함
            requestAnimationFrame(() => {
                Ui.createRadarChart(result.data);
            });

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

            // [핵심 수정] 갱신 시에도 동일하게 적용
            requestAnimationFrame(() => {
                Ui.createRadarChart(result.data);
            });
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
    initTheme();
    document.getElementById('themeToggle').addEventListener('click', toggleTheme);

    window.registerUser = () => handleRegisterUser(true);

    const input = document.getElementById('usernameInput');
    const btnClear = document.getElementById('btnClear');
    const recentBox = document.getElementById('recentSearchBox');

    input.addEventListener('keydown', (e) => {
        const list = document.getElementById('recentList');
        let items = list ? list.getElementsByTagName('li') : null;

        if (e.key === 'ArrowDown') {
            e.preventDefault();
            currentFocus++;
            addActive(items);
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            currentFocus--;
            addActive(items);
        } else if (e.key === 'Enter') {
            e.preventDefault();
            if (currentFocus > -1 && items) {
                if (items[currentFocus]) items[currentFocus].click();
            } else {
                handleRegisterUser(true);
            }
        } else if (e.key === 'Escape') {
            recentBox.classList.add('hidden');
            currentFocus = -1;
        } else if ((e.key === 'Delete' || (e.shiftKey && e.key === 'Delete')) && currentFocus > -1) {
            e.preventDefault();
            const targetUser = items[currentFocus].querySelector('.btn-delete-item').getAttribute('data-user');
            Utils.removeRecentSearch(targetUser);
            renderRecentSearches();
            items = list.getElementsByTagName('li');
            if (currentFocus >= items.length) currentFocus = items.length - 1;
            if (items.length > 0) addActive(items);
            else {
                currentFocus = -1;
                recentBox.classList.add('hidden');
            }
        }
    });

    input.addEventListener('input', () => {
        if (input.value.length > 0) btnClear.classList.remove('hidden');
        else btnClear.classList.add('hidden');
        currentFocus = -1;
    });

    input.addEventListener('focus', () => {
        renderRecentSearches();
    });

    btnClear.addEventListener('click', () => {
        input.value = '';
        input.focus();
        btnClear.classList.add('hidden');
        renderRecentSearches();
    });

    document.addEventListener('click', (e) => {
        if (!input.contains(e.target) && !recentBox.contains(e.target)) {
            recentBox.classList.add('hidden');
            currentFocus = -1;
        }
    });

    document.addEventListener('requestRefreshUser', handleRefreshUser);
    document.addEventListener('requestUserDetail', (e) => handleUserDetail(e.detail));

    window.addEventListener('popstate', (event) => {
        if (event.state && event.state.username) {
            document.getElementById('usernameInput').value = event.state.username;
            handleRegisterUser(false);
        } else {
            location.reload();
        }
    });

    handleLoadRankings(0);
    startCountdownTimer();
    checkUrlParams();
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