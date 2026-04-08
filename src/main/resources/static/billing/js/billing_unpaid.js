// 필터 변경 시 GET /api/billing/admin/unpaid 재조회
// → tbody 교체, 납부완료 처리 confirm 후
// PATCH /api/billing/admin/{id}/pay 호출
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
    // 동 목록은 서버에서 내려주는 게 정확하므로 현재 tbody에서 파싱
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
        { val: 'all',    label: '전체'          },
        { val: 'unpaid', label: '미납만 보기'    },
        { val: 'long',   label: '3개월 이상 체납' },
        { val: 'paid',   label: '납부완료'        },
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
   API 재조회 — GET /hometop/api/billing/admin/unpaid
================================================================ */
async function fetchUnpaidList() {
    const params = new URLSearchParams();
    if (selYear)   params.set('year',   selYear);
    if (selMonth)  params.set('month',  String(selMonth).padStart(2, '0'));
    if (selDong)   params.set('dong',   selDong);
    if (selFilter) params.set('filter', selFilter);
    params.set('page', currentPage);
    params.set('size', 10);

    try {
        const res  = await fetch(
            `${CONTEXT_PATH}/api/billing/admin/unpaid?${params.toString()}`
        );
        const data = await res.json();
        renderTableBody(data.content);
        renderPagination(data.currentPage, data.totalPages);
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
                data-unit="${item.dong} ${item.ho}"
                data-month="${item.billingMonth}"
                onclick="openConfirm(this)">납부완료 처리</button>`
            : `<span class="btn-process done-txt">처리완료</span>`;

        return `
        <tr class="${isUnpaid ? '' : 'done'}">
            <td>${num}</td>
            <td>${item.dong} ${item.ho}</td>
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

/* ── 확인 → PATCH API ── */
async function processPayment() {
    if (!pendingBilling) return;
    const { billingId, unit, month } = pendingBilling;
    closeConfirm();

    try {
        const res = await fetch(
            `${CONTEXT_PATH}/api/billing/admin/${billingId}/pay`,
            {
                method:  'PATCH',
                headers: { [CSRF_HEADER]: CSRF_TOKEN },
            }
        );

        if (!res.ok) throw new Error('처리 실패');

        // 해당 행 즉시 업데이트 (API 재조회 없이 UX 즉시 반영)
        const row = document.querySelector(
            `[data-billing-id="${billingId}"]`
        )?.closest('tr');

        if (row) {
            row.classList.add('done');
            // 뱃지 교체
            const badgeCell = row.querySelector('td:nth-child(7)');
            if (badgeCell) badgeCell.innerHTML = `<span class="badge badge-paid">PAID</span>`;
            // 처리 버튼 교체
            const actionCell = row.querySelector('td:nth-child(8)');
            if (actionCell) actionCell.innerHTML = `<span class="btn-process done-txt">처리완료</span>`;
        }

        // 통계 카드 갱신 (전체 페이지 fetch 대신 간단히 재조회)
        refreshStats();

    } catch (err) {
        console.error('납부완료 처리 실패', err);
        alert('처리 중 오류가 발생했습니다. 다시 시도해 주세요.');
    }
}

/* ── 통계 카드 갱신 ── */
async function refreshStats() {
    // 현재 선택된 부과월 기준으로 통계 재조회
    const month = selYear && selMonth
        ? `${selYear}-${String(selMonth).padStart(2, '0')}`
        : null;
    if (!month) return;

    try {
        const res  = await fetch(
            `${CONTEXT_PATH}/api/billing/admin/unpaid?year=${selYear}`
            + (selMonth ? `&month=${String(selMonth).padStart(2,'0')}` : '')
            + `&page=0&size=1`
        );
        // 통계는 별도 API가 없으므로 페이지 새로고침으로 처리
        // TODO: 통계 전용 API 추가 후 교체
    } catch (err) {
        // 통계 갱신 실패는 무시 (다음 필터 변경 시 자동 갱신)
    }
}