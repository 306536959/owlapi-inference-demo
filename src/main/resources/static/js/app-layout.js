/* app-layout.js — 按需加载 HTML 片段注入页面（与 index 壳子配合） */

async function fetchPartial(path) {
    const res = await fetch(path, { cache: 'no-store' });
    if (!res.ok) {
        throw new Error(path + ' HTTP ' + res.status);
    }
    return (await res.text()).trim();
}

function injectHtml(containerId, html) {
    const el = document.getElementById(containerId);
    if (!el) {
        throw new Error('Missing container #' + containerId);
    }
    el.innerHTML = html;
}

async function loadPagePartials() {
    const base = 'partials/';
    const sidebar = await fetchPartial(base + 'sidebar.html');
    const topbar = await fetchPartial(base + 'topbar.html');
    injectHtml('layout-sidebar', sidebar);
    injectHtml('layout-topbar', topbar);

    const sectionFiles = [
        'section-dashboard.html',
        'section-repositories.html',
        'section-create-fedx.html',
        'section-query.html'
    ];
    const sectionsHtml = await Promise.all(sectionFiles.map(f => fetchPartial(base + f)));
    injectHtml('layout-sections', sectionsHtml.join('\n'));

    const modalFiles = ['modal-create-repo.html', 'modal-repo-detail.html'];
    const modalsHtml = await Promise.all(modalFiles.map(f => fetchPartial(base + f)));
    injectHtml('layout-modals', modalsHtml.join('\n'));
}
