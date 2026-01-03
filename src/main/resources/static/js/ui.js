import { getTierIconClass, formatDateTime, formatNumber, isRefreshAvailable, calculateNextRefreshTime } from './utils.js';

// === 공통 UI 컴포넌트 ===
export function showLoading(isLoading) {
    const overlay = document.getElementById('loadingOverlay');
    overlay.style.display = isLoading ? 'flex' : 'none';
}

function updateStatWithDiff(statId, diffId, totalValue, diffValue) {
    const statEl = document.getElementById(statId);
    const diffEl = document.getElementById(diffId);

    if(statEl) statEl.innerText = formatNumber(totalValue);

    if (diffEl) {
        if (diffValue > 0) {
            diffEl.className = 'badge rounded-pill bg-success diff-badge';
            diffEl.innerText = `+${diffValue}`;
            diffEl.style.display = 'inline-block';
        } else if (diffValue < 0) {
            diffEl.className = 'badge rounded-pill bg-danger diff-badge';
            diffEl.innerText = `${diffValue}`;
            diffEl.style.display = 'inline-block';
        } else {
            diffEl.style.display = 'none';
        }
    }
}

// === 갱신 버튼 컴포넌트 ===
export function renderRefreshButton(lastFullScanAt) {
    const btn = document.getElementById('btnRefresh');
    const statusText = document.getElementById('refreshStatusText');
    const span = btn.querySelector('span');

    const lastScanDate = new Date(lastFullScanAt);
    const isAvailable = isRefreshAvailable(lastFullScanAt);
    const lastScanStr = formatDateTime(lastScanDate);

    if (isAvailable) {
        btn.disabled = false;
        btn.className = "btn btn-sm btn-outline-primary rounded-pill px-3 mb-2";
        span.innerText = "데이터 갱신하기";

        statusText.innerHTML = `
            마지막 갱신: ${lastScanStr}<br>
            <span class="text-primary fw-bold">지금 즉시 갱신 가능합니다!</span>
        `;
    } else {
        const nextTime = calculateNextRefreshTime(lastFullScanAt);
        const nextScanStr = formatDateTime(nextTime);

        btn.disabled = true;
        btn.className = "btn btn-sm btn-outline-secondary rounded-pill px-3 mb-2";
        span.innerText = "갱신 대기 중";

        statusText.innerHTML = `
            마지막 갱신: ${lastScanStr}<br>
            다음 갱신 가능: <span class="fw-bold">${nextScanStr}</span>
        `;
    }
}

// === 사용자 결과 카드 렌더링 ===
export function renderUserResult(data) {
    const section = document.getElementById('resultSection');
    section.classList.remove('d-none');

    document.getElementById('resNodeId').value = data.nodeId;
    document.getElementById('resUsername').value = data.username;
    document.getElementById('resProfileImage').src = data.profileImage;
    document.getElementById('resGithubLink').href = `https://github.com/${data.username}`;

    const tierText = document.getElementById('resTierText');
    tierText.innerText = data.tier;
    tierText.className = `fw-bold mb-1 text-${data.tier}`;

    document.getElementById('resPercentile').innerText = data.percentile ? data.percentile.toFixed(2) : '0.00';
    document.getElementById('resRanking').innerText = data.ranking;
    document.getElementById('resTotalScore').innerText = formatNumber(data.totalScore);

    updateStatWithDiff('statCommit', 'diffCommit', data.commitCount, data.diffCommitCount);
    updateStatWithDiff('statIssue', 'diffIssue', data.issueCount, data.diffIssueCount);
    updateStatWithDiff('statReview', 'diffReview', data.reviewCount, data.diffReviewCount);
    updateStatWithDiff('statPrOpen', 'diffPrOpen', data.prCount, data.diffPrCount);
    updateStatWithDiff('statPrMerged', 'diffPrMerged', data.mergedPrCount, data.diffMergedPrCount);

    const iconClass = getTierIconClass(data.tier);
    document.getElementById('resTierIcon').className = `${iconClass} tier-icon text-${data.tier}`;

    renderRefreshButton(data.lastFullScanAt);

    section.scrollIntoView({behavior: 'smooth', block: 'center'});
}

// === 랭킹 테이블 렌더링 ===
export function renderRankingTable(users) {
    const tbody = document.getElementById('rankingTableBody');
    tbody.innerHTML = '';

    if (!users || users.length === 0) {
        tbody.innerHTML = `<tr><td colspan="5" class="py-4">등록된 랭킹 데이터가 없습니다.</td></tr>`;
        return;
    }

    users.forEach((user) => {
        // [Tip] 복잡한 HTML 스트링은 Template Literal로 분리
        const row = `
            <tr>
                <td class="fw-bold">${user.ranking}</td>
                <td><span class="badge tier-badge text-${user.tier}">${user.tier}</span></td>
                <td class="text-start">
                    <img src="${user.profileImage}" class="rounded-circle me-2" width="30" height="30" alt="profile">
                    <a href="https://github.com/${user.username}" target="_blank" class="text-decoration-none text-dark fw-bold">
                        ${user.username}
                    </a>
                </td>
                <td class="fw-bold text-primary">${formatNumber(user.totalScore)}</td>
                <td>
                    <button class="btn btn-sm btn-outline-secondary user-detail-btn" data-username="${user.username}">
                        View
                    </button>
                </td>
            </tr>
        `;
        tbody.innerHTML += row;
    });
}

// === 모달 렌더링 ===
export function renderUserDetailModal(data, modalInstance) {
    document.getElementById('modalProfileImage').src = data.profileImage;
    document.getElementById('modalUsername').innerText = data.username;

    const tierEl = document.getElementById('modalTierBadge');
    tierEl.className = `d-inline-block px-3 py-1 rounded-pill fw-bold border mb-2 tier-badge text-${data.tier}`;
    tierEl.innerHTML = `<i class="${getTierIconClass(data.tier)} me-1"></i> ${data.tier}`;

    document.getElementById('modalRanking').innerText = data.ranking;
    document.getElementById('modalPercentile').innerText = data.percentile.toFixed(2);
    document.getElementById('modalTotalScore').innerText = formatNumber(data.totalScore);

    updateStatWithDiff('modalStatCommit', 'modalDiffCommit', data.commitCount, data.diffCommitCount);
    updateStatWithDiff('modalStatIssue', 'modalDiffIssue', data.issueCount, data.diffIssueCount);
    updateStatWithDiff('modalStatReview', 'modalDiffReview', data.reviewCount, data.diffReviewCount);
    updateStatWithDiff('modalStatPrOpen', 'modalDiffPrOpen', data.prCount, data.diffPrCount);
    updateStatWithDiff('modalStatPrMerged', 'modalDiffPrMerged', data.mergedPrCount, data.diffMergedPrCount);

    modalInstance.show();
}

// === 페이지네이션 ===
export function renderPagination(pageInfo, loadRankingsCallback) {
    const pagination = document.getElementById('pagination');
    pagination.innerHTML = '';

    const { currentPage, totalPages, isFirst, isLast } = pageInfo;
    const pageSize = 5;
    const startPage = Math.floor(currentPage / pageSize) * pageSize;
    const endPage = Math.min(startPage + pageSize, totalPages);

    // Helper for creating page item
    const createItem = (text, page, disabled = false, active = false) => {
        const li = document.createElement('li');
        li.className = `page-item ${disabled ? 'disabled' : ''} ${active ? 'active' : ''}`;
        const a = document.createElement('a');
        a.className = 'page-link';
        a.href = '#';
        a.innerHTML = text;
        if (!disabled) {
            a.onclick = (e) => {
                e.preventDefault();
                loadRankingsCallback(page);
            };
        }
        li.appendChild(a);
        return li;
    };

    pagination.appendChild(createItem('&laquo;', currentPage - 1, isFirst));

    for (let i = startPage; i < endPage; i++) {
        pagination.appendChild(createItem(i + 1, i, false, i === currentPage));
    }

    pagination.appendChild(createItem('&raquo;', currentPage + 1, isLast));
}