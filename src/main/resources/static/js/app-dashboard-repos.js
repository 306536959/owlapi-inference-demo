/* app-dashboard-repos.js — GraphDB 管理页脚本片段 */
function checkGraphDbStatus() {
    fetch('/api/graphdb/status')
        .then(r => r.json())
        .then(data => {
            const badge = document.getElementById('graphDbStatus');
            if (data.success && data.connected) {
                badge.className = 'status-badge online';
                badge.innerHTML = '<span class="status-dot"></span><span>GraphDB 已连接</span>';
            } else {
                badge.className = 'status-badge offline';
                badge.innerHTML = '<span class="status-dot"></span><span>GraphDB 未连接</span>';
            }
        })
        .catch(() => {
            document.getElementById('graphDbStatus').className = 'status-badge offline';
            document.getElementById('graphDbStatus').innerHTML = '<span class="status-dot"></span><span>GraphDB 未连接</span>';
        });
}

// Dashboard
function loadDashboardData() {
    loadRepositories();
    
    // Update uptime
    setInterval(() => {
        const elapsed = Date.now() - startTime;
        const hours = Math.floor(elapsed / 3600000);
        const mins = Math.floor((elapsed % 3600000) / 60000);
        document.getElementById('stat-uptime').textContent = hours + 'h ' + mins + 'm';
    }, 1000);
}

// Repositories
function loadRepositories() {
    fetch('/api/graphdb/repositories')
        .then(r => r.json())
        .then(data => {
            currentRepos = Array.isArray(data) ? data : [];
            renderRepositoriesTable(currentRepos);
            renderDashboardRepos(currentRepos);
            updateRepoSelects(currentRepos);
            document.getElementById('stat-repos').textContent = currentRepos.length;
            
            // Calculate total triples
            let totalTriples = 0;
            currentRepos.forEach(repo => {
                if (repo.id) {
                    fetchRepoSize(repo.id);
                }
            });
        })
        .catch(err => {
            console.error('Error loading repos:', err);
            document.getElementById('repositoriesTableBody').innerHTML = 
                '<tr><td colspan="5" class="text-danger text-center">加载失败</td></tr>';
        });
}

function fetchRepoSize(repoId) {
    fetch('/api/graphdb/repositories/' + repoId + '/size')
        .then(r => r.json())
        .then(data => {
            const size = data.totalTriples || data.size || 0;
            const current = parseInt(document.getElementById('stat-triples').textContent) || 0;
            document.getElementById('stat-triples').textContent = current + size;
        })
        .catch(() => {});
}

function renderRepositoriesTable(repos) {
    const tbody = document.getElementById('repositoriesTableBody');
    if (!repos.length) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted py-4">暂无仓库</td></tr>';
        return;
    }
    tbody.innerHTML = repos.map(repo => `
        <tr>
            <td><code>${repo.id || '-'}</code></td>
            <td>${repo.title || '-'}</td>
            <td><span class="badge bg-secondary">${repo.type || '-'}</span></td>
            <td><span class="badge ${repo.readable ? 'bg-success' : 'bg-warning'}">${repo.readable ? '在线' : '离线'}</span></td>
            <td>
                <button class="btn btn-sm btn-outline-primary me-1" onclick="viewRepo('${repo.id}')">
                    <i class="fas fa-eye"></i>
                </button>
                <button class="btn btn-sm btn-outline-warning me-1" onclick="restartRepo('${repo.id}')">
                    <i class="fas fa-redo"></i>
                </button>
                <button class="btn btn-sm btn-outline-danger" onclick="deleteRepo('${repo.id}')">
                    <i class="fas fa-trash"></i>
                </button>
            </td>
        </tr>
    `).join('');
}

function renderDashboardRepos(repos) {
    const container = document.getElementById('dashboardRepoList');
    if (!repos.length) {
        container.innerHTML = '<div class="text-center text-muted py-3">暂无仓库</div>';
        return;
    }
    container.innerHTML = repos.slice(0, 5).map(repo => `
        <div class="d-flex justify-content-between align-items-center py-2 border-bottom">
            <div>
                <strong>${repo.title || repo.id}</strong>
                <small class="text-muted d-block">${repo.id}</small>
            </div>
            <span class="badge ${repo.readable ? 'bg-success' : 'bg-secondary'}">
                ${repo.readable ? '在线' : '离线'}
            </span>
        </div>
    `).join('');
}

function updateRepoSelects(repos) {
    const selects = ['queryRepoSelect'];
    selects.forEach(id => {
        const select = document.getElementById(id);
        if (select) {
            const currentVal = select.value;
            select.innerHTML = '<option value="">-- 选择仓库 --</option>';
            repos.forEach(repo => {
                select.innerHTML += `<option value="${repo.id}">${repo.title || repo.id}</option>`;
            });
            select.value = currentVal;
        }
    });
}
function refreshAll() {
    checkGraphDbStatus();
    loadRepositories();
    toast('success', '刷新完成');
}
