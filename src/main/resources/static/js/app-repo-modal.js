/* app-repo-modal.js — GraphDB 管理页脚本片段 */
function showCreateRepoModal() {
    createRepoGeneratedFiles = null;
    const resultDiv = document.getElementById('newRepoGenerateResult');
    resultDiv.style.display = 'none';
    resultDiv.innerHTML = '';
    const connDiv = document.getElementById('newRepoConnTestResult');
    connDiv.style.display = 'none';
    connDiv.innerHTML = '';
    updateGenerateButtonState(false);
    new bootstrap.Modal(document.getElementById('createRepoModal')).show();
}

function updateGenerateButtonState(enabled) {
    const btn = document.getElementById('btnGenerateOntopFiles');
    btn.disabled = !enabled;
    btn.classList.toggle('btn-outline-primary', !enabled);
    btn.classList.toggle('btn-primary', enabled);
}

function updateGenerateButtonStatePage(enabled) {
    const btn = document.getElementById('btnGenerateOntopFilesPage');
    if (!btn) return;
    btn.disabled = !enabled;
    btn.classList.toggle('btn-outline-primary', !enabled);
    btn.classList.toggle('btn-primary', enabled);
}
