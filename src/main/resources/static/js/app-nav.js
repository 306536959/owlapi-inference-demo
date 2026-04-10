/* app-nav.js — GraphDB 管理页脚本片段 */
function initNavigation() {
    document.querySelectorAll('.nav-item').forEach(item => {
        item.addEventListener('click', function() {
            const section = this.getAttribute('data-section');
            if (!section) return;
            switchSection(section);
        });
    });
}

function switchSection(section) {
    document.querySelectorAll('.nav-item').forEach(item => {
        item.classList.toggle('active', item.getAttribute('data-section') === section);
    });
    document.querySelectorAll('.section').forEach(sec => {
        sec.classList.toggle('active', sec.id === 'section-' + section);
    });
    const titles = {
        'dashboard': '仪表盘',
        'repositories': '仓库管理',
        'create-fedx': '创建FedX仓库',
        'query': 'SPARQL查询'
    };
    document.getElementById('pageTitle').textContent = titles[section] || '仪表盘';

    if (section === 'create-fedx') {
        refreshFedxMembers();
    }
}
