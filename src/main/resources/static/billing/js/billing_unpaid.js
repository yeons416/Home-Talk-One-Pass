// 필터 변경 시 GET /api/billing/admin/unpaid 재조회
// → tbody 교체, 납부완료 처리 confirm 후
// PATCH /api/billing/admin/{id}/paid 호출
// → 해당 행 즉시 업데이트

/* ================================================================
   billing_unpaid.js
   관리자 미납 세대 관리 페이지 동작
   - 필터 칩 그리드 패널 (연도 / 월 / 동 / 미납 보기)
   - 필터 변경 시 API 재조회 → tbody 교체
   - 납부완료 처리 confirm 팝업
   - 페이지네이션
   ================================================================ */

'use strict';

/* ── 상수 ────────────────────────────────────────────────────── */
const YEARS  = [2026,2025,2024,2023,2022,2021,2020,2019,2018];
const MONTHS = ['1월','2월','3월','4월','5월','6월','7월','8월',
    '9월','10월','11월','12월'];

/* ── 상태 ────────────────────────────────────────────────────── */
let selYear        = new Date().getFullYear();
let selMonth       = null;
let selDong        = null;
let selFilter      = 'unpaid';  // 'all' | 'unpaid' | 'long' | 'paid'
let currentPage    = 0;
let openPanel      = null;
let pendingBilling = null;      // confirm 대기 고지서 정보

/* ================================================================
   초기화
================================================================ */
document.addEventListener('DOMContentLoaded', () => {
    fetchUnpaidList(); // 추가
    document.addEventListener('click', e => {
        if (!e.target.closest('.panel-wrap')) closeAllPanels();
    });
});

/* ================================================================
   필터 패널
================================================================ */
function togglePanel(name) {
    if (openPanel === name) { closeAllPanels(); return; }
    closeAllPanels();
    openPanel = name;

    if (name === 'year')   buildYearGrid();
    if (name === 'month')  buildMonthGrid();
    if (name === 'dong')   buildDongGrid();
    if (name === 'unpaid') buildUnpaidFilterGrid();

    const panelMap = {
        year:'panelYear', month:'panelMonth',
        dong:'panelDong', unpaid:'panelUnpaid'
    };
    const btnMap = {
        year:'btnYear', month:'btnMonth',
        dong:'btnDong', unpaid:'btnUnpaidFilter'
    };
    document.getElementById(panelMap[name]).style.display = 'block';
    document.getElementById(btnMap[name]).classList.add('active');
}

function closeAllPanels() {
    ['panelYear','panelMonth','panelDong','panelUnpaid'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.style.display = 'none';
    });
    ['btnYear','btnMonth','btnDong','btnUnpaidFilter'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.classList.remove('active');
    });
    openPanel = null;
}

/* ── 그리드 빌더 ── */
function buildYearGrid() {
    document.getElementById('yearGrid').innerHTML =
        `<button class="chip full${selYear===null?' selected':''}"
            onclick="pickYear(null)">전체</button>`
        + YEARS.map(y =>
            `<button class="chip${y===selYear?' selected':''}"
                onclick="pickYear(${y})">${y}년</button>`
        ).join('');
}

function buildMonthGrid() {
    document.getElementById('monthGrid').innerHTML =
        `<button class="chip full${selMonth===null?' selected':''}"
            onclick="pickMonth(null)">전체</button>`
        + MONTHS.map((m, i) =>
            `<button class="chip${selMonth===i+1?' selected':''}"
                onclick="pickMonth(${i+1})">${m}</button>`
        ).join('');
}

function buildDongGrid() {
    // 동 목록은 현재 tbody에서 파싱
    const dongs = [...new Set(
        [...document.querySelectorAll('#billingTableBody tr td:nth-child(2)')]
            .map(td => td.textContent.replace(/\d+호$/, '').trim())
            .filter(Boolean)
    )].sort();

    const grid = document.getElementById('dongGrid');
    grid.innerHTML =
        `<button class="chip full${!selDong?' selected':''}"
            onclick="pickDong(null)">전체 동</button>`
        + dongs.map(d =>
            `<button class="chip${selDong===d?' selected':''}"
                onclick="pickDong('${d}')">${d}</button>`
        ).join('');
}

function buildUnpaidFilterGrid() {
    const opts = [
        { val: 'all',    label: '전체'           },
        { val: 'unpaid', label: '미납만 보기'     },
        { val: 'long',   label: '3개월 이상 체납' },
        { val: 'paid',   label: '납부완료'         },
    ];
    document.getElementById('panelUnpaid').querySelector('.chip-grid').innerHTML =
        opts.map(o =>
            `<button class="chip status-chip${selFilter===o.val?' selected':''}"
                onclick="pickUnpaidFilter('${o.val}','${o.label}')">${o.label}</button>`
        ).join('');
}

/* ── 선택 핸들러 ── */
function pickYear(y) {
    selYear = y;
    document.getElementById('lblYear').textContent = y ? y + '년' : '전체';
    closeAllPanels(); currentPage = 0; fetchUnpaidList();
}

function pickMonth(m) {
    selMonth = m;
    document.getElementById('lblMonth').textContent = m ? MONTHS[m-1] : '전체 월';
    closeAllPanels(); currentPage = 0; fetchUnpaidList();
}

function pickDong(d) {
    selDong = d;
    document.getElementById('lblDong').textContent = d ? d : '전체 동';
    closeAllPanels(); currentPage = 0; fetchUnpaidList();
}

function pickUnpaidFilter(val, label) {
    selFilter = val;
    document.getElementById('lblUnpaid').textContent = label;
    closeAllPanels(); currentPage = 0; fetchUnpaidList();
}

/* ================================================================
   API 재조회 — GET /hometalk/api/billing/admin/unpaid
   Controller 파라미터: year, month, dong, status, overdue, page, size
================================================================ */
async function fetchUnpaidList() {
    const params = new URLSearchParams();

    if (selYear)  params.set('year',  selYear);
    if (selMonth) params.set('month', `${selYear}-${String(selMonth).padStart(2, '0')}`);
    if (selDong)  params.set('dong',  selDong);

    // selFilter → Controller 파라미터 변환
    // Controller: status(UNPAID/PAID), overdue(true/false)
    if (selFilter === 'unpaid') params.set('status',  'UNPAID');
    if (selFilter === 'paid')   params.set('status',  'PAID');
    if (selFilter === 'long')   params.set('overdue', 'true');
    // 'all' 은 파라미터 없음 (전체 조회)

    params.set('page', currentPage);
    params.set('size', 10);

    try {
        const res  = await fetch(
            `${CONTEXT_PATH}/api/billing/admin/unpaid?${params.toString()}`
        );
        const data = await res.json();
        renderTableBody(data.content);
        renderPagination(data.number, data.totalPages);
    } catch (err) {
        console.error('미납 목록 조회 실패', err);
    }
}

/* ── tbody 교체 ── */
function renderTableBody(items) {
    const tbody = document.getElementById('billingTableBody');
    if (!items || items.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8" class="empty-state">조회된 미납 세대가 없습니다.</td>
            </tr>`;
        return;
    }

    tbody.innerHTML = items.map((item, idx) => {
        const isUnpaid = item.status === 'UNPAID';
        const num      = currentPage * 10 + idx + 1;

        const badge = isUnpaid
            ? `<span class="badge badge-unpaid">UNPAID</span>`
            : `<span class="badge badge-paid">PAID</span>`;

        const action = isUnpaid
            ? `<button class="btn-process"
                data-billing-id="${item.billingId}"
                data-unit="${item.unit}"
                data-month="${item.billingMonth}"
                onclick="openConfirm(this)">납부완료 처리</button>`
            : `<span class="btn-process done-txt">처리완료</span>`;

        return `
        <tr class="${isUnpaid ? '' : 'done'}">
            <td>${num}</td>
            <td>${item.unit}</td>
            <td>${item.residentName || '—'}</td>
            <td>${item.billingMonth}</td>
            <td>${Number(item.totalAmount).toLocaleString()}원</td>
            <td>${item.dueDate ? item.dueDate.replace(/-/g, '.') : '—'}</td>
            <td>${badge}</td>
            <td>${action}</td>
        </tr>`;
    }).join('');
}

/* ── 페이지네이션 교체 ── */
function renderPagination(cur, total) {
    const wrap = document.querySelector('.pagination');
    if (!wrap || total <= 1) {
        if (wrap) wrap.innerHTML = '';
        return;
    }

    const prevDisabled = cur === 0 ? 'disabled' : '';
    const nextDisabled = cur === total - 1 ? 'disabled' : '';

    wrap.innerHTML =
        `<button class="page-btn ${prevDisabled}"
            onclick="${cur > 0 ? `goPage(${cur-1})` : ''}">&lt;</button>`
        + Array.from({ length: total }, (_, i) =>
            `<button class="page-btn${i === cur ? ' active' : ''}"
                onclick="goPage(${i})">${i + 1}</button>`
        ).join('')
        + `<button class="page-btn ${nextDisabled}"
            onclick="${cur < total-1 ? `goPage(${cur+1})` : ''}">&gt;</button>`;
}

function goPage(page) {
    currentPage = page;
    fetchUnpaidList();
}

/* ================================================================
   납부완료 처리 confirm 팝업
================================================================ */
function openConfirm(btn) {
    pendingBilling = {
        billingId: btn.dataset.billingId,
        unit:      btn.dataset.unit,
        month:     btn.dataset.month,
    };

    document.getElementById('confirmUnit').textContent  = pendingBilling.unit;
    document.getElementById('confirmMonth').textContent =
        pendingBilling.month.replace('-', '년 ') + '월 관리비';

    document.getElementById('confirmOverlay').style.display = 'flex';
}

function closeConfirm() {
    document.getElementById('confirmOverlay').style.display = 'none';
    pendingBilling = null;
}

/* ── 확인 → PATCH /api/billing/admin/{billingId}/paid ── */
async function processPayment() {
    if (!pendingBilling) return;
    const { billingId } = pendingBilling;
    closeConfirm();

    try {
        const res = await fetch(
            `${CONTEXT_PATH}/api/billing/admin/${billingId}/paid?adminId=1`,
            {
                method:  'PATCH',
                headers: { [CSRF_HEADER]: CSRF_TOKEN },
            }
        );

        if (!res.ok) throw new Error('처리 실패');

        // 해당 행 즉시 업데이트
        const row = document.querySelector(
            `[data-billing-id="${billingId}"]`
        )?.closest('tr');

        if (row) {
            row.classList.add('done');
            const badgeCell  = row.querySelector('td:nth-child(7)');
            const actionCell = row.querySelector('td:nth-child(8)');
            if (badgeCell)  badgeCell.innerHTML  = `<span class="badge badge-paid">PAID</span>`;
            if (actionCell) actionCell.innerHTML = `<span class="btn-process done-txt">처리완료</span>`;
        }

        // 통계 카드 갱신
        refreshStats();

    } catch (err) {
        console.error('납부완료 처리 실패', err);
        alert('처리 중 오류가 발생했습니다. 다시 시도해 주세요.');
    }
}

/* ── 통계 카드 갱신 — GET /api/billing/admin/stats ── */
async function refreshStats() {
    const month = selYear && selMonth
        ? `${selYear}-${String(selMonth).padStart(2, '0')}`
        : `${new Date().getFullYear()}-${String(new Date().getMonth()+1).padStart(2,'0')}`;

    try {
        const res  = await fetch(
            `${CONTEXT_PATH}/api/billing/admin/stats?billingMonth=${month}`
        );
        const data = await res.json();

        // 통계 카드 DOM 업데이트
        const cards = document.querySelectorAll('.stat-card .stat-value');
        if (cards[0]) cards[0].textContent = data.total   ?? '—';
        if (cards[1]) cards[1].textContent = data.paid    ?? '—';
        if (cards[2]) cards[2].textContent = data.unpaid  ?? '—';
        if (cards[3]) cards[3].textContent = data.paidRate != null
            ? data.paidRate.toFixed(1) + '%' : '—';

    } catch (err) {
        console.error('통계 갱신 실패', err);
    }
}