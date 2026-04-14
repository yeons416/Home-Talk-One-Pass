/* ================================================================
   billing_resident.js
   입주민 관리비 페이지 동작
   - 필터 칩 그리드 패널 (연도 / 월 / 상태)
   - 필터 변경 시 API 재조회 → 목록 교체
   - 더보기 (3건씩 추가 로드)
   - 고지서 모달 (API fetch → 렌더링)
   ================================================================ */

'use strict';

/* ── 상수 ────────────────────────────────────────────────────── */
const YEARS  = [2026,2025,2024,2023,2022,2021,2020,2019,2018];
const MONTHS = ['1월','2월','3월','4월','5월','6월','7월','8월',
                '9월','10월','11월','12월'];

/* ── 상태 ────────────────────────────────────────────────────── */
let selYear   = new Date().getFullYear();
let selMonth  = null;   // null = 전체
let selStatus = '';     // '' = 전체
let openPanel = null;   // 'year' | 'month' | 'status' | null
let currentPage = 1;    // 더보기 페이지 (3건 단위)
let allBillings = [];   // 전체 목록 캐시 (필터 변경 시 교체)

/* ================================================================
   초기화
================================================================ */
document.addEventListener('DOMContentLoaded', () => {
    // 서버에서 SSR된 목록을 JS 캐시로 읽기
    // (필터 변경 전까지는 SSR 목록 그대로 표시, 변경 시 API 재조회)
    document.addEventListener('click', onOutsideClick);
});

/* ================================================================
   필터 패널
================================================================ */
function togglePanel(name) {
    if (openPanel === name) { closeAllPanels(); return; }
    closeAllPanels();
    openPanel = name;

    const panelMap = { year: 'panelYear', month: 'panelMonth', status: 'panelStatus' };
    const btnMap   = { year: 'btnYear',   month: 'btnMonth',   status: 'btnStatus'  };

    if (name === 'year')   buildYearGrid();
    if (name === 'month')  buildMonthGrid();
    if (name === 'status') buildStatusGrid();

    document.getElementById(panelMap[name]).style.display = 'block';
    document.getElementById(btnMap[name]).classList.add('active');
}

function closeAllPanels() {
    ['panelYear','panelMonth','panelStatus'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.style.display = 'none';
    });
    ['btnYear','btnMonth','btnStatus'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.classList.remove('active');
    });
    openPanel = null;
}

function onOutsideClick(e) {
    if (!e.target.closest('.panel-wrap')) closeAllPanels();
}

/* ── 연도 그리드 ── */
function buildYearGrid() {
    const grid = document.getElementById('yearGrid');
    const allChip = `<button class="chip full${selYear === null ? ' selected' : ''}"
        onclick="pickYear(null)">전체</button>`;
    const yearChips = YEARS.map(y =>
        `<button class="chip${y === selYear ? ' selected' : ''}"
            onclick="pickYear(${y})">${y}년</button>`
    ).join('');
    grid.innerHTML = allChip + yearChips;
}

/* ── 월 그리드 ── */
function buildMonthGrid() {
    const grid = document.getElementById('monthGrid');
    const allChip = `<button class="chip full${selMonth === null ? ' selected' : ''}"
        onclick="pickMonth(null)">전체</button>`;
    const monthChips = MONTHS.map((m, i) =>
        `<button class="chip${selMonth === i + 1 ? ' selected' : ''}"
            onclick="pickMonth(${i + 1})">${m}</button>`
    ).join('');
    grid.innerHTML = allChip + monthChips;
}

/* ── 상태 그리드 ── */
function buildStatusGrid() {
    const opts = [
        { val: '',       label: '전체'    },
        { val: 'UNPAID', label: '미납'    },
        { val: 'PAID',   label: '납부완료' },
    ];
    document.getElementById('statusGrid').innerHTML = opts.map(o =>
        `<button class="chip status-chip${selStatus === o.val ? ' selected' : ''}"
            onclick="pickStatus('${o.val}', '${o.label}')">${o.label}</button>`
    ).join('');
}

/* ── 선택 핸들러 ── */
function pickYear(y) {
    selYear = y;
    document.getElementById('lblYear').textContent = y ? y + '년' : '전체';
    closeAllPanels();
    fetchBillings();
}

function pickMonth(m) {
    selMonth = m;
    document.getElementById('lblMonth').textContent = m ? MONTHS[m - 1] : '전체 월';
    closeAllPanels();
    fetchBillings();
}

function pickStatus(val, label) {
    selStatus = val;
    document.getElementById('lblStatus').textContent = label;
    closeAllPanels();
    fetchBillings();
}

/* ================================================================
   API 재조회 — GET /hometalk/api/billing
================================================================ */
async function fetchBillings() {
    currentPage = 1;

    const params = new URLSearchParams();
    if (selYear)   params.set('year',   selYear);
    if (selMonth) params.set('month', `${selYear}-${String(selMonth).padStart(2, '0')}`);
    if (selStatus) params.set('status', selStatus);

    try {
        params.set('householdId', HOUSEHOLD_ID || 1); // TODO: Security 연동 후 교체
        const res = await fetch(`${CONTEXT_PATH}/api/billing/list?${params.toString()}`);
        const data = await res.json();
        allBillings = data.content ?? data;
        renderList();
    } catch (err) {
        console.error('관리비 목록 조회 실패', err);
    }
}

/* ================================================================
   목록 렌더링
================================================================ */
function renderList() {
    const shown   = allBillings.slice(0, currentPage * 3);
    const hasMore = allBillings.length > shown.length;
    const list    = document.getElementById('billingList');
    const moreWrap = document.getElementById('loadMoreWrap');

    if (shown.length === 0) {
        list.innerHTML = '<div class="bp-empty">조회된 관리비 내역이 없습니다.</div>';
        if (moreWrap) moreWrap.style.display = 'none';
        return;
    }

    const today = new Date();

    list.innerHTML = shown.map(b => {
        const isUnpaid = b.status === 'UNPAID';
        const dueDate  = b.dueDate ? new Date(b.dueDate) : null;
        const isOverdue = isUnpaid && dueDate && today > dueDate;

        const dueText = isUnpaid
            ? `납부기한 ${formatDate(b.dueDate)}`
            : '납부 완료';

        return `
        <div class="bp-item${isOverdue ? ' overdue' : ''}">
            <div class="bp-item-left">
                <div class="bp-item-month">${b.billingMonth} 관리비</div>
                <div class="bp-item-due${isUnpaid ? '' : ' paid-text'}">${dueText}</div>
            </div>
            <div class="bp-item-mid">
                <span class="bp-badge ${isUnpaid ? 'unpaid' : 'paid'}">
                    ${isUnpaid ? 'UNPAID' : 'PAID'}
                </span>
                <span class="bp-amount${isUnpaid ? ' unpaid' : ''}">
                    ${formatAmount(b.totalAmount)}
                </span>
            </div>
            <button class="bp-btn-view ${isUnpaid ? 'primary' : 'secondary'}"
                    onclick="openModal(${b.billingId})">
                고지서 보기
            </button>
        </div>`;
    }).join('');

    if (moreWrap) moreWrap.style.display = hasMore ? 'flex' : 'none';
}

/* ── 더보기 ── */
function loadMore() {
    currentPage++;
    renderList();
}

/* ================================================================
   고지서 모달 — GET /hometalk/api/billing/{billingId}/detail
================================================================ */
async function openModal(billingId) {
    try {
        const res  = await fetch(`${CONTEXT_PATH}/api/billing/${billingId}/detail`);
        const data = await res.json();
        renderModal(data);
    } catch (err) {
        console.error('고지서 상세 조회 실패', err);
    }
}

function renderModal(data) {
    const today   = new Date();
    const dueDate = data.dueDate ? new Date(data.dueDate) : null;
    const isUnpaid  = data.status === 'UNPAID';
    const isPastDue = isUnpaid && dueDate && today > dueDate;

    // 헤더
    document.getElementById('modalSubtitle').textContent =
        `${data.billingMonth} · ${data.dongHo}`;

    // 기간
    document.getElementById('modalPeriod').textContent =
        `부과월: ${data.billingMonth} · 납부기한: ${formatDate(data.dueDate)}`;

    // 항목 행
    document.getElementById('modalRows').innerHTML = (data.items || []).map(item =>
        `<div class="bm-row">
            <span>${item.itemName}</span>
            <span>${formatAmount(item.itemAmount)}</span>
        </div>`
    ).join('');

    // 합계
    document.getElementById('modalTotal').textContent = formatAmount(data.totalAmount);

    // 납부 상태 박스
    const box = document.getElementById('modalStatusBox');
    if (data.status === 'PAID') {
        box.className = 'bm-status-box done';
        box.innerHTML = `
            <div class="bm-status-title">납부 완료되었습니다.</div>
            ${data.paidAt
                ? `<div class="bm-status-sub">납부일: ${formatDate(data.paidAt)}</div>`
                : ''}`;
    } else if (isPastDue) {
        box.className = 'bm-status-box past';
        box.innerHTML = `
            <div class="bm-status-title">납부기한이 지났습니다.</div>
            <div class="bm-status-sub">관리사무소에 문의해 주세요. 📞 02-222-2222</div>`;
    } else {
        box.className = 'bm-status-box upcoming';
        box.innerHTML = `
            <div class="bm-status-title">납부기한: ${formatDate(data.dueDate)}</div>
            <div class="bm-status-sub">기한 내 납부 부탁드립니다.</div>`;
    }

    // 모달 표시
    document.getElementById('modalOverlay').style.display = 'block';
    document.getElementById('billingModal').style.display = 'block';
}

function closeModal(e) {
    if (e && e.target !== document.getElementById('modalOverlay')) return;
    closeModalBtn();
}

function closeModalBtn() {
    document.getElementById('modalOverlay').style.display = 'none';
    document.getElementById('billingModal').style.display = 'none';
}

/* ================================================================
   유틸
================================================================ */
function formatAmount(val) {
    if (val == null) return '—';
    return Number(val).toLocaleString() + '원';
}

function formatDate(val) {
    if (!val) return '—';
    // "2026-03-31" → "2026.03.31"
    return String(val).replace(/-/g, '.');
}