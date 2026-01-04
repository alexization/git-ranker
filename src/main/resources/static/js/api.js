const API_BASE_URL = '/api/v1';

export async function fetchRankings(page, tier) {
    let url = `${API_BASE_URL}/ranking?page=${page}`;
    // 티어가 'ALL'이 아니거나 값이 있을 때만 파라미터 추가
    if (tier && tier !== 'ALL') {
        url += `&tier=${tier}`;
    }
    const response = await fetch(url);
    return response.json();
}

export async function registerUser(username) {
    const response = await fetch(`${API_BASE_URL}/users`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({username: username})
    });
    return response.json();
}

export async function getUserDetail(username) {
    const response = await fetch(`${API_BASE_URL}/users/${username}`);
    return response.json();
}

export async function refreshUser(username) {
    const response = await fetch(`${API_BASE_URL}/users/${username}/refresh`, {
        method: 'POST'
    });
    return response.json();
}