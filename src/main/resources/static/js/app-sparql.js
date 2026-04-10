/* app-sparql.js — GraphDB 管理页脚本片段 */
function executeQuery() {
    const repoId = document.getElementById('queryRepoSelect').value;
    const query = document.getElementById('sparqlQuery').value;
    const queryType = document.getElementById('queryType').value;
    
    if (!repoId) {
        toast('error', '请选择仓库');
        return;
    }
    if (!query.trim()) {
        toast('error', '请输入查询语句');
        return;
    }
    
    const resultsDiv = document.getElementById('queryResults');
    resultsDiv.innerHTML = '<pre class="text-muted">执行中...</pre>';
    
    let endpoint = '/api/graphdb/repositories/' + repoId + '/sparql';
    if (queryType === 'construct') endpoint += '/construct';
    if (queryType === 'ask') endpoint += '/ask';
    
    showLoading();
    fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ query: query })
    })
    .then(r => r.json())
    .then(data => {
        hideLoading();
        if (data.error) {
            resultsDiv.innerHTML = '<pre class="text-danger">' + data.error + '</pre>';
        } else if (queryType === 'select') {
            displaySelectResults(data, resultsDiv);
        } else {
            resultsDiv.innerHTML = '<pre>' + JSON.stringify(data, null, 2) + '</pre>';
        }
    })
    .catch(err => {
        hideLoading();
        resultsDiv.innerHTML = '<pre class="text-danger">查询失败: ' + err.message + '</pre>';
    });
}

function displaySelectResults(data, container) {
    if (!data.results || !data.results.bindings || !data.results.bindings.length) {
        container.innerHTML = '<pre class="text-muted">无结果</pre>';
        return;
    }
    
    const bindings = data.results.bindings;
    const vars = data.head ? data.head.vars : Object.keys(bindings[0] || {});
    
    let html = '<table class="table table-sm table-striped"><thead><tr>';
    vars.forEach(v => html += '<th>' + v + '</th>');
    html += '</tr></thead><tbody>';
    
    bindings.forEach(row => {
        html += '<tr>';
        vars.forEach(v => {
            const val = row[v];
            html += '<td>' + (val ? `<span class="badge bg-light text-dark">${val.type || 'uri'}</span> ${val.value}` : '-') + '</td>';
        });
        html += '</tr>';
    });
    html += '</tbody></table>';
    html += '<p class="text-muted mt-2">共 ' + bindings.length + ' 条结果</p>';
    
    container.innerHTML = html;
}

function clearQuery() {
    document.getElementById('sparqlQuery').value = '';
    document.getElementById('queryResults').innerHTML = '<pre class="text-muted">查询结果将显示在这里...</pre>';
}

function loadPresetQuery(type) {
    const queries = {
        classes: `PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX owl: <http://www.w3.org/2002/07/owl#>

SELECT ?class (COUNT(?instance) AS ?count)
WHERE {
  ?instance a ?class .
  FILTER(isIRI(?class))
}
GROUP BY ?class
ORDER BY DESC(?count)
LIMIT 20`,
        properties: `PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

SELECT ?property ?type (COUNT(?s) AS ?usage)
WHERE {
  ?s ?property ?o .
  BIND(DATATYPE(?o) AS ?dtype)
  BIND(IF(isIRI(?o), "object", "datatype") AS ?type)
}
GROUP BY ?property ?type
ORDER BY DESC(?usage)
LIMIT 20`,
        individuals: `SELECT ?s ?type
WHERE {
  ?s a ?type .
}
LIMIT 50`,
        count: `SELECT (COUNT(*) AS ?triples)
WHERE {
  ?s ?p ?o
}`
    };
    document.getElementById('sparqlQuery').value = queries[type] || '';
    document.getElementById('queryType').value = 'select';
}

function copyResults() {
    const results = document.getElementById('queryResults').textContent;
    navigator.clipboard.writeText(results).then(() => toast('success', '已复制到剪贴板'));
}
