import * as Api from './api.js';
import * as Ui from './ui.js';
import * as Utils from './utils.js';

let userDetailModal = null;
let currentFocus = -1;
let currentTier = 'ALL';

// Drag Scrolling Variables
let isDown = false;
let startX;
let scrollLeft;
let isDragging = false;

function initTheme() {
    const savedTheme = localStorage.getItem('theme');
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    const isDark = savedTheme === 'dark' || (!savedTheme && prefersDark);

    if (isDark) {
        document.documentElement.setAttribute('data-theme', 'dark');
    } else {
        document.documentElement.setAttribute('data-theme', 'light');
    }
    updateThemeUI(isDark);
}

function toggleTheme() {
    const currentTheme = document.documentElement.getAttribute('data-theme');
    const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
    const isDark = newTheme === 'dark';

    document.documentElement.setAttribute('data-theme', newTheme);
    localStorage.setItem('theme', newTheme);

    updateThemeUI(isDark);
    Ui.updateChartTheme();
}

function updateThemeUI(isDark) {
    const btn = document.getElementById('themeToggle');
    const metaThemeColor = document.getElementById('metaThemeColor');

    if (btn) btn.innerHTML = isDark ? '<i class="fas fa-sun"></i>' : '<i class="fas fa-moon"></i>';

    if (metaThemeColor) {
        metaThemeColor.setAttribute('content', isDark ? '#101010' : '#F2F4F6');
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
        li.innerHTML = `<span class="recent-name">${username}</span><button class="btn-delete-item" data-user="${username}" tabindex="-1"><i class="fas fa-times"></i></button>`;
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
        const result = await Api.fetchRankings(page, currentTier);
        if (result.result === 'SUCCESS') {
            Ui.renderRankingTable(result.data.rankings);
            Ui.renderPagination(result.data.pageInfo, (newPage) => handleLoadRankings(newPage));

            if (page > 0) {
                const rankingHeader = document.getElementById('rankingHeader');
                if (rankingHeader) {
                    rankingHeader.scrollIntoView({behavior: 'smooth', block: 'start'});
                }
            }
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
            requestAnimationFrame(() => {
                Ui.createRadarChart(result.data);
            });
        } else {
            // 백엔드 에러 메시지 사용 및 에러 상태(true) 전달
            const msg = result.error ? result.error.message : '일시적인 오류가 발생했어요.';
            Ui.showToast(msg, true);
        }
    } catch (error) {
        Ui.showToast('서버와 연결할 수 없어요. 잠시 후 다시 시도해주세요.', true);
    } finally {
        Ui.showLoading(false);
    }
}

async function handleRefreshUser() {
    const username = document.getElementById('resUsername').value;
    if (!username) return;
    Ui.showConfirmModal(async () => {
        Ui.showLoading(true);
        try {
            const result = await Api.refreshUser(username);
            if (result.result === 'SUCCESS') {
                Ui.showToast('데이터를 최신으로 업데이트했어요.');
                Ui.renderUserResult(result.data);
                Ui.showResultSection();
                requestAnimationFrame(() => {
                    Ui.createRadarChart(result.data);
                });
            } else {
                const msg = result.error ? result.error.message : '갱신에 실패했어요.';
                Ui.showToast(msg, true);
            }
        } catch (error) {
            Ui.showToast('갱신 요청 중 오류가 발생했어요.', true);
        } finally {
            Ui.showLoading(false);
        }
    });
}

async function handleUserDetail(username) {
    const modalElement = document.getElementById('userDetailModal');
    if (!userDetailModal) userDetailModal = new bootstrap.Modal(modalElement);
    try {
        const result = await Api.getUserDetail(username);
        if (result.result === 'SUCCESS') {
            Ui.renderUserDetailModal(result.data, userDetailModal);
        } else {
            Ui.showToast('사용자 정보를 불러올 수 없어요.', true);
        }
    } catch (error) {
        Ui.showToast('상세 정보를 불러오는 중 오류가 발생했어요.', true);
    }
}

window.copyBadgeMarkdown = () => {
    const nodeId = document.getElementById('resNodeId').value;
    const origin = window.location.origin;
    const markdown = `[![Git Ranker](${origin}/api/v1/badges/${nodeId})](https://www.git-ranker.com)`;
    navigator.clipboard.writeText(markdown)
        .then(() => Ui.showToast('README 코드를 복사했어요'))
        .catch(() => Ui.showToast('복사에 실패했어요.', true));
};

document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    document.getElementById('themeToggle').addEventListener('click', toggleTheme);
    window.registerUser = () => handleRegisterUser(true);
    const input = document.getElementById('usernameInput');
    const btnClear = document.getElementById('btnClear');
    const recentBox = document.getElementById('recentSearchBox');

    const tabsContainer = document.getElementById('tierTabs');
    const scrollHintLeft = document.getElementById('scrollHintLeft');
    const scrollHintRight = document.getElementById('scrollHintRight');

    const tabs = document.querySelectorAll('.tab-item');
    tabs.forEach(tab => {
        tab.addEventListener('click', (e) => {
            if (isDragging) {
                e.preventDefault();
                return;
            }

            const clickedTier = tab.getAttribute('data-tier');

            if (tab.classList.contains('active')) {
                if (clickedTier === 'ALL') return;
                tabs.forEach(t => t.classList.remove('active'));
                const allTab = document.querySelector('.tab-item[data-tier="ALL"]');
                if (allTab) allTab.classList.add('active');
                currentTier = 'ALL';
            } else {
                tabs.forEach(t => t.classList.remove('active'));
                tab.classList.add('active');
                currentTier = clickedTier;
            }

            handleLoadRankings(0);
        });
    });

    if (tabsContainer) {
        tabsContainer.addEventListener('mousedown', (e) => {
            isDown = true;
            isDragging = false;
            tabsContainer.classList.add('active');
            startX = e.pageX - tabsContainer.offsetLeft;
            scrollLeft = tabsContainer.scrollLeft;
        });

        tabsContainer.addEventListener('mouseleave', () => {
            isDown = false;
            tabsContainer.classList.remove('active');
        });

        tabsContainer.addEventListener('mouseup', () => {
            isDown = false;
            tabsContainer.classList.remove('active');
            setTimeout(() => isDragging = false, 50);
        });

        tabsContainer.addEventListener('mousemove', (e) => {
            if (!isDown) return;
            e.preventDefault();
            const x = e.pageX - tabsContainer.offsetLeft;
            const walk = (x - startX) * 1.5;
            tabsContainer.scrollLeft = scrollLeft - walk;
            if (Math.abs(walk) > 5) {
                isDragging = true;
            }
        });

        const updateScrollHints = () => {
            const maxScrollLeft = tabsContainer.scrollWidth - tabsContainer.clientWidth;

            if (tabsContainer.scrollLeft > 5) {
                scrollHintLeft.style.opacity = '1';
                scrollHintLeft.classList.add('visible');
            } else {
                scrollHintLeft.style.opacity = '0';
                scrollHintLeft.classList.remove('visible');
            }

            if (maxScrollLeft - tabsContainer.scrollLeft > 2) {
                scrollHintRight.style.opacity = '1';
                scrollHintRight.classList.add('visible');
            } else {
                scrollHintRight.style.opacity = '0';
                scrollHintRight.classList.remove('visible');
            }
        };

        tabsContainer.addEventListener('scroll', updateScrollHints);
        window.addEventListener('resize', updateScrollHints);

        scrollHintLeft.addEventListener('click', () => {
            tabsContainer.scrollBy({left: -200, behavior: 'smooth'});
        });
        scrollHintRight.addEventListener('click', () => {
            tabsContainer.scrollBy({left: 200, behavior: 'smooth'});
        });

        updateScrollHints();
    }

    input.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            if (currentFocus > -1) {
                const list = document.getElementById('recentList');
                if (list && !list.classList.contains('hidden')) {
                    const items = list.getElementsByTagName('li');
                    if (items[currentFocus]) {
                        items[currentFocus].click();
                        return;
                    }
                }
            }
            handleRegisterUser(true);
            return;
        }

        const list = document.getElementById('recentList');
        if (!list || list.classList.contains('hidden')) return;

        const items = Array.from(list.getElementsByTagName('li'));
        if (items.length === 0) return;

        if (e.key === 'ArrowRight') {
            e.preventDefault();
            currentFocus++;
            if (currentFocus >= items.length) currentFocus = 0;
            addActive(items);
        } else if (e.key === 'ArrowLeft') {
            e.preventDefault();
            currentFocus--;
            if (currentFocus < 0) currentFocus = items.length - 1;
            addActive(items);
        } else if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
            e.preventDefault();

            if (currentFocus === -1) {
                currentFocus = 0;
                addActive(items);
                return;
            }

            const currentRect = items[currentFocus].getBoundingClientRect();
            const currentCenterX = currentRect.left + currentRect.width / 2;
            const currentCenterY = currentRect.top + currentRect.height / 2;

            let closestIndex = -1;
            let minDistance = Number.MAX_VALUE;

            items.forEach((item, index) => {
                if (index === currentFocus) return;

                const targetRect = item.getBoundingClientRect();
                const targetCenterX = targetRect.left + targetRect.width / 2;
                const targetCenterY = targetRect.top + targetRect.height / 2;

                const isBelow = targetRect.top >= currentRect.bottom - 5;
                const isAbove = targetRect.bottom <= currentRect.top + 5;

                let isValidCandidate = false;
                if (e.key === 'ArrowDown' && isBelow) isValidCandidate = true;
                if (e.key === 'ArrowUp' && isAbove) isValidCandidate = true;

                if (isValidCandidate) {
                    const dist = Math.hypot(targetCenterX - currentCenterX, targetCenterY - currentCenterY);
                    const weightedDist = dist + Math.abs(targetCenterX - currentCenterX) * 0.5;

                    if (weightedDist < minDistance) {
                        minDistance = weightedDist;
                        closestIndex = index;
                    }
                }
            });

            if (closestIndex !== -1) {
                currentFocus = closestIndex;
                addActive(items);
            }
        } else if (e.key === 'Escape') {
            recentBox.classList.add('hidden');
            currentFocus = -1;
        } else if ((e.key === 'Delete' || (e.shiftKey && e.key === 'Delete')) && currentFocus > -1) {
            e.preventDefault();
            const targetUser = items[currentFocus].querySelector('.btn-delete-item').getAttribute('data-user');
            Utils.removeRecentSearch(targetUser);
            renderRecentSearches();

            const newItems = list.getElementsByTagName('li');
            if (currentFocus >= newItems.length) currentFocus = newItems.length - 1;
            if (newItems.length > 0) addActive(newItems);
            else {
                currentFocus = -1;
                recentBox.classList.add('hidden');
            }
        }
    });

    input.addEventListener('input', () => {
        if (input.value.length > 0) btnClear.classList.remove('hidden'); else btnClear.classList.add('hidden');
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