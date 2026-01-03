export function getTierIconClass(tier) {
    switch (tier) {
        case 'CHALLENGER': return 'fas fa-crown';
        case 'MASTER': return 'fas fa-star';
        case 'EMERALD':
        case 'GOLD':
        case 'SILVER':
        case 'BRONZE':
        case 'PLATINUM': return 'fas fa-medal';
        case 'DIAMOND': return 'fas fa-gem';
        default: return 'fas fa-shield-alt';
    }
}

export function formatDateTime(date) {
    const d = new Date(date);
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    const hour = String(d.getHours()).padStart(2, '0');
    const min = String(d.getMinutes()).padStart(2, '0');

    return `${year}-${month}-${day} ${hour}:${min}`;
}

export function formatNumber(num) {
    return num ? num.toLocaleString() : '0';
}

export function calculateNextRefreshTime(lastFullScanAt) {
    const lastScanDate = new Date(lastFullScanAt);
    // 쿨타임 7일
    const cooldownTime = 7 * 24 * 60 * 60 * 1000;
    return new Date(lastScanDate.getTime() + cooldownTime);
}

export function isRefreshAvailable(lastFullScanAt) {
    const nextTime = calculateNextRefreshTime(lastFullScanAt);
    return new Date() >= nextTime;
}