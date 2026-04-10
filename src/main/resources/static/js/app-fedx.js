/* app-fedx.js — GraphDB 管理页脚本片段 */
function refreshFedxMembers() {
    const container = document.getElementById('fedxMemberRepos');
    if (!container) return;
    const repos = (currentRepos || []).filter(r => {
        const type = (r.type || '').toLowerCase();
        const isFedx = type.includes('fedx');
        return !isFedx;
    });
    if (!repos.length) {
        container.innerHTML = '<div class="text-muted">暂无可选成员仓库</div>';
        return;
    }
    container.innerHTML = repos.map(repo => {
        const readable = repo.readable ? '在线' : '离线';
        const badgeClass = repo.readable ? 'bg-success' : 'bg-warning text-dark';
        const label = (repo.title || repo.id);
        return `
            <label class="d-flex align-items-center justify-content-between border rounded px-2 py-2 mb-2">
                <span class="d-flex align-items-center">
                    <input class="form-check-input me-2 fedx-member-checkbox" type="checkbox" value="${repo.id}">
                    <span>${label} <small class="text-muted">(${repo.id})</small></span>
                </span>
                <span class="badge ${badgeClass}">${readable}</span>
            </label>
        `;
    }).join('');
}

function getSelectedFedxMembers() {
    const container = document.getElementById('fedxMemberRepos');
    if (!container) return [];
    return Array.from(container.querySelectorAll('.fedx-member-checkbox:checked'))
        .map(el => el.value)
        .filter(Boolean);
}

function selectAllFedxMembers() {
    const container = document.getElementById('fedxMemberRepos');
    if (!container) return;
    container.querySelectorAll('.fedx-member-checkbox').forEach(el => {
        el.checked = true;
    });
}

function clearFedxMembers() {
    const container = document.getElementById('fedxMemberRepos');
    if (!container) return;
    container.querySelectorAll('.fedx-member-checkbox').forEach(el => {
        el.checked = false;
    });
}

function createFedxRepositoryFromPage() {
    const id = (document.getElementById('newFedxRepoId').value || '').trim();
    const title = (document.getElementById('newFedxRepoTitle').value || '').trim();
    const members = getSelectedFedxMembers();

    if (!id) {
        toast('error', '请填写 FedX 仓库ID');
        return;
    }
    if (!title) {
        toast('error', '请填写 FedX 仓库标题');
        return;
    }
    if (!members.length) {
        toast('error', '请至少选择一个联邦成员仓库');
        return;
    }

    const formData = new FormData();
    formData.append('id', id);
    formData.append('title', title);
    formData.append('members', members.join(','));

    showLoading();
    fetch('/api/graphdb/repositories/fedx', {
        method: 'POST',
        body: formData
    })
    .then(r => r.json())
    .then(data => {
        hideLoading();
        if (data.success) {
            toast('success', 'FedX 仓库创建成功');
            loadRepositories();
            switchSection('repositories');
        } else {
            toast('error', data.error || data.message || '创建失败');
        }
    })
    .catch(err => {
        hideLoading();
        toast('error', '创建失败: ' + err.message);
    });
}
