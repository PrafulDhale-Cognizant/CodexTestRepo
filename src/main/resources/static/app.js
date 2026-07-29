const form = document.querySelector('#claim-form');
const result = document.querySelector('#result');

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  if (!form.reportValidity()) return;
  const button = form.querySelector('.submit');
  button.disabled = true;
  button.querySelector('span').textContent = 'Checking legacy data…';
  const data = Object.fromEntries(new FormData(form));
  data.amount = Number(data.amount);
  try {
    const response = await fetch('/api/v1/claims/adjudicate', {
      method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(data)
    });
    const payload = await response.json();
    if (!response.ok) throw new Error(Object.values(payload.errors || {}).join(', ') || payload.detail || 'Unable to check claim');
    renderDecision(payload);
  } catch (error) {
    result.innerHTML = `<div class="empty-state"><h2>We couldn’t run this check</h2><p>${escapeHtml(error.message)}</p><div class="check-list"><span>Please verify the details and try again.</span></div></div>`;
  } finally {
    button.disabled = false;
    button.querySelector('span').textContent = 'Run claim check';
  }
});

function renderDecision(d) {
  const reasons = d.reasons.map(r => `<div class="reason"><b>${escapeHtml(r.code)} · ${escapeHtml(r.severity)}</b><p>${escapeHtml(r.message)}</p></div>`).join('');
  const duplicate = d.duplicateMatch ? `<div class="reason"><b>Matched claim ${escapeHtml(d.duplicateMatch.claimId)}</b><p>${d.duplicateMatch.confidence}% exact match · ${escapeHtml(d.duplicateMatch.serviceDate)} · $${escapeHtml(d.duplicateMatch.amount)}</p></div>` : '';
  result.innerHTML = `<div class="decision ${d.status}"><div class="decision-head"><span class="decision-badge">${escapeHtml(d.status)}</span><h2>${escapeHtml(d.headline)}</h2><span class="claim-ref">${escapeHtml(d.claimId)}</span></div>${reasons}${duplicate}<div class="decision-meta"><span>Processed in ${d.processingTimeMs} ms</span><span>Correlation ${escapeHtml(d.correlationId.slice(0, 8))}</span></div></div>`;
}

function escapeHtml(value) {
  const node = document.createElement('span'); node.textContent = String(value); return node.innerHTML;
}
