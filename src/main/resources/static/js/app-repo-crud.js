/* app-repo-crud.js — GraphDB 管理页脚本片段 */
function restartRepo(repoId) {
    if (!confirm('确定重启仓库 ' + repoId + '?')) return;
    showLoading();
    fetch('/api/graphdb/repositories/' + repoId + '/restart', { method: 'POST' })
        .then(r => r.json())
        .then(data => {
            hideLoading();
            toast(data.success ? 'success' : 'error', data.message || data.error || (data.success ? '重启成功' : '重启失败'));
            loadRepositories();
        })
        .catch(err => {
            hideLoading();
            toast('error', '重启失败: ' + err.message);
        });
}

function deleteRepo(repoId) {
    if (!confirm('确定删除仓库 ' + repoId + '? 此操作不可恢复!')) return;
    showLoading();
    fetch('/api/graphdb/repositories/' + repoId, { method: 'DELETE' })
        .then(r => r.json())
        .then(data => {
            hideLoading();
            toast(data.success ? 'success' : 'error', data.message || data.error || (data.success ? '删除成功' : '删除失败'));
            loadRepositories();
        })
        .catch(err => {
            hideLoading();
            toast('error', '删除失败: ' + err.message);
        });
}

function viewRepo(repoId) {
    // Find repo info
    const repo = currentRepos.find(r => r.id === repoId);
    if (!repo) {
        toast('error', '仓库未找到');
        return;
    }
    
    // Fill basic info
    document.getElementById('repoDetailTitle').textContent = repo.title || repo.id;
    document.getElementById('repoDetailId').textContent = repo.id;
    document.getElementById('repoDetailStatus').textContent = repo.readable ? '在线' : '离线';
    document.getElementById('repoDetailStatus').className = 'badge ' + (repo.readable ? 'bg-success' : 'bg-warning');
    
    // Fill info tab
    document.getElementById('infoRepoId').textContent = repo.id || '-';
    document.getElementById('infoRepoTitle').textContent = repo.title || '-';
    document.getElementById('infoRepoType').textContent = repo.type || '-';
    document.getElementById('infoRepoReadable').innerHTML = repo.readable 
        ? '<span class="badge bg-success">在线</span>' 
        : '<span class="badge bg-warning">离线</span>';
    
    // Store current repo id for query button
    window.currentRepoDetailId = repoId;
    
    // Load repo files
    loadRepoFiles(repoId);
    
    // Reset preview
    document.getElementById('repoFilePreview').innerHTML = '<pre class="mb-0 text-light">选择左侧文件查看内容...</pre>';
    
    // Show modal
    new bootstrap.Modal(document.getElementById('repoDetailModal')).show();
}

function loadRepoFiles(repoId) {
    const placeholders = ['owl', 'obda', 'properties'];
    placeholders.forEach(type => {
        document.getElementById('repo' + type.charAt(0).toUpperCase() + type.slice(1) + 'File')
            .innerHTML = '<p class="text-muted text-center">加载中...</p>';
    });
    
    fetch('/api/graphdb/repositories/' + repoId + '/files')
        .then(r => r.json())
        .then(data => {
            if (!data.success) {
                placeholders.forEach(type => {
                    document.getElementById('repo' + type.charAt(0).toUpperCase() + type.slice(1) + 'File')
                        .innerHTML = '<p class="text-danger text-center">加载失败</p>';
                });
                return;
            }
            
            const files = data.files;
            
            // OWL file
            if (files.owl) {
                document.getElementById('repoOwlFile').innerHTML = `
                    <div class="d-flex justify-content-between align-items-center">
                        <div>
                            <small class="text-muted">${formatSize(files.owl.size)}</small>
                        </div>
                        <button class="btn btn-sm btn-outline-primary" onclick="viewRepoFile('${repoId}', '${files.owl.name}')">
                            <i class="fas fa-eye"></i> 查看
                        </button>
                    </div>
                `;
            } else {
                document.getElementById('repoOwlFile').innerHTML = '<p class="text-muted text-center">无文件</p>';
            }
            
            // OBDA file
            if (files.obda) {
                document.getElementById('repoObdaFile').innerHTML = `
                    <div class="d-flex justify-content-between align-items-center">
                        <div>
                            <small class="text-muted">${formatSize(files.obda.size)}</small>
                        </div>
                        <button class="btn btn-sm btn-outline-success" onclick="viewRepoFile('${repoId}', '${files.obda.name}')">
                            <i class="fas fa-eye"></i> 查看
                        </button>
                    </div>
                `;
            } else {
                document.getElementById('repoObdaFile').innerHTML = '<p class="text-muted text-center">无文件</p>';
            }
            
            // Properties file
            if (files.properties) {
                document.getElementById('repoPropertiesFile').innerHTML = `
                    <div class="d-flex justify-content-between align-items-center">
                        <div>
                            <small class="text-muted">${formatSize(files.properties.size)}</small>
                        </div>
                        <button class="btn btn-sm btn-outline-info" onclick="viewRepoFile('${repoId}', '${files.properties.name}')">
                            <i class="fas fa-eye"></i> 查看
                        </button>
                    </div>
                `;
            } else {
                document.getElementById('repoPropertiesFile').innerHTML = '<p class="text-muted text-center">无文件</p>';
            }
        })
        .catch(err => {
            placeholders.forEach(type => {
                document.getElementById('repo' + type.charAt(0).toUpperCase() + type.slice(1) + 'File')
                    .innerHTML = '<p class="text-danger text-center">加载失败</p>';
            });
        });
}

function viewRepoFile(repoId, fileName) {
    const preview = document.getElementById('repoFilePreview');
    preview.innerHTML = '<pre class="mb-0 text-light"><i class="fas fa-spinner fa-spin"></i> 加载中...</pre>';
    
    fetch('/api/graphdb/repositories/' + repoId + '/files/' + encodeURIComponent(fileName) + '/content')
        .then(r => r.json())
        .then(data => {
            if (data.success) {
                preview.innerHTML = '<pre class="mb-0 text-light">' + escapeHtml(data.content) + '</pre>';
            } else {
                preview.innerHTML = '<pre class="text-danger mb-0">' + escapeHtml(data.message) + '</pre>';
            }
        })
        .catch(err => {
            preview.innerHTML = '<pre class="text-danger mb-0">加载失败: ' + escapeHtml(err.message) + '</pre>';
        });
}

function openRepoInQuery() {
    if (window.currentRepoDetailId) {
        const modal = bootstrap.Modal.getInstance(document.getElementById('repoDetailModal'));
        modal.hide();
        switchSection('query');
        document.getElementById('queryRepoSelect').value = window.currentRepoDetailId;
    }
}
