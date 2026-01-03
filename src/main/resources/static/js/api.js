const API_BASE_URL = '/api/v1';

export async function fetchRankings(page) {
    const response = await fetch(`${API_BASE_URL}/ranking?page=${page}`);
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