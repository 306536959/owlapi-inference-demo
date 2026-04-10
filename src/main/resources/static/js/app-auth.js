/* app-auth.js — GraphDB 管理页脚本片段 */
function ensureAuthenticated() {
    return fetch('/api/auth/status')
        .then(r => r.json())
        .then(data => {
            if (data && data.authenticated) {
                return true;
            }
            window.location.href = 'login.html';
            return false;
        })
        .catch(() => {
            window.location.href = 'login.html';
            return false;
        });
}

function logout() {
    fetch('/api/auth/logout', { method: 'POST' })
        .finally(() => {
            window.location.href = 'login.html';
        });
}

// Handle URL parameters for shared query links
function checkUrlParams() {
    const urlParams = new URLSearchParams(window.location.search);
    const queryName = urlParams.get('query');
    const repoId = urlParams.get('repo');
    
    if (queryName) {
        // Switch to query section
        switchSection('query');
        
        // Load the saved query
        fetch('/api/graphdb/saved-queries')
            .then(r => r.json())
            .then(queries => {
                const query = (queries || []).find(q => 
                    (q.name || q.queryName) === queryName
                );
                
                if (query) {
                    const body = query.body || query.queryBody || '';
                    document.getElementById('sparqlQuery').value = body;
                    
                    // If repo is specified, select it and optionally auto-execute
                    if (repoId) {
                        document.getElementById('queryRepoSelect').value = repoId;
                        // Auto-execute after a short delay to ensure repo is loaded
                        setTimeout(() => {
                            if (document.getElementById('queryRepoSelect').value === repoId) {
                                executeQuery();
                            }
                        }, 500);
                    } else {
                        toast('info', '已加载查询 "' + queryName + '"，请选择仓库后执行');
                    }
                } else {
                    toast('error', '未找到查询: ' + queryName);
                }
                
                // Clean up URL
                const newUrl = window.location.pathname;
                window.history.replaceState({}, document.title, newUrl);
            })
            .catch(err => {
                toast('error', '加载查询失败: ' + err.message);
            });
    }
}
