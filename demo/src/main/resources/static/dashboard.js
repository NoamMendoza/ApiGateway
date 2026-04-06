// ===== ESTADO GLOBAL =====
let statusChart = null;
const API_KEY_STORAGE = 'pgw_api_key';

// ===== INICIALIZACIÓN =====
document.addEventListener('DOMContentLoaded', () => {
    ensureApiKey();
    loadMetrics();
    setupFilters();
    loadPayments();
    setInterval(() => { loadMetrics(); loadPayments(); }, 30_000);
});

// ===== API KEY =====
function ensureApiKey() {
    if (!localStorage.getItem(API_KEY_STORAGE)) {
        const key = prompt('Ingresa tu API Key para cargar las transacciones:\n(se guarda en localStorage)');
        if (key) localStorage.setItem(API_KEY_STORAGE, key.trim());
    }
}

// ===== HELPER: parsear fechas de Jackson 3 =====
// Jackson 3.x serializa LocalDateTime como array [year, month, day, hour, min, sec, nano]
function parseJavaDate(val) {
    if (!val) return 'N/A';
    if (Array.isArray(val)) {
        const [year, month, day] = val;
        return new Date(year, month - 1, day).toLocaleDateString('es-MX');
    }
    return new Date(val).toLocaleDateString('es-MX');
}

// ===== MÉTRICAS (KPIs + Gráfica) =====
async function loadMetrics() {
    try {
        const res = await fetch('/api/admin/metrics');
        const data = await res.json();

        document.getElementById('total-today').textContent   = data.totalToday;
        document.getElementById('revenue-today').textContent = `$${Number(data.revenueToday).toLocaleString('es-MX', { minimumFractionDigits: 2 })}`;
        document.getElementById('success-rate').textContent  = `${data.successRate.toFixed(1)}%`;
        document.getElementById('disputes').textContent      = data.activeDisputes;
        document.getElementById('last-updated').textContent  =
            `Actualizado: ${new Date().toLocaleTimeString('es-MX')}`;

        renderChart(data.statusDistribution);
    } catch (err) {
        console.error('Error cargando métricas:', err);
    }
}

// ===== GRÁFICA DE DONA =====
function renderChart(dist) {
    const labels = Object.keys(dist);
    const values = Object.values(dist);
    const colors = {
        CAPTURED: '#10b981', PENDING: '#f59e0b', DECLINED: '#ef4444',
        REFUNDED: '#6366f1', DISPUTED: '#ff6b6b', FAILED: '#64748b',
    };

    const ctx = document.getElementById('status-chart').getContext('2d');
    if (statusChart) statusChart.destroy();

    statusChart = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels,
            datasets: [{
                data: values,
                backgroundColor: labels.map(l => colors[l] || '#334155'),
                borderWidth: 0,
                hoverOffset: 8,
            }]
        },
        options: {
            cutout: '70%',
            plugins: {
                legend: { position: 'bottom', labels: { color: '#64748b', padding: 16, font: { size: 12 } } }
            }
        }
    });
}

// ===== TABLA DE TRANSACCIONES =====
let filterTimeout = null;

function setupFilters() {
    const inputs = document.querySelectorAll('.filter-input');
    inputs.forEach(input => {
        input.addEventListener('input', () => {
            clearTimeout(filterTimeout);
            filterTimeout = setTimeout(() => {
                loadPayments();
            }, 500); // Debounce de 500ms
        });
    });
}

async function loadPayments() {
    const apiKey = localStorage.getItem(API_KEY_STORAGE);
    if (!apiKey) return;

    // Recoger valores de los filtros
    const params = new URLSearchParams({ page: 0, size: 20 });
    
    const id = document.getElementById('filter-id').value.trim();
    if (id) params.append('paymentId', id);
    
    const status = document.getElementById('filter-status').value;
    if (status) params.append('status', status);
    
    const start = document.getElementById('filter-start-date').value;
    if (start) params.append('startDate', start + 'T00:00:00');
    
    const end = document.getElementById('filter-end-date').value;
    if (end) params.append('endDate', end + 'T23:59:59.999999');
    
    const min = document.getElementById('filter-min').value;
    if (min) params.append('minAmount', min);
    
    const max = document.getElementById('filter-max').value;
    if (max) params.append('maxAmount', max);

    try {
        const res = await fetch(`/api/payments?${params.toString()}`, {
            headers: { 'X-API-KEY': apiKey }
        });

        if (!res.ok) {
            if (res.status === 429) {
                document.getElementById('payments-body').innerHTML = 
                    `<tr><td colspan="4" style="color:#f59e0b; text-align:center;">Límite de recargas excedido. Espera 60s.</td></tr>`;
            } else {
                document.getElementById('payments-body').innerHTML =
                    `<tr><td colspan="4" style="color:#ef4444; text-align:center;">Error: API Key inválida o servidor inalcanzable</td></tr>`;
            }
            return;
        }

        const payments = await res.json();
        renderTable(payments);
    } catch (err) {
        console.error('Error cargando pagos:', err);
    }
}

// ===== RENDER DE TABLA =====
function escapeHTML(str) {
    if (!str) return '';
    return str.toString()
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function renderTable(payments) {
    const tbody = document.getElementById('payments-body');

    if (!payments || payments.length === 0) {
        tbody.innerHTML = `<tr><td colspan="4" style="color:#64748b; text-align:center; padding:2rem">Sin transacciones</td></tr>`;
        return;
    }

    tbody.innerHTML = payments.map(p => `
        <tr>
            <td title="${escapeHTML(p.id)}">${escapeHTML(p.id).substring(0, 8)}…</td>
            <td>$${Number(p.amount).toLocaleString('es-MX', { minimumFractionDigits: 2 })} ${escapeHTML(p.currency)}</td>
            <td><span class="badge badge-${escapeHTML(p.status)}">${escapeHTML(p.status)}</span></td>
            <td>${escapeHTML(parseJavaDate(p.createdAt))}</td>
        </tr>
    `).join('');
}
