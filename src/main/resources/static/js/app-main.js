/* app-main.js — GraphDB 管理页入口：鉴权通过后注入 partials 再初始化 */
document.addEventListener('DOMContentLoaded', function() {
    ensureAuthenticated().then(ok => {
        if (!ok) return;
        loadPagePartials()
            .then(() => {
                bindCreateRepoDbInputListeners();
                bindCreateRepoPageDbInputListeners();
                initNavigation();
                checkGraphDbStatus();
                loadDashboardData();
                loadRepositories();
                checkUrlParams();
            })
            .catch(err => {
                console.error(err);
                toast('error', '页面片段加载失败: ' + err.message);
            });
    });
});
