/* app-utils.js — GraphDB 管理页脚本片段 */
function showLoading() {
    document.getElementById('loadingOverlay').style.display = 'flex';
}

function hideLoading() {
    document.getElementById('loadingOverlay').style.display = 'none';
}

function toast(type, message) {
    const container = document.getElementById('toastContainer');
    const id = 'toast-' + Date.now();
    const bg = type === 'success' ? 'bg-success' : type === 'error' ? 'bg-danger' : 'bg-info';
    container.innerHTML += `
        <div id="${id}" class="toast ${bg} text-white" role="alert" style="min-width: 250px;">
            <div class="toast-body d-flex justify-content-between align-items-center">
                <span>${message}</span>
                <button type="button" class="btn-close btn-close-white ms-2" onclick="document.getElementById('${id}').remove()"></button>
            </div>
        </div>
    `;
    const toastEl = document.getElementById(id);
    new bootstrap.Toast(toastEl).show();
    setTimeout(() => toastEl.remove(), 5000);
}

function refreshAll() {
    checkGraphDbStatus();
    loadRepositories();
    toast('success', '刷新完成');
}

function formatSize(bytes) {
    if (!bytes) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function formatDate(timestamp) {
    return new Date(timestamp).toLocaleString();
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
