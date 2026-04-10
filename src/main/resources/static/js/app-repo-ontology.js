/* app-repo-ontology.js — GraphDB 管理页脚本片段 */
function scrollToEditor(editorId) {
    const el = document.getElementById(editorId);
    if (!el) return;
    el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    el.focus();
}

function getTextAreaValue(id) {
    const el = document.getElementById(id);
    return el ? (el.value || '') : '';
}

function setTextAreaValue(id, value) {
    const el = document.getElementById(id);
    if (!el) return;
    el.value = value || '';
}

function inferPkFromSubjectTemplate(subjectTemplate) {
    // Example: :employee/{emp_id} -> emp_id
    const m = (subjectTemplate || '').match(/\{([^}]+)\}/);
    return m ? m[1] : null;
}

function uniqStrings(xs) {
    const set = new Set();
    (xs || []).forEach(x => {
        const v = (x || '').trim();
        if (v) set.add(v);
    });
    return Array.from(set.values()).sort();
}

function extractIrisFromOwl(owlXml) {
    const xml = owlXml || '';
    const classIris = [];
    const objPropIris = [];
    const dataPropIris = [];

    // RDF/XML patterns
    const classRe = /<owl:Class\b[^>]*\brdf:about="([^"]+)"/g;
    const opRe = /<owl:ObjectProperty\b[^>]*\brdf:about="([^"]+)"/g;
    const dpRe = /<owl:DatatypeProperty\b[^>]*\brdf:about="([^"]+)"/g;

    let m;
    while ((m = classRe.exec(xml)) !== null) classIris.push(m[1]);
    while ((m = opRe.exec(xml)) !== null) objPropIris.push(m[1]);
    while ((m = dpRe.exec(xml)) !== null) dataPropIris.push(m[1]);

    return {
        classes: uniqStrings(classIris),
        objectProperties: uniqStrings(objPropIris),
        dataProperties: uniqStrings(dataPropIris)
    };
}

function fillSelectOptions(selectId, items, placeholder) {
    const sel = document.getElementById(selectId);
    if (!sel) return;
    const ph = placeholder || '-- 请选择 --';
    sel.innerHTML = '<option value="">' + ph + '</option>' +
        (items || []).map(iri => '<option value="' + iri + '">' + iri + '</option>').join('');
}

function refreshOntologyEntityPickersFromEditors() {
    const owl = getTextAreaValue('createRepoOwlEditor');
    if (!owl.trim()) {
        toast('error', '请先生成并载入 OWL 到编辑器');
        return;
    }
    const { classes, objectProperties, dataProperties } = extractIrisFromOwl(owl);
    if (!classes.length) {
        toast('error', '未从 OWL 中解析到 owl:Class（请确认是 RDF/XML 或已载入正确内容）');
        return;
    }

    // Class pickers
    ['enhSubClassIri', 'enhSuperClassIri', 'enhDomainClass', 'enhRangeClass', 'enhEqClassA', 'enhEqClassB'].forEach(id => {
        fillSelectOptions(id, classes, '-- 选择 Class --');
    });

    // ObjectProperty pickers
    ['enhInversePropA', 'enhInversePropB', 'enhDomainRangeProp', 'enhSubProp', 'enhSuperProp'].forEach(id => {
        fillSelectOptions(id, objectProperties, '-- 选择 ObjectProperty --');
    });

    // Default helpful guesses
    const baseIri = (document.getElementById('createRepoBaseIri')?.value || '').trim();
    if (baseIri) {
        const trySet = (id, local) => {
            const el = document.getElementById(id);
            if (!el) return;
            const iri = baseIri + local;
            if (classes.includes(iri) || objectProperties.includes(iri)) el.value = iri;
        };
        trySet('enhSubClassIri', 'doctor');
        trySet('enhSuperClassIri', 'employee');
    }

    toast('success', `已解析：Class ${classes.length} 个，ObjectProperty ${objectProperties.length} 个，DataProperty ${dataProperties.length} 个`);
}

async function fetchText(url) {
    const resp = await fetch(url);
    if (!resp.ok) throw new Error('HTTP ' + resp.status + ' for ' + url);
    return await resp.text();
}

async function loadGeneratedFileToEditorsFromPage() {
    if (!createRepoGeneratedFilesPage || !createRepoGeneratedFilesPage.owlFile || !createRepoGeneratedFilesPage.obdaFile) {
        toast('error', '请先生成 OWL/OBDA，或手动上传文件后再载入');
        return;
    }
    try {
        showLoading();
        const owlText = await fetchText('/api/ontology/files/' + encodeURIComponent(createRepoGeneratedFilesPage.owlFile) + '/download');
        const obdaText = await fetchText('/api/ontology/files/' + encodeURIComponent(createRepoGeneratedFilesPage.obdaFile) + '/download');
        setTextAreaValue('createRepoOwlEditor', owlText);
        setTextAreaValue('createRepoObdaEditor', obdaText);
        // Populate pickers
        refreshOntologyEntityPickersFromEditors();
        toast('success', '已载入生成的 OWL/OBDA 到编辑器');
    } catch (e) {
        toast('error', '载入生成文件失败: ' + e.message);
    } finally {
        hideLoading();
    }
}

function applySubclassAxiomToOwlFromPage() {
    const subIri = (document.getElementById('enhSubClassIri')?.value || '').trim();
    const superIri = (document.getElementById('enhSuperClassIri')?.value || '').trim();
    if (!subIri || !superIri) {
        toast('error', '请选择 子类 / 父类');
        return;
    }
    const owl = getTextAreaValue('createRepoOwlEditor');
    if (!owl.trim()) {
        toast('error', '请先生成并载入 OWL 到编辑器');
        return;
    }
    if (owl.includes(subIri) && owl.includes('subClassOf') && owl.includes(superIri)) {
        toast('info', 'OWL 中似乎已存在该继承关系（未重复写入）');
        return;
    }

    const axiomXml =
        '\n\n    <owl:Class rdf:about="' + subIri + '">\n' +
        '        <rdfs:subClassOf rdf:resource="' + superIri + '"/>\n' +
        '    </owl:Class>\n';

    const idx = owl.lastIndexOf('</rdf:RDF>');
    if (idx === -1) {
        toast('error', 'OWL 格式不符合预期（未找到 </rdf:RDF>），请手工编辑');
        return;
    }
    const next = owl.slice(0, idx) + axiomXml + owl.slice(idx);
    setTextAreaValue('createRepoOwlEditor', next);
    toast('success', '已写入 OWL 继承（subClassOf）');
}

function upsertObjectPropertyBlock(owl, propIri, innerXmlLines) {
    const lines = innerXmlLines || [];
    const inner = lines.map(l => '        ' + l).join('\n');
    const block =
        '\n\n    <owl:ObjectProperty rdf:about="' + propIri + '">\n' +
        inner + '\n' +
        '    </owl:ObjectProperty>\n';

    // If property block exists, inject missing lines just before its closing tag
    const re = new RegExp('<owl:ObjectProperty\\b[^>]*rdf:about="' + propIri.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '"[\\s\\S]*?<\\/owl:ObjectProperty>', 'm');
    const m = owl.match(re);
    if (m && m[0]) {
        let existing = m[0];
        let changed = false;
        lines.forEach(l => {
            if (!existing.includes(l)) {
                existing = existing.replace('</owl:ObjectProperty>', '        ' + l + '\n    </owl:ObjectProperty>');
                changed = true;
            }
        });
        return changed ? owl.replace(re, existing) : owl;
    }

    // Otherwise append before </rdf:RDF>
    const idx = owl.lastIndexOf('</rdf:RDF>');
    if (idx === -1) return null;
    return owl.slice(0, idx) + block + owl.slice(idx);
}

function applyInverseOfToOwlFromPage() {
    const a = (document.getElementById('enhInversePropA')?.value || '').trim();
    const b = (document.getElementById('enhInversePropB')?.value || '').trim();
    if (!a || !b) {
        toast('error', '请选择 属性 A / 属性 B');
        return;
    }
    let owl = getTextAreaValue('createRepoOwlEditor');
    if (!owl.trim()) {
        toast('error', '请先生成并载入 OWL 到编辑器');
        return;
    }
    const next = upsertObjectPropertyBlock(owl, a, ['<owl:inverseOf rdf:resource="' + b + '"/>']);
    if (!next) {
        toast('error', 'OWL 格式不符合预期（未找到 </rdf:RDF>），请手工编辑');
        return;
    }
    if (next === owl) {
        toast('info', 'inverseOf 似乎已存在（未重复写入）');
        return;
    }
    setTextAreaValue('createRepoOwlEditor', next);
    toast('success', '已写入 OWL inverseOf');
}

function applyDomainRangeToOwlFromPage() {
    const p = (document.getElementById('enhDomainRangeProp')?.value || '').trim();
    const d = (document.getElementById('enhDomainClass')?.value || '').trim();
    const r = (document.getElementById('enhRangeClass')?.value || '').trim();
    if (!p || !d || !r) {
        toast('error', '请选择 属性 / domain / range');
        return;
    }
    let owl = getTextAreaValue('createRepoOwlEditor');
    if (!owl.trim()) {
        toast('error', '请先生成并载入 OWL 到编辑器');
        return;
    }
    const lines = [
        '<rdfs:domain rdf:resource="' + d + '"/>',
        '<rdfs:range rdf:resource="' + r + '"/>'
    ];
    const next = upsertObjectPropertyBlock(owl, p, lines);
    if (!next) {
        toast('error', 'OWL 格式不符合预期（未找到 </rdf:RDF>），请手工编辑');
        return;
    }
    if (next === owl) {
        toast('info', 'domain/range 似乎已存在（未重复写入）');
        return;
    }
    setTextAreaValue('createRepoOwlEditor', next);
    toast('success', '已写入 OWL domain/range');
}

function applySubPropertyToOwlFromPage() {
    const sub = (document.getElementById('enhSubProp')?.value || '').trim();
    const sup = (document.getElementById('enhSuperProp')?.value || '').trim();
    if (!sub || !sup) {
        toast('error', '请选择 子属性 / 父属性');
        return;
    }
    let owl = getTextAreaValue('createRepoOwlEditor');
    if (!owl.trim()) {
        toast('error', '请先生成并载入 OWL 到编辑器');
        return;
    }
    const line = '<rdfs:subPropertyOf rdf:resource="' + sup + '"/>';
    const next = upsertObjectPropertyBlock(owl, sub, [line]);
    if (!next) {
        toast('error', 'OWL 格式不符合预期（未找到 </rdf:RDF>），请手工编辑');
        return;
    }
    if (next === owl) {
        toast('info', 'subPropertyOf 似乎已存在（未重复写入）');
        return;
    }
    setTextAreaValue('createRepoOwlEditor', next);
    toast('success', '已写入 OWL subPropertyOf');
}

function applyEquivalentClassToOwlFromPage() {
    const a = (document.getElementById('enhEqClassA')?.value || '').trim();
    const b = (document.getElementById('enhEqClassB')?.value || '').trim();
    if (!a || !b) {
        toast('error', '请选择 类 A / 类 B');
        return;
    }
    const owl = getTextAreaValue('createRepoOwlEditor');
    if (!owl.trim()) {
        toast('error', '请先生成并载入 OWL 到编辑器');
        return;
    }
    // Simple RDF/XML insertion: add equivalentClass under Class A (or append new Class block)
    const re = new RegExp('<owl:Class\\b[^>]*rdf:about="' + a.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '"[\\s\\S]*?<\\/owl:Class>', 'm');
    const m = owl.match(re);
    let next = null;
    if (m && m[0]) {
        if (m[0].includes('owl:equivalentClass') && m[0].includes(b)) {
            toast('info', 'equivalentClass 似乎已存在（未重复写入）');
            return;
        }
        const injected = m[0].replace('</owl:Class>', '        <owl:equivalentClass rdf:resource="' + b + '"/>\n    </owl:Class>');
        next = owl.replace(re, injected);
    } else {
        const idx = owl.lastIndexOf('</rdf:RDF>');
        if (idx === -1) {
            toast('error', 'OWL 格式不符合预期（未找到 </rdf:RDF>），请手工编辑');
            return;
        }
        next = owl.slice(0, idx) +
            '\n\n    <owl:Class rdf:about="' + a + '">\n' +
            '        <owl:equivalentClass rdf:resource="' + b + '"/>\n' +
            '    </owl:Class>\n' +
            owl.slice(idx);
    }
    setTextAreaValue('createRepoOwlEditor', next);
    toast('success', '已写入 OWL equivalentClass');
}
function applyConditionalTypeMappingToObdaFromPage() {
    const typeLocal = (document.getElementById('enhTypeLocalName')?.value || '').trim();
    const subjectTemplate = (document.getElementById('enhSubjectTemplate')?.value || '').trim();
    const sqlCondition = (document.getElementById('enhSqlCondition')?.value || '').trim();
    if (!typeLocal || !subjectTemplate || !sqlCondition) {
        toast('error', '请填写 类型 / 个体模板 / SQL 条件');
        return;
    }
    const obda = getTextAreaValue('createRepoObdaEditor');
    if (!obda.trim()) {
        toast('error', '请先生成并载入 OBDA 到编辑器');
        return;
    }

    const pk = inferPkFromSubjectTemplate(subjectTemplate);
    if (!pk) {
        toast('error', '无法从“个体模板”中解析字段（需要形如 {emp_id}）');
        return;
    }

    const mappingId = 'MAPPING-ENH-TYPE-' + Date.now();
    const block =
        '\n' +
        'mappingId\t' + mappingId + '\n' +
        'target\t\t' + subjectTemplate + ' a :' + typeLocal + ' .\n' +
        'source\t\tSELECT `' + pk + '` ' + sqlCondition + '\n';

    if (obda.includes('target\t\t' + subjectTemplate + ' a :' + typeLocal)) {
        toast('info', 'OBDA 中似乎已存在该条件类型映射（未重复写入）');
        return;
    }

    const endIdx = obda.lastIndexOf(']]');
    if (endIdx === -1) {
        toast('error', 'OBDA 格式不符合预期（未找到 ]]），请手工编辑');
        return;
    }
    const next = obda.slice(0, endIdx) + block + '\n' + obda.slice(endIdx);
    setTextAreaValue('createRepoObdaEditor', next);
    toast('success', '已写入 OBDA 条件类型映射');
}

function bindCreateRepoDbInputListeners() {
    ['newRepoDbUrl', 'newRepoDbUser', 'newRepoDbPassword', 'newRepoDbDriver', 'newRepoBaseIri'].forEach(id => {
        const el = document.getElementById(id);
        if (!el) return;
        el.addEventListener('input', () => {
            updateGenerateButtonState(false);
        });
    });
}

function bindCreateRepoPageDbInputListeners() {
    ['createRepoDbUrl', 'createRepoDbUser', 'createRepoDbPassword', 'createRepoDbDriver', 'createRepoBaseIri'].forEach(id => {
        const el = document.getElementById(id);
        if (!el) return;
        el.addEventListener('input', () => {
            updateGenerateButtonStatePage(false);
        });
    });
}

function testDataSourceConnection() {
    const request = {
        jdbcUrl: document.getElementById('newRepoDbUrl').value,
        user: document.getElementById('newRepoDbUser').value,
        password: document.getElementById('newRepoDbPassword').value,
        driverClass: document.getElementById('newRepoDbDriver').value
    };
    if (!request.jdbcUrl || !request.user) {
        toast('error', '请先填写数据库连接信息');
        return;
    }

    showLoading();
    fetch('/api/ontology/datasource/test', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request)
    })
    .then(r => r.json())
    .then(data => {
        hideLoading();
        const resultDiv = document.getElementById('newRepoConnTestResult');
        resultDiv.style.display = 'block';
        if (data.success) {
            resultDiv.innerHTML = '<div class="alert alert-success mb-0"><i class="fas fa-check-circle me-2"></i>' +
                (data.message || '连接成功') + (data.elapsedMs != null ? ('（耗时 ' + data.elapsedMs + ' ms）') : '') + '</div>';
            updateGenerateButtonState(true);
            toast('success', '数据源连接成功');
        } else {
            resultDiv.innerHTML = '<div class="alert alert-danger mb-0"><i class="fas fa-times-circle me-2"></i>' +
                (data.message || '连接失败') + '</div>';
            updateGenerateButtonState(false);
            toast('error', '数据源连接失败');
        }
    })
    .catch(err => {
        hideLoading();
        const resultDiv = document.getElementById('newRepoConnTestResult');
        resultDiv.style.display = 'block';
        resultDiv.innerHTML = '<div class="alert alert-danger mb-0"><i class="fas fa-times-circle me-2"></i>连接失败: ' + err.message + '</div>';
        updateGenerateButtonState(false);
        toast('error', '数据源连接失败: ' + err.message);
    });
}

function testDataSourceConnectionFromPage() {
    const request = {
        jdbcUrl: document.getElementById('createRepoDbUrl').value,
        user: document.getElementById('createRepoDbUser').value,
        password: document.getElementById('createRepoDbPassword').value,
        driverClass: document.getElementById('createRepoDbDriver').value
    };
    if (!request.jdbcUrl || !request.user) {
        toast('error', '请先填写数据库连接信息');
        return;
    }

    showLoading();
    fetch('/api/ontology/datasource/test', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request)
    })
    .then(r => r.json())
    .then(data => {
        hideLoading();
        const resultDiv = document.getElementById('createRepoConnTestResult');
        resultDiv.style.display = 'block';
        if (data.success) {
            resultDiv.innerHTML = '<div class="alert alert-success mb-0"><i class="fas fa-check-circle me-2"></i>' +
                (data.message || '连接成功') + (data.elapsedMs != null ? ('（耗时 ' + data.elapsedMs + ' ms）') : '') + '</div>';
            updateGenerateButtonStatePage(true);
            toast('success', '数据源连接成功');
        } else {
            resultDiv.innerHTML = '<div class="alert alert-danger mb-0"><i class="fas fa-times-circle me-2"></i>' +
                (data.message || '连接失败') + '</div>';
            updateGenerateButtonStatePage(false);
            toast('error', '数据源连接失败');
        }
    })
    .catch(err => {
        hideLoading();
        const resultDiv = document.getElementById('createRepoConnTestResult');
        resultDiv.style.display = 'block';
        resultDiv.innerHTML = '<div class="alert alert-danger mb-0"><i class="fas fa-times-circle me-2"></i>连接失败: ' + err.message + '</div>';
        updateGenerateButtonStatePage(false);
        toast('error', '数据源连接失败: ' + err.message);
    });
}

function generateOntopFilesForCreate() {
    const request = {
        jdbcUrl: document.getElementById('newRepoDbUrl').value,
        user: document.getElementById('newRepoDbUser').value,
        password: document.getElementById('newRepoDbPassword').value,
        driverClass: document.getElementById('newRepoDbDriver').value,
        tablePattern: '%',
        baseIri: document.getElementById('newRepoBaseIri').value
    };

    if (!request.jdbcUrl || !request.user) {
        toast('error', '请先填写数据库连接信息');
        return;
    }

    showLoading();
    fetch('/api/ontology/generate/all', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request)
    })
    .then(r => r.json())
    .then(data => {
        hideLoading();
        const resultDiv = document.getElementById('newRepoGenerateResult');
        if (!data.success) {
            resultDiv.style.display = 'block';
            resultDiv.innerHTML = '<div class="alert alert-danger mb-0"><i class="fas fa-times-circle me-2"></i>' + (data.message || '生成失败') + '</div>';
            return;
        }

        createRepoGeneratedFiles = {
            owlFile: data.owlFile,
            obdaFile: data.obdaFile,
            propertiesFile: data.propertiesFile
        };

        let html = '<div class="card border-success mt-2"><div class="card-body">';
        html += '<div class="alert alert-success"><i class="fas fa-check-circle me-2"></i>生成成功，可直接创建仓库或下载后手动替换</div>';
        if (data.owl) {
            html += '<p class="mb-2"><strong>OWL统计:</strong> 表: ' + data.owl.tableCount + ' | 数据属性: ' + data.owl.dataPropertyCount + ' | 对象属性: ' + data.owl.objectPropertyCount + '</p>';
        }
        html += '<div class="d-flex flex-wrap gap-2">';
        if (data.owlFile) {
            html += '<a href="/api/ontology/files/' + data.owlFile + '/download" class="btn btn-outline-primary btn-sm"><i class="fas fa-file-code me-1"></i>下载 OWL</a>';
        }
        if (data.obdaFile) {
            html += '<a href="/api/ontology/files/' + data.obdaFile + '/download" class="btn btn-outline-success btn-sm"><i class="fas fa-file-alt me-1"></i>下载 OBDA</a>';
        }
        html += '</div></div></div>';

        resultDiv.innerHTML = html;
        resultDiv.style.display = 'block';
        toast('success', 'OWL/OBDA 生成成功');
    })
    .catch(err => {
        hideLoading();
        toast('error', '生成失败: ' + err.message);
    });
}

function generateOntopFilesForCreateFromPage() {
    const request = {
        jdbcUrl: document.getElementById('createRepoDbUrl').value,
        user: document.getElementById('createRepoDbUser').value,
        password: document.getElementById('createRepoDbPassword').value,
        driverClass: document.getElementById('createRepoDbDriver').value,
        tablePattern: '%',
        baseIri: document.getElementById('createRepoBaseIri').value
    };

    if (!request.jdbcUrl || !request.user) {
        toast('error', '请先填写数据库连接信息');
        return;
    }

    showLoading();
    fetch('/api/ontology/generate/all', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request)
    })
    .then(r => r.json())
    .then(data => {
        hideLoading();
        const resultDiv = document.getElementById('createRepoGenerateResult');
        if (!data.success) {
            resultDiv.style.display = 'block';
            resultDiv.innerHTML = '<div class="alert alert-danger mb-0"><i class="fas fa-times-circle me-2"></i>' + (data.message || '生成失败') + '</div>';
            return;
        }

        createRepoGeneratedFilesPage = {
            owlFile: data.owlFile,
            obdaFile: data.obdaFile,
            propertiesFile: data.propertiesFile
        };

        let html = '<div class="card border-success mt-2"><div class="card-body">';
        html += '<div class="alert alert-success"><i class="fas fa-check-circle me-2"></i>生成成功，可直接创建仓库或下载后手动替换</div>';
        if (data.owl) {
            html += '<p class="mb-2"><strong>OWL统计:</strong> 表: ' + data.owl.tableCount + ' | 数据属性: ' + data.owl.dataPropertyCount + ' | 对象属性: ' + data.owl.objectPropertyCount + '</p>';
        }
        html += '<div class="d-flex flex-wrap gap-2">';
        if (data.owlFile) {
            html += '<a href="/api/ontology/files/' + data.owlFile + '/download" class="btn btn-outline-primary btn-sm"><i class="fas fa-file-code me-1"></i>下载 OWL</a>';
        }
        if (data.obdaFile) {
            html += '<a href="/api/ontology/files/' + data.obdaFile + '/download" class="btn btn-outline-success btn-sm"><i class="fas fa-file-alt me-1"></i>下载 OBDA</a>';
        }
        html += '</div></div></div>';

        resultDiv.innerHTML = html;
        resultDiv.style.display = 'block';
        // Auto-load into editors for immediate tweak
        loadGeneratedFileToEditorsFromPage();
        toast('success', 'OWL/OBDA 生成成功');
    })
    .catch(err => {
        hideLoading();
        toast('error', '生成失败: ' + err.message);
    });
}

function createOntopRepository() {
    const formData = new FormData();
    formData.append('id', document.getElementById('newRepoId').value);
    formData.append('title', document.getElementById('newRepoTitle').value);
    formData.append('dbUrl', document.getElementById('newRepoDbUrl').value);
    formData.append('dbUser', document.getElementById('newRepoDbUser').value);
    formData.append('dbPassword', document.getElementById('newRepoDbPassword').value);
    formData.append('dbDriver', document.getElementById('newRepoDbDriver').value);
    formData.append('baseIri', document.getElementById('newRepoBaseIri').value);
    const owlFile = document.getElementById('newRepoOwlFile').files[0];
    const obdaFile = document.getElementById('newRepoObdaFile').files[0];
    if (owlFile) formData.append('owlFile', owlFile);
    if (obdaFile) formData.append('obdaFile', obdaFile);

    if (createRepoGeneratedFiles && createRepoGeneratedFiles.owlFile && createRepoGeneratedFiles.obdaFile) {
        formData.append('generatedOwlFile', createRepoGeneratedFiles.owlFile);
        formData.append('generatedObdaFile', createRepoGeneratedFiles.obdaFile);
    }
    
    showLoading();
    fetch('/api/graphdb/repositories/ontop', { method: 'POST', body: formData })
        .then(r => r.json())
        .then(data => {
            hideLoading();
            if (data.success) {
                toast('success', '仓库创建成功');
                bootstrap.Modal.getInstance(document.getElementById('createRepoModal')).hide();
                loadRepositories();
            } else {
                toast('error', data.error || '创建失败');
            }
        })
        .catch(err => {
            hideLoading();
            toast('error', '创建失败: ' + err.message);
        });
}

function createOntopRepositoryFromPage() {
    const formData = new FormData();
    formData.append('id', document.getElementById('createRepoId').value);
    formData.append('title', document.getElementById('createRepoTitle').value);
    formData.append('dbUrl', document.getElementById('createRepoDbUrl').value);
    formData.append('dbUser', document.getElementById('createRepoDbUser').value);
    formData.append('dbPassword', document.getElementById('createRepoDbPassword').value);
    formData.append('dbDriver', document.getElementById('createRepoDbDriver').value);
    formData.append('baseIri', document.getElementById('createRepoBaseIri').value);

    // Prefer editor content > uploaded file > generated file references
    const owlEditor = getTextAreaValue('createRepoOwlEditor').trim();
    const obdaEditor = getTextAreaValue('createRepoObdaEditor').trim();
    if (owlEditor) {
        formData.append('owlFile', new File([owlEditor], 'ontology.owl', { type: 'application/xml' }));
    } else {
        const owlFile = document.getElementById('createRepoOwlFile').files[0];
        if (owlFile) formData.append('owlFile', owlFile);
    }
    if (obdaEditor) {
        formData.append('obdaFile', new File([obdaEditor], 'mapping.obda', { type: 'text/plain' }));
    } else {
        const obdaFile = document.getElementById('createRepoObdaFile').files[0];
        if (obdaFile) formData.append('obdaFile', obdaFile);
    }

    if (createRepoGeneratedFilesPage && createRepoGeneratedFilesPage.owlFile && createRepoGeneratedFilesPage.obdaFile) {
        formData.append('generatedOwlFile', createRepoGeneratedFilesPage.owlFile);
        formData.append('generatedObdaFile', createRepoGeneratedFilesPage.obdaFile);
    }

    showLoading();
    fetch('/api/graphdb/repositories/ontop', { method: 'POST', body: formData })
        .then(r => r.json())
        .then(data => {
            hideLoading();
            if (data.success) {
                toast('success', '仓库创建成功');
                loadRepositories();
                switchSection('repositories');
            } else {
                toast('error', data.error || '创建失败');
            }
        })
        .catch(err => {
            hideLoading();
            toast('error', '创建失败: ' + err.message);
        });
}
