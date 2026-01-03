export function getTierIconClass(tier) {
    switch (tier) {
        case 'CHALLENGER':
            return 'fas fa-crown';
        case 'MASTER':
            return 'fas fa-star';
        case 'EMERALD':
        case 'GOLD':
        case 'SILVER':
        case 'BRONZE':
        case 'PLATINUM':
            return 'fas fa-medal';
        case 'DIAMOND':
            return 'fas fa-gem';
        default:
            return 'fas fa-shield-alt';
    }
}

export function formatDateTime(date) {
    const d = new Date(date);
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    const hour = String(d.getHours()).padStart(2, '0');
    const min = String(d.getMinutes()).padStart(2, '0');

    return `${month}-${day} ${hour}:${min}`;
}

export function formatNumber(num) {
    return num ? num.toLocaleString() : '0';
}

export function calculateNextRefreshTime(lastFullScanAt) {
    const lastScanDate = new Date(lastFullScanAt);
    const cooldownTime = 7 * 24 * 60 * 60 * 1000;
    return new Date(lastScanDate.getTime() + cooldownTime);
}

export function isRefreshAvailable(lastFullScanAt) {
    const nextTime = calculateNextRefreshTime(lastFullScanAt);
    return new Date() >= nextTime;
}

export function animateCountUp(element, target, duration = 1500) {
    if (!element) return;

    const targetNum = typeof target === 'string' ? parseInt(target.replace(/,/g, ''), 10) : target;
    const startNum = 0;
    const startTime = performance.now();

    const easeOutQuart = (x) => 1 - Math.pow(1 - x, 4);

    function update(currentTime) {
        const elapsed = currentTime - startTime;
        const progress = Math.min(elapsed / duration, 1);
        const ease = easeOutQuart(progress);

        const currentNum = Math.floor(startNum + (targetNum - startNum) * ease);
        element.innerText = currentNum.toLocaleString();

        if (progress < 1) {
            requestAnimationFrame(update);
        } else {
            element.innerText = targetNum.toLocaleString();
        }
    }

    requestAnimationFrame(update);
}

const RECENT_SEARCH_KEY = 'git_ranker_recent_searches';
const MAX_RECENT_ITEMS = 8;

export function getRecentSearches() {
    const json = localStorage.getItem(RECENT_SEARCH_KEY);
    return json ? JSON.parse(json) : [];
}

export function saveRecentSearch(username) {
    let searches = getRecentSearches();
    searches = searches.filter(item => item !== username);
    searches.unshift(username);

    if (searches.length > MAX_RECENT_ITEMS) {
        searches = searches.slice(0, MAX_RECENT_ITEMS);
    }

    localStorage.setItem(RECENT_SEARCH_KEY, JSON.stringify(searches));
}

export function removeRecentSearch(username) {
    let searches = getRecentSearches();
    searches = searches.filter(item => item !== username);
    localStorage.setItem(RECENT_SEARCH_KEY, JSON.stringify(searches));
}

export function clearAllRecentSearches() {
    localStorage.removeItem(RECENT_SEARCH_KEY);
}