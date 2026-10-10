const state = { sessionId: null, pollTimer: null, rules: null };
const $ = (selector) => document.querySelector(selector);

async function requestJson(url, options = {}) {
  const response = await fetch(url, options);
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(data.error || `Request failed: ${response.status}`);
  }
  return data;
}

function toast(message) {
  const element = $('#toast');
  element.textContent = message;
  element.classList.remove('hidden');
  setTimeout(() => element.classList.add('hidden'), 3200);
}

async function loadTemplates() {
  const templates = await requestJson('/api/doc/templates');
  const select = $('#template');
  const ruleSelect = $('#rule-template');
  const options = templates.map(item => `<option value="${item}">${item}</option>`).join('');
  select.innerHTML = templates.map(item => `<option value="${item}">${item}</option>`).join('');
  ruleSelect.innerHTML = options;
  const mode = await requestJson('/api/doc/mode');
  $('#mode').textContent = mode.mode === 'mock' ? 'Mock Model' : `Real Model · ${mode.model}`;
  await loadRules($('#template').value);
  await loadSessions();
}

async function loadRules(templateName) {
  state.rules = await requestJson(`/api/doc/templates/${encodeURIComponent(templateName)}`);
  renderRules();
}

function renderRules() {
  const container = $('#reviewers');
  const reviewers = state.rules?.reviewers || [];
  container.innerHTML = '';
  reviewers.forEach((reviewer, index) => container.append(ruleEditor(reviewer, index)));
}

function ruleEditor(reviewer, index) {
  const article = document.createElement('article');
  article.className = 'reviewer-editor';
  article.innerHTML = `
    <label>规则名称
      <input class="rule-role" value="${escapeHtml(reviewer.role || '')}" placeholder="例如：legal_risk">
    </label>
    <label>审查规则
      <textarea class="rule-prompt" rows="4" placeholder="描述这个角色要检查什么、输出什么建议">${escapeHtml(reviewer.prompt || '')}</textarea>
    </label>
  `;
  const actions = document.createElement('div');
  actions.className = 'feedback';
  const remove = document.createElement('button');
  remove.className = 'secondary';
  remove.textContent = '删除';
  remove.onclick = () => article.remove();
  actions.append(remove);
  article.append(actions);
  article.dataset.index = index;
  return article;
}

function collectRules() {
  const reviewers = [...document.querySelectorAll('.reviewer-editor')].map(editor => ({
    role: editor.querySelector('.rule-role').value.trim(),
    prompt: editor.querySelector('.rule-prompt').value.trim()
  }));
  return { ...state.rules, reviewers };
}

async function saveRules() {
  const name = $('#rule-template').value;
  const template = collectRules();
  if (!template.reviewers.length) {
    toast('至少保留一条审查规则');
    return;
  }
  if (template.reviewers.some(item => !item.role || !item.prompt)) {
    toast('每条规则都需要名称和审查要求');
    return;
  }
  try {
    state.rules = await requestJson(`/api/doc/templates/${encodeURIComponent(name)}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(template)
    });
    renderRules();
    toast('审查规则已保存，下次审查生效');
  } catch (error) {
    toast(error.message);
  }
}

async function resetRules() {
  const name = $('#rule-template').value;
  if (!confirm('确定要恢复当前模板的默认审查规则吗？')) return;
  try {
    state.rules = await requestJson(`/api/doc/templates/${encodeURIComponent(name)}/override`, { method: 'DELETE' });
    renderRules();
    toast('已恢复默认规则');
  } catch (error) {
    toast(error.message);
  }
}

async function generateRules() {
  const name = $('#rule-template').value;
  const button = $('#generate-rules');
  button.disabled = true;
  button.textContent = 'AI 生成中';
  try {
    state.rules = await requestJson(`/api/doc/templates/${encodeURIComponent(name)}/generate-rules`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        businessContext: $('#rule-context').value.trim(),
        ruleCount: Number($('#rule-count').value || 5)
      })
    });
    renderRules();
    toast('AI 规则已生成，确认后请点击保存');
  } catch (error) {
    toast(error.message);
  } finally {
    button.disabled = false;
    button.textContent = 'AI 生成规则';
  }
}

async function loadSessions() {
  const sessions = await requestJson('/api/doc/sessions');
  const select = $('#previous-session');
  const current = select.value;
  select.innerHTML = '<option value="">全新审查</option>' + sessions
    .filter(item => item.status === 'done')
    .map(item => `<option value="${item.id}">v${item.version} · ${item.docName}</option>`)
    .join('');
  select.value = current;
}

async function upload(event) {
  event.preventDefault();
  const fileInput = $('#file');
  const button = $('#upload-button');
  if (!fileInput.files.length) return;
  button.disabled = true;
  button.textContent = '上传中';
  try {
    const body = new FormData();
    body.append('file', fileInput.files[0]);
    body.append('template', $('#template').value);
    const previousSessionId = $('#previous-session').value;
    if (previousSessionId) body.append('previousSessionId', previousSessionId);
    const result = await requestJson('/api/doc/upload', { method: 'POST', body });
    state.sessionId = result.id;
    $('#result').classList.remove('hidden');
    $('#doc-name').textContent = fileInput.files[0].name;
    updateLinks();
    $('#doc-status').textContent = '状态：正在审查';
    pollStatus();
    await loadSessions();
  } catch (error) {
    toast(error.message);
  } finally {
    button.disabled = false;
    button.textContent = '开始审查';
  }
}

function updateLinks() {
  $('#report-md').href = `/api/doc/${state.sessionId}/report?format=markdown`;
  $('#report-html').href = `/api/doc/${state.sessionId}/report?format=html`;
}

function pollStatus() {
  clearInterval(state.pollTimer);
  state.pollTimer = setInterval(async () => {
    await refreshResults();
    const statusText = $('#doc-status').textContent;
    if (statusText.includes('已完成') || statusText.includes('失败')) {
      clearInterval(state.pollTimer);
    }
  }, 1800);
}

async function refreshResults() {
  if (!state.sessionId) return;
  try {
    const status = await requestJson(`/api/doc/${state.sessionId}/status`);
    const versionSuffix = status.incremental
      ? ` · v${status.version} · 变更 ${status.changedSegmentCount}/${status.changedSegmentCount + status.unchangedSegmentCount}`
      : ` · v${status.version}`;
    $('#doc-status').textContent = status.error
      ? `状态：${status.status}（${status.error}）`
      : `状态：${status.status}${versionSuffix}`;
    const score = status.riskScore ?? '--';
    const scoreElement = $('#risk-score');
    scoreElement.textContent = score === '--' ? score : `${score}`;
    scoreElement.className = 'score';
    if (typeof score === 'number') {
      if (score < 50) scoreElement.classList.add('critical');
      else if (score < 80) scoreElement.classList.add('major');
      else scoreElement.classList.add('good');
    }
    if (status.status === 'done') await renderFindings();
    await renderAudit();
  } catch (error) {
    toast(error.message);
  }
}

async function renderAudit() {
  const logs = await requestJson(`/api/doc/${state.sessionId}/audit`);
  const element = $('#audit');
  element.innerHTML = logs.length ? '' : '<div class="empty">暂无审计记录</div>';
  logs.forEach(item => {
    const section = document.createElement('div');
    section.className = 'segment';
    section.innerHTML = `<strong>${escapeHtml(item.action)}</strong><div>${escapeHtml(item.detail || '')}</div><small>${new Date(item.createdAt).toLocaleString()}</small>`;
    element.append(section);
  });
}

async function deleteSession() {
  if (!state.sessionId || !confirm('确定要删除当前审查会话吗？')) return;
  try {
    await requestJson(`/api/doc/${state.sessionId}`, { method: 'DELETE' });
    clearInterval(state.pollTimer);
    state.sessionId = null;
    $('#result').classList.add('hidden');
    $('#file').value = '';
    $('#audit').innerHTML = '';
    toast('会话已删除');
    await loadSessions();
  } catch (error) {
    toast(error.message);
  }
}

async function renderFindings() {
  const [findings, segments] = await Promise.all([
    requestJson(`/api/doc/${state.sessionId}/findings`),
    requestJson(`/api/doc/${state.sessionId}/segments`)
  ]);
  const segmentMap = new Map(segments.map(item => [item.id, item.sectionPath]));
  const findingsElement = $('#findings');
  findingsElement.innerHTML = findings.length ? '' : '<div class="empty">未发现明显问题</div>';
  findings.forEach(item => {
    const article = document.createElement('article');
    article.className = `finding ${item.severity}`;
    article.innerHTML = `
      <div class="finding-top"><span class="tag">${item.severity}</span><span class="tag">${item.reviewerRole}</span><span class="tag">${segmentMap.get(item.segmentId) || '未知位置'}</span></div>
      <h3>${item.riskType || 'risk'}</h3>
      <p>${escapeHtml(item.description)}</p>
      <p><strong>建议：</strong>${escapeHtml(item.suggestedRevision || '无')}</p>
      ${item.userAction ? `<p><strong>处理状态：</strong>${item.userAction}</p>` : ''}
    `;
    const feedback = document.createElement('div');
    feedback.className = 'feedback';
    ['accepted', 'rejected', 'modified'].forEach(action => {
      const button = document.createElement('button');
      button.textContent = { accepted: '采纳', rejected: '驳回', modified: '修改' }[action];
      button.className = action;
      button.onclick = () => submitFeedback(item.id, action);
      feedback.append(button);
    });
    article.append(feedback);
    findingsElement.append(article);
  });

  const segmentsElement = $('#segments');
  segmentsElement.innerHTML = segments.length ? '' : '<div class="empty">暂无分段</div>';
  segments.forEach(item => {
    const section = document.createElement('div');
    section.className = 'segment';
    section.innerHTML = `<strong>${escapeHtml(item.sectionPath)}</strong><div>${escapeHtml(item.content)}</div>`;
    segmentsElement.append(section);
  });
}

async function submitFeedback(findingId, action) {
  let description;
  let suggestedRevision;
  if (action === 'modified') {
    suggestedRevision = prompt('请输入修改后的建议');
    if (!suggestedRevision) return;
  }
  try {
    await requestJson(`/api/doc/${state.sessionId}/feedback`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ findingId, action, description, suggestedRevision })
    });
    toast('反馈已保存');
    await renderFindings();
  } catch (error) {
    toast(error.message);
  }
}

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, item => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  })[item]);
}

$('#upload-form').addEventListener('submit', upload);
$('#refresh').addEventListener('click', refreshResults);
$('#delete-session').addEventListener('click', deleteSession);
$('#template').addEventListener('change', () => loadRules($('#template').value));
$('#rule-template').addEventListener('change', () => loadRules($('#rule-template').value));
$('#add-rule').addEventListener('click', () => $('#reviewers').append(ruleEditor({ role: '', prompt: '' }, Date.now())));
$('#generate-rules').addEventListener('click', generateRules);
$('#save-rules').addEventListener('click', saveRules);
$('#reset-rules').addEventListener('click', resetRules);
loadTemplates().catch(error => toast(error.message));
