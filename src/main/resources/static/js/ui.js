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

/**
 * Toss Style Toast Message (Fix: Vertical Stack & Auto Remove)
 */
export function showToast(message, isError = false) {
    const container = document.getElementById('toast-container');

    const toast = document.createElement('div');
    toast.className = 'toss-toast';

    let iconHtml = '';
    if (message.includes('<i class=')) {
        toast.innerHTML = message;
    } else {
        if (isError) {
            iconHtml = `<i class="fas fa-times-circle" style="color:#EF4444; font-size: 20px;"></i>`;
        } else {
            iconHtml = `<i class="fas fa-check-circle" style="color:#4ADE80; font-size: 20px;"></i>`;
        }
        toast.innerHTML = `${iconHtml}<span>${message}</span>`;
    }

    container.appendChild(toast);

    // 등장 애니메이션
    requestAnimationFrame(() => {
        toast.classList.add('show');
    });

    // 3초 후 사라짐 (CSS transition 시간 0.4초 고려)
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => {
            if (toast.parentElement) {
                toast.remove();
            }
        }, 400); // CSS transition duration과 일치
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

function loadScript(src) {
    return new Promise((resolve, reject) => {
        if (document.querySelector(`script[src="${src}"]`)) {
            resolve();
            return;
        }
        const script = document.createElement('script');
        script.src = src;
        script.onload = resolve;
        script.onerror = reject;
        document.head.appendChild(script);
    });
}

function canvasToBlob(canvas) {
    return new Promise((resolve, reject) => {
        try {
            canvas.toBlob((blob) => {
                if (blob) resolve(blob);
                else reject(new Error("Blob creation failed"));
            }, 'image/png');
        } catch (e) {
            reject(e);
        }
    });
}

async function loadImageAsBase64(url) {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 3000);

    try {
        const response = await fetch(url, {signal: controller.signal});
        const blob = await response.blob();
        return new Promise((resolve) => {
            const reader = new FileReader();
            reader.onloadend = () => resolve(reader.result);
            reader.readAsDataURL(blob);
        });
    } catch (e) {
        console.warn("Image load failed or timed out, using original url", e);
        return url;
    } finally {
        clearTimeout(timeoutId);
    }
}

function showSharePreviewModal(file, filename) {
    const existingModal = document.getElementById('sharePreviewModal');
    if (existingModal) existingModal.remove();

    const imageUrl = URL.createObjectURL(file);

    const modalHtml = `
        <div id="sharePreviewModal" class="custom-modal">
            <div class="custom-modal-backdrop"></div>
            <div class="custom-modal-content" style="max-width: 360px; width: 90%;">
                <h3 class="custom-modal-title" style="margin-bottom:12px;">리포트가 준비되었어요</h3>
                <p class="custom-modal-desc" style="margin-bottom:20px;">친구들에게 내 개발 전투력을 자랑해보세요!</p>
                <div style="margin: 0 auto 24px; background: #ffffff; border-radius: 12px; overflow: hidden; border: 1px solid #e5e8eb; box-shadow: 0 4px 12px rgba(0,0,0,0.08);">
                    <img src="${imageUrl}" style="width: 100%; height: auto; display: block;" alt="Report Preview">
                </div>
                <div class="custom-modal-actions">
                    <button id="btnShareCancel" class="btn-modal-secondary">닫기</button>
                    <button id="btnShareAction" class="btn-modal-primary"><i class="fas fa-share-alt"></i> 공유하기</button>
                </div>
            </div>
        </div>
    `;

    document.body.insertAdjacentHTML('beforeend', modalHtml);

    const modal = document.getElementById('sharePreviewModal');
    const btnCancel = document.getElementById('btnShareCancel');
    const btnShare = document.getElementById('btnShareAction');

    requestAnimationFrame(() => {
        const backdrop = modal.querySelector('.custom-modal-backdrop');
        const content = modal.querySelector('.custom-modal-content');
        if (backdrop) backdrop.style.opacity = '1';
        if (content) content.style.transform = 'scale(1)';
    });

    const closeModal = () => {
        modal.remove();
        URL.revokeObjectURL(imageUrl);
    };

    btnCancel.onclick = closeModal;

    btnShare.onclick = async () => {
        btnShare.disabled = true;

        try {
            if (navigator.canShare && navigator.canShare({files: [file]})) {
                const isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);

                let shareData = {files: [file]};

                if (isMobile) {
                    shareData.title = 'Git Ranker Report';
                    shareData.text = '나의 개발자 전투력을 확인해보세요! \n https://git-ranker.com';
                }

                await navigator.share(shareData);

                showToast('공유에 성공했어요');
                closeModal();
            } else {
                throw new Error('Share API not supported');
            }
        } catch (err) {
            if (err.name !== 'AbortError') {
                showToast('공유할 수 없어 이미지를 저장합니다.');
                const link = document.createElement('a');
                link.download = filename;
                link.href = imageUrl;
                link.click();
                closeModal();
            }
        } finally {
            btnShare.disabled = false;
        }
    };
}

window.captureAndDownload = async () => {
    const btn = document.querySelector('.btn-black');
    const originalText = btn.innerHTML;

    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 생성 중...';
    btn.disabled = true;

    const isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
    const timeoutDuration = isMobile ? 15000 : 20000;

    const safetyTimer = setTimeout(() => {
        if (btn.disabled) {
            btn.innerHTML = originalText;
            btn.disabled = false;
            showToast('생성 시간이 초과되었어요. 다시 시도해주세요.', true);
            const el = document.getElementById('report-export-view');
            if (el) el.remove();
        }
    }, timeoutDuration);

    try {
        if (typeof html2canvas === 'undefined') {
            await loadScript('https://html2canvas.hertzen.com/dist/html2canvas.min.js');
        }

        const username = document.getElementById('resUsername').value;
        const originalProfileSrc = document.getElementById('resProfileImage').src;
        const chartBase64 = radarChartInstance.toBase64Image();

        let profileSrc;
        if (isMobile) {
            profileSrc = originalProfileSrc;
        } else {
            profileSrc = await loadImageAsBase64(originalProfileSrc);
        }

        const stats = [
            {label: 'Commits', value: document.getElementById('statCommit').innerText},
            {label: 'PR Merged', value: document.getElementById('statPrMerged').innerText},
            {label: 'PR Open', value: document.getElementById('statPrOpen').innerText},
            {label: 'Reviews', value: document.getElementById('statReview').innerText},
            {label: 'Issues', value: document.getElementById('statIssue').innerText}
        ];

        const reportContainer = document.createElement('div');
        reportContainer.id = 'report-export-view';
        reportContainer.style.position = 'fixed';
        reportContainer.style.left = '-9999px';
        reportContainer.style.top = '0';
        document.body.appendChild(reportContainer);

        const today = new Date();
        const localDate = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;

        reportContainer.innerHTML = `
            <div class="simple-report-card">
                <div class="sim-header">
                    <div class="sim-brand"><i class="fab fa-github"></i> Git Ranker</div>
                    <div class="sim-date">${localDate}</div>
                </div>
                <div class="sim-body">
                    <div class="sim-profile">
                        <img src="${profileSrc}" class="sim-avatar" crossorigin="anonymous" alt="profile">
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

        const scale = isMobile ? 1.5 : 2;

        const canvas = await html2canvas(reportContainer, {
            backgroundColor: '#ffffff',
            scale: scale,
            useCORS: true,
            allowTaint: false,
            logging: false,
            imageTimeout: 5000
        });

        const blob = await canvasToBlob(canvas);
        const filename = `GitRanker_${username}.png`;
        const file = new File([blob], filename, {type: 'image/png'});

        showSharePreviewModal(file, filename);

    } catch (err) {
        console.error(err);
        showToast("이미지를 만들지 못했어요. 잠시 후 다시 시도해주세요.", true);
    } finally {
        clearTimeout(safetyTimer);
        const el = document.getElementById('report-export-view');
        if (el) el.remove();

        btn.innerHTML = originalText;
        btn.disabled = false;
    }
};

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
        else animateCountUp(statEl, totalValue || 0, 2000);
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
    else animateCountUp(scoreEl, data.totalScore, 2000);

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

    const fragment = document.createDocumentFragment();

    users.forEach((user, index) => {
        const row = document.createElement('div');
        row.className = 'ranking-row stagger-item';
        row.style.animationDelay = `${index * 0.03}s`;

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
                <img src="${user.profileImage}" class="user-avatar" loading="lazy" alt="${user.username}">
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

        fragment.appendChild(row);
    });

    listContainer.appendChild(fragment);
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