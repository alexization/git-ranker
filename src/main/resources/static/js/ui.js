import {
    animateCountUp,
    calculateNextRefreshTime,
    formatDateTime,
    formatNumber,
    getTierIconClass,
    isRefreshAvailable
} from './utils.js';

let radarChartInstance = null;

const isReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

export function updateChartTheme() {
    if (!radarChartInstance) return;

    const gridColor = getComputedStyle(document.documentElement).getPropertyValue('--chart-grid').trim();
    const textColor = getComputedStyle(document.documentElement).getPropertyValue('--chart-text').trim();

    radarChartInstance.options.scales.r.grid.color = gridColor;
    radarChartInstance.options.scales.r.angleLines.color = gridColor;
    radarChartInstance.options.scales.r.pointLabels.color = textColor;

    radarChartInstance.update();
}

export function showToast(message) {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = 'toss-toast';
    toast.innerHTML = message;
    container.appendChild(toast);

    setTimeout(() => {
        toast.classList.add('hide');
        toast.addEventListener('animationend', () => {
            toast.remove();
        });
    }, 3000);
}

export function showConfirmModal(onConfirm) {
    const modal = document.getElementById('customConfirmModal');
    const btnCancel = document.getElementById('btnModalCancel');
    const btnConfirm = document.getElementById('btnModalConfirm');

    modal.classList.remove('hidden');

    const closeModal = () => {
        modal.classList.add('hidden');
        btnCancel.onclick = null;
        btnConfirm.onclick = null;
    };

    btnCancel.onclick = closeModal;
    btnConfirm.onclick = () => {
        closeModal();
        onConfirm();
    };
}

window.captureAndDownload = async () => {
    const btn = document.querySelector('.btn-black');
    const originalText = btn.innerHTML;

    // 로딩 상태 표시
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 처리 중...';
    btn.disabled = true;

    try {
        const username = document.getElementById('resUsername').value;
        const profileSrc = document.getElementById('resProfileImage').src;
        const chartBase64 = radarChartInstance.toBase64Image();

        const stats = [
            {label: 'Commits', value: document.getElementById('statCommit').innerText},
            {label: 'PR Merged', value: document.getElementById('statPrMerged').innerText},
            {label: 'PR Open', value: document.getElementById('statPrOpen').innerText},
            {label: 'Reviews', value: document.getElementById('statReview').innerText},
            {label: 'Issues', value: document.getElementById('statIssue').innerText}
        ];

        const reportContainer = document.createElement('div');
        reportContainer.id = 'report-export-view';
        document.body.appendChild(reportContainer);

        reportContainer.innerHTML = `
            <div class="simple-report-card">
                <div class="sim-header">
                    <div class="sim-brand"><i class="fab fa-github"></i> Git Ranker</div>
                    <div class="sim-date">${new Date().toISOString().split('T')[0]}</div>
                </div>
                <div class="sim-body">
                    <div class="sim-profile">
                        <img src="${profileSrc}" class="sim-avatar">
                        <div class="sim-username">${username}</div>
                    </div>
                    <div class="sim-chart">
                        <img src="${chartBase64}" style="width: 100%; height: auto; display: block;">
                    </div>
                    <div class="sim-stats-row">
                        ${stats.map(s => `
                            <div class="sim-stat-item">
                                <div class="sim-stat-label">${s.label}</div>
                                <div class="sim-stat-value">${s.value}</div>
                            </div>
                        `).join('')}
                    </div>
                </div>
                <div class="sim-footer">Get your tier at <strong>git-ranker.com</strong></div>
            </div>
        `;

        // 이미지 생성 (html2canvas)
        const canvas = await html2canvas(reportContainer, {
            backgroundColor: null,
            scale: 2,
            useCORS: true,
            logging: false
        });

        // Blob 변환 후 처리 (비동기 콜백 내부에서 에러 발생 시 멈춤 방지를 위해 Promise로 감싸지 않고 직접 처리하되 finally 보장)
        canvas.toBlob(async (blob) => {
            try {
                if (!blob) throw new Error("Blob creation failed");

                const file = new File([blob], `GitRanker_${username}.png`, {type: 'image/png'});

                // Case A: 모바일/지원 브라우저 (Web Share API 사용)
                if (navigator.share && navigator.canShare && navigator.canShare({files: [file]})) {
                    try {
                        await navigator.share({
                            title: 'Git Ranker 결과',
                            text: `${username}님의 개발자 전투력입니다!`,
                            files: [file]
                        });
                        showToast('<i class="fas fa-share-alt"></i> 공유 화면을 열었어요');
                    } catch (err) {
                        if (err.name !== 'AbortError') showToast('공유하기를 실패했어요.');
                    }
                }
                // Case B: PC (클립보드 복사 시도) - HTTPS 환경 필요
                else if (typeof ClipboardItem !== 'undefined' && navigator.clipboard && navigator.clipboard.write) {
                    try {
                        await navigator.clipboard.write([
                            new ClipboardItem({'image/png': blob})
                        ]);
                        showToast('<i class="fas fa-check-circle" style="color:#4ADE80"></i> 이미지를 복사했어요! (Ctrl+V)');
                    } catch (err) {
                        // 클립보드 실패 시(보안 정책 등) 다운로드로 Fallback
                        console.warn("Clipboard failed, falling back to download", err);
                        triggerDownload(canvas, username);
                    }
                }
                // Case C: 그 외 (다운로드)
                else {
                    triggerDownload(canvas, username);
                }
            } catch (innerErr) {
                console.error(innerErr);
                showToast("이미지 처리에 실패했어요.");
            } finally {
                // 어떤 상황에서도 버튼 상태 복구 및 DOM 정리
                const el = document.getElementById('report-export-view');
                if (el) el.remove();
                btn.innerHTML = originalText;
                btn.disabled = false;
            }
        }, 'image/png');

    } catch (err) {
        console.error(err);
        showToast("리포트 생성 중 오류가 발생했어요.");
        const el = document.getElementById('report-export-view');
        if (el) el.remove();
        btn.innerHTML = originalText;
        btn.disabled = false;
    }
};

function triggerDownload(canvas, username) {
    try {
        const link = document.createElement('a');
        link.download = `GitRanker_${username}.png`;
        link.href = canvas.toDataURL('image/png');
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        showToast('<i class="fas fa-download"></i> 이미지를 저장했어요');
    } catch (e) {
        showToast("다운로드에 실패했어요.");
    }
}

function apply3DEffect(cardElement) {
    if (!cardElement) return;
    if (window.matchMedia("(max-width: 768px)").matches || isReducedMotion) return;

    let bounds;

    cardElement.addEventListener('mouseenter', () => {
        bounds = cardElement.getBoundingClientRect();
    });

    cardElement.addEventListener('mousemove', (e) => {
        if (!bounds) bounds = cardElement.getBoundingClientRect();

        const mouseX = e.clientX;
        const mouseY = e.clientY;
        const leftX = mouseX - bounds.x;
        const topY = mouseY - bounds.y;

        const center = {
            x: leftX - bounds.width / 2,
            y: topY - bounds.height / 2
        };

        const rotateX = ((center.y / bounds.height) * -4).toFixed(2);
        const rotateY = ((center.x / bounds.width) * 4).toFixed(2);

        requestAnimationFrame(() => {
            cardElement.style.transform = `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg)`;
        });
    });

    cardElement.addEventListener('mouseleave', () => {
        requestAnimationFrame(() => {
            cardElement.style.transform = 'perspective(1000px) rotateX(0) rotateY(0)';
        });
        bounds = null;
    });
}

export function showLoading(isLoading) {
    const resultSection = document.getElementById('resultSection');
    const skeletonSection = document.getElementById('skeletonSection');

    if (isLoading) {
        resultSection.classList.add('hidden');
        skeletonSection.classList.remove('hidden');
    } else {
        setTimeout(() => {
            skeletonSection.classList.add('hidden');
        }, 300);
    }
}

export function showResultSection() {
    const resultSection = document.getElementById('resultSection');
    resultSection.classList.remove('hidden');
    apply3DEffect(document.getElementById('profileCard'));
}

function updateStatWithDiff(statId, diffId, totalValue, diffValue) {
    const statEl = document.getElementById(statId);
    const diffEl = document.getElementById(diffId);

    if (statEl) {
        if (isReducedMotion) statEl.innerText = (totalValue || 0).toLocaleString();
        else animateCountUp(statEl, totalValue || 0, 3000);
    }

    if (diffEl) {
        if (diffValue > 0) {
            diffEl.className = 'diff-badge diff-plus';
            diffEl.innerHTML = `<i class="fas fa-caret-up"></i> ${diffValue}`;
            diffEl.style.display = 'inline-flex';
        } else if (diffValue < 0) {
            diffEl.className = 'diff-badge diff-minus';
            diffEl.innerHTML = `<i class="fas fa-caret-down"></i> ${Math.abs(diffValue)}`;
            diffEl.style.display = 'inline-flex';
        } else {
            diffEl.style.display = 'none';
        }
    }
}

export function renderUserResult(data) {
    document.getElementById('resNodeId').value = data.nodeId;
    document.getElementById('resUsername').value = data.username;
    document.getElementById('resProfileImage').src = data.profileImage;
    document.getElementById('resGithubLink').href = `https://github.com/${data.username}`;

    const tierText = document.getElementById('resTierText');
    tierText.innerText = data.tier;
    document.getElementById('resTierBadgeWrapper').className = `tier-badge ${data.tier}`;
    document.getElementById('resTierIcon').className = getTierIconClass(data.tier);

    const profileSection = document.querySelector('.profile-section');
    profileSection.className = 'card-box profile-section';
    profileSection.classList.add(`glow-${data.tier}`);

    document.getElementById('resPercentile').innerText = data.percentile ? data.percentile.toFixed(2) : '0.00';
    document.getElementById('resRanking').innerText = data.ranking;

    const scoreEl = document.getElementById('resTotalScore');
    if (isReducedMotion) scoreEl.innerText = data.totalScore.toLocaleString();
    else animateCountUp(scoreEl, data.totalScore, 3000);

    updateStatWithDiff('statCommit', 'diffCommit', data.commitCount, data.diffCommitCount);
    updateStatWithDiff('statIssue', 'diffIssue', data.issueCount, data.diffIssueCount);
    updateStatWithDiff('statReview', 'diffReview', data.reviewCount, data.diffReviewCount);
    updateStatWithDiff('statPrOpen', 'diffPrOpen', data.prCount, data.diffPrCount);
    updateStatWithDiff('statPrMerged', 'diffPrMerged', data.mergedPrCount, data.diffMergedPrCount);

    document.title = `${data.username} (${data.tier}) | Git Ranker`;

    renderRefreshButton(data.lastFullScanAt);
}

export function createRadarChart(data) {
    const ctx = document.getElementById('statRadarChart');
    if (!ctx) return;

    if (radarChartInstance) {
        radarChartInstance.destroy();
    }

    const rawData = [
        data.commitCount,
        data.mergedPrCount,
        data.prCount,
        data.reviewCount,
        data.issueCount
    ];

    const chartData = rawData.map(v => Math.log10(v + 1));

    const gridColor = getComputedStyle(document.documentElement).getPropertyValue('--chart-grid').trim();
    const textColor = getComputedStyle(document.documentElement).getPropertyValue('--chart-text').trim();

    radarChartInstance = new Chart(ctx, {
        type: 'radar',
        data: {
            labels: ['Commits', 'PR Merged', 'PR Open', 'Reviews', 'Issues'],
            datasets: [{
                label: 'Activity Score',
                data: chartData,
                backgroundColor: 'rgba(49, 130, 246, 0.2)',
                borderColor: 'rgba(49, 130, 246, 1)',
                pointBackgroundColor: 'rgba(49, 130, 246, 1)',
                pointBorderColor: '#fff',
                pointHoverBackgroundColor: '#fff',
                pointHoverBorderColor: 'rgba(49, 130, 246, 1)',
                borderWidth: 2,
                fill: true
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                r: {
                    angleLines: {color: gridColor},
                    grid: {color: gridColor},
                    pointLabels: {
                        font: {family: 'Pretendard', size: 12, weight: '600'},
                        color: textColor
                    },
                    ticks: {display: false, maxTicksLimit: 5},
                    suggestedMin: 0
                }
            },
            plugins: {
                legend: {display: false},
                tooltip: {
                    callbacks: {
                        label: function (context) {
                            const index = context.dataIndex;
                            const realValue = rawData[index];
                            return `${context.label}: ${realValue.toLocaleString()}`;
                        }
                    }
                }
            },
            animation: {
                duration: isReducedMotion ? 0 : 2000,
                easing: 'easeInOutQuart',
                loop: false
            }
        }
    });
}

export function renderRefreshButton(lastFullScanAt) {
    const btn = document.getElementById('btnRefresh');
    const statusText = document.getElementById('refreshStatusText');
    const isAvailable = isRefreshAvailable(lastFullScanAt);
    const newBtn = btn.cloneNode(true);
    btn.parentNode.replaceChild(newBtn, btn);

    if (isAvailable) {
        newBtn.style.opacity = "1";
        newBtn.style.cursor = "pointer";
        newBtn.disabled = false;
        newBtn.onclick = () => document.dispatchEvent(new CustomEvent('requestRefreshUser'));
        // [수정] Toss 스타일의 심플한 상태 표시 (번개 이모지 제거, 파란 점 사용)
        statusText.innerHTML = `<span style="color:var(--toss-blue); font-weight:700; font-size:13px;"><i class="fas fa-circle" style="font-size:8px; vertical-align:middle; margin-right:4px;"></i>업데이트 가능</span>`;
    } else {
        const nextTime = calculateNextRefreshTime(lastFullScanAt);
        newBtn.style.opacity = "0.5";
        newBtn.style.cursor = "not-allowed";
        newBtn.disabled = true;
        statusText.innerHTML = `<span style="color:var(--text-tertiary);">${formatDateTime(nextTime)} 이후 가능</span>`;
    }
}

export function renderRankingTable(users) {
    const listContainer = document.getElementById('rankingList');
    listContainer.innerHTML = '';

    if (!users || users.length === 0) {
        listContainer.innerHTML = `<div class="text-center py-5 text-secondary">랭킹 데이터가 없습니다.</div>`;
        return;
    }

    users.forEach((user, index) => {
        const row = document.createElement('div');
        row.className = 'ranking-row';

        let tierColor = '#6B7684';
        if (user.tier === 'CHALLENGER') tierColor = '#3182F6';
        else if (user.tier === 'MASTER') tierColor = '#9B59B6';
        else if (user.tier === 'DIAMOND') tierColor = '#00B4FC';
        else if (user.tier === 'PLATINUM') tierColor = '#00C7BE';
        else if (user.tier === 'EMERALD') tierColor = '#2ECC71';
        else if (user.tier === 'GOLD') tierColor = '#FFD700';

        row.innerHTML = `
            <div class="col-rank font-code">${user.ranking}</div>
            <div class="col-user">
                <img src="${user.profileImage}" class="user-avatar">
                <div style="min-width:0;">
                    <div class="user-name">
                        <span class="mobile-tier-dot" style="background:${tierColor};"></span>
                        ${user.username}
                    </div>
                </div>
            </div>
            <div class="col-tier">
                <span class="tier-badge ${user.tier}" style="margin:0; padding:4px 10px; font-size:11px;">${user.tier}</span>
            </div>
            <div class="col-score font-code">${formatNumber(user.totalScore)}</div>
        `;
        row.onclick = () => document.dispatchEvent(new CustomEvent('requestUserDetail', {detail: user.username}));
        listContainer.appendChild(row);
    });
}

export function renderPagination(pageInfo, loadRankingsCallback) {
    const pagination = document.getElementById('pagination');
    pagination.innerHTML = '';
    const {currentPage, totalPages, isFirst, isLast} = pageInfo;
    const pageSize = 5;
    const startPage = Math.floor(currentPage / pageSize) * pageSize;
    const endPage = Math.min(startPage + pageSize, totalPages);

    const createItem = (text, page, disabled = false, active = false) => {
        const li = document.createElement('li');
        const btn = document.createElement('button');
        btn.innerHTML = text;
        btn.style.cssText = `border:none; background:${active ? 'var(--toss-blue)' : 'transparent'}; color:${active ? 'white' : 'var(--text-secondary)'}; width:32px; height:32px; border-radius:10px; font-weight:600; cursor:pointer; transition:all 0.2s;`;
        if (!disabled) {
            btn.onclick = (e) => {
                e.preventDefault();
                loadRankingsCallback(page);
            };
        } else {
            btn.style.opacity = '0.3';
            btn.style.cursor = 'default';
        }
        li.appendChild(btn);
        return li;
    };
    pagination.appendChild(createItem('<i class="fas fa-chevron-left"></i>', currentPage - 1, isFirst));
    for (let i = startPage; i < endPage; i++) {
        pagination.appendChild(createItem(i + 1, i, false, i === currentPage));
    }
    pagination.appendChild(createItem('<i class="fas fa-chevron-right"></i>', currentPage + 1, isLast));
}

export function renderUserDetailModal(data, modalInstance) {
    document.getElementById('modalProfileImage').src = data.profileImage;
    document.getElementById('modalUsername').innerText = data.username;
    const tierEl = document.getElementById('modalTierBadge');
    tierEl.className = `tier-badge ${data.tier} mb-3`;
    tierEl.innerHTML = `<i class="${getTierIconClass(data.tier)} me-1"></i> ${data.tier}`;
    document.getElementById('modalRanking').innerText = data.ranking;
    document.getElementById('modalPercentile').innerText = data.percentile.toFixed(2);
    document.getElementById('modalTotalScore').innerText = formatNumber(data.totalScore);
    document.getElementById('modalGithubLink').href = `https://github.com/${data.username}`;

    updateStatWithDiff('modalStatCommit', 'modalDiffCommit', data.commitCount, data.diffCommitCount);
    updateStatWithDiff('modalStatIssue', 'modalDiffIssue', data.issueCount, data.diffIssueCount);
    updateStatWithDiff('modalStatReview', 'modalDiffReview', data.reviewCount, data.diffReviewCount);
    updateStatWithDiff('modalStatPrOpen', 'modalDiffPrOpen', data.prCount, data.diffPrCount);
    updateStatWithDiff('modalStatPrMerged', 'modalDiffPrMerged', data.mergedPrCount, data.diffMergedPrCount);
    modalInstance.show();
}