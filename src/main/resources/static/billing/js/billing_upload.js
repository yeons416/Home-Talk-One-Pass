// 파일 선택 시 GET /api/billing/admin/upload/check
// 중복 확인, SheetJS 파싱 + 유효성 검사,
// 확정 시 POST /api/billing/admin/upload multipart 전송

/* ================================================================
   billing_upload.js
   관리자 고지서 업로드 페이지 동작
   - 드래그&드롭 / 클릭 파일 업로드
   - 중복 부과월 확인 팝업 (API)
   - SheetJS 엑셀 파싱 + 유효성 검사
   - 테이블 렌더링 / 필터 / 정렬
   - 고지서 미리보기 모달
   - 업로드 확정 (API POST)
   ================================================================ */

'use strict';

/* ── 상수 ────────────────────────────────────────────────────── */
const YEARS     = [2026,2025,2024,2023,2022,2021,2020,2019,2018];
const MONTHS    = ['1월','2월','3월','4월','5월','6월','7월','8월',
    '9월','10월','11월','12월'];
const ITEM_COLS = ['일반관리비','청소비','전기료','수도료','난방비'];

/* ── 상태 ────────────────────────────────────────────────────── */
let validRows   = [];
let selYear     = new Date().getFullYear();
let selMonth    = null;
let selDong     = null;
let openPanel   = null;
let sortKey     = null;
let sortDir     = 1;
let pendingFile = null;
let uploadDone  = false;

/* ================================================================
   초기화
================================================================ */
document.addEventListener('DOMContentLoaded', () => {
    initUploadZone();
    document.addEventListener('click', e => {
        if (!e.target.closest('.panel-wrap')) closeAllPanels();
    });
});

/* ================================================================
   업로드 존
================================================================ */
function initUploadZone() {
    const zone      = document.getElementById('uploadZone');
    const fileInput = document.getElementById('fileInput');
    if (!zone || !fileInput) return;

    zone.addEventListener('click',     () => fileInput.click());
    zone.addEventListener('dragover',  e  => { e.preventDefault(); zone.classList.add('drag-over'); });
    zone.addEventListener('dragleave', () => zone.classList.remove('drag-over'));
    zone.addEventListener('drop', e => {
        e.preventDefault();
        zone.classList.remove('drag-over');
        if (e.dataTransfer.files[0]) handleFile(e.dataTransfer.files[0]);
    });
    fileInput.addEventListener('change', e => {
        if (e.target.files[0]) handleFile(e.target.files[0]);
    });
}

/* ── 파일 선택 → 중복 확인 → 파싱 ── */
async function handleFile(file) {
    pendingFile = file;

    // 파일명에서 billing_month 추출 시도 (예: 2026-03_관리비.xlsx)
    const monthMatch = file.name.match(/(\d{4}-\d{2})/);
    const billingMonth = monthMatch ? monthMatch[1] : null;

    // API로 중복 확인
    if (billingMonth) {
        try {
            const res  = await fetch(
                `${CONTEXT_PATH}/api/billing/admin/upload/check?billingMonth=${billingMonth}`
            );
            const data = await res.json();
            if (data.exists) {
                document.getElementById('dupOverlay').style.display = 'flex';
                return;
            }
        } catch (err) {
            console.warn('중복 확인 API 실패, 계속 진행', err);
        }
    }

    processFile(file);
}

function cancelDup() {
    document.getElementById('dupOverlay').style.display = 'none';
    pendingFile = null;
    document.getElementById('fileInput').value = '';
}

function confirmDup() {
    document.getElementById('dupOverlay').style.display = 'none';
    processFile(pendingFile);
}

/* ── SheetJS 파싱 ── */
function processFile(file) {
    const reader = new FileReader();
    reader.onload = e => {
        const wb   = XLSX.read(e.target.result, { type: 'binary' });
        const ws   = wb.Sheets[wb.SheetNames[0]];
        const rows = XLSX.utils.sheet_to_json(ws, { defval: '' });
        runValidation(rows);
    };
    reader.readAsBinaryString(file);
}

/* ================================================================
   유효성 검사
================================================================ */
function runValidation(rows) {
    uploadDone = false;
    document.getElementById('uploadDoneBanner').style.display = 'none';

    validRows = rows.map((row, idx) => {
        const hid   = String(row.household_id  || '').trim();
        const month = String(row.billing_month || '').trim();
        const total = Number(row.total_amount) || 0;

        // 검증
        let valid       = '정상';
        let errorReason = '';
        if (!hid || !month)    { valid = '오류';     errorReason = '필수값 누락'; }
        else if (total === 0)  { valid = '금액 누락'; errorReason = '금액 누락';  }

        // 항목 파싱
        const details = ITEM_COLS
            .map(col => ({ item_name: col, item_amount: Number(row[col]) || 0 }))
            .filter(d => d.item_amount > 0);

        // UPSERT 타입은 확정 시 서버에서 판별 — 여기서는 표시용으로만 사용
        // 실제 INSERT/UPDATE 판별은 BillingUploadService에서 처리
        const upsertType = valid !== '정상' ? null : 'INSERT'; // 기본 INSERT, 서버에서 확정

        return {
            num:           idx + 1,
            household_id:  hid,
            billing_month: month,
            total_amount:  total,
            unit:          hid,   // TODO: 서버에서 세대 정보 매핑 후 교체
            valid,
            errorReason,
            upsertType,
            details,
        };
    });

    // 자동 필터: 첫 행의 billing_month 기준
    if (validRows.length > 0) {
        const parts = validRows[0].billing_month.split('-');
        if (parts.length >= 2) {
            selYear  = Number(parts[0]);
            selMonth = Number(parts[1]);
            document.getElementById('lblYear').textContent  = selYear + '년';
            document.getElementById('lblMonth').textContent = MONTHS[selMonth - 1];
        }
    }

    showTableSection();
}

/* ================================================================
   테이블 영역 표시 + 렌더링
================================================================ */
function showTableSection() {
    document.getElementById('filterBar').style.display    = 'flex';
    document.getElementById('tableSection').style.display = 'block';
    buildDongGrid();
    renderTable();
}

function renderTable() {
    let rows = validRows.filter(r => {
        const [y, m] = r.billing_month.split('-').map(Number);
        if (selYear  && y !== selYear)  return false;
        if (selMonth && m !== selMonth) return false;
        if (selDong  && r.unit.split('-')[0] !== selDong) return false;
        return true;
    });

    // 정렬
    if (sortKey) {
        rows = [...rows].sort((a, b) => {
            const av = a[sortKey], bv = b[sortKey];
            if (av < bv) return -sortDir;
            if (av > bv) return  sortDir;
            return 0;
        });
    }

    // num 재계산
    rows = rows.map((r, i) => ({ ...r, num: i + 1 }));

    // 메타 정보
    const totalCount  = validRows.length;
    const errorCount  = validRows.filter(r => r.valid !== '정상').length;
    const normalCount = totalCount - errorCount;
    const insertCount = validRows.filter(r => r.upsertType === 'INSERT').length;
    const updateCount = validRows.filter(r => r.upsertType === 'UPDATE').length;

    document.getElementById('tableMeta').innerHTML =
        `총 ${totalCount}세대 · <span class="meta-error">오류 ${errorCount}건</span>`;
    document.getElementById('tableSummary').innerHTML =
        `정상 ${normalCount}건 (신규 ${insertCount} · 업데이트 ${updateCount}) · 오류 ${errorCount}건`;

    // 확정 버튼 활성화 여부
    const btnConfirm = document.getElementById('btnConfirm');
    btnConfirm.disabled = errorCount > 0 || uploadDone;
    btnConfirm.classList.toggle('disabled', errorCount > 0 || uploadDone);

    // tbody 렌더
    document.getElementById('tableBody').innerHTML = rows.map(r => {
        const isError = r.valid !== '정상';

        const validCell = isError
            ? `<span class="badge badge-error">${r.valid}</span>`
            : `<span class="badge badge-ok">정상</span>`;

        const previewCell = isError
            ? `<button class="btn-preview disabled-preview" disabled>미리보기 불가</button>`
            : `<button class="btn-preview"
                onclick="openPreview('${r.household_id}','${r.billing_month}')">
                미리보기 →</button>`;

        const upsertCell = !r.upsertType ? '—'
            : r.upsertType === 'INSERT'
                ? `<span class="badge badge-insert">신규 INSERT</span>`
                : `<span class="badge badge-update">기존 UPDATE</span>`;

        return `
        <tr class="${isError ? 'row-error' : ''}">
            <td>${r.num}</td>
            <td>${r.unit}</td>
            <td>${r.household_id}</td>
            <td>${r.billing_month}</td>
            <td>${isError ? '—' : Number(r.total_amount).toLocaleString() + '원'}</td>
            <td>${validCell}</td>
            <td>${previewCell}</td>
            <td>${upsertCell}</td>
        </tr>`;
    }).join('');

    // 전체 행이 많을 때 안내
    if (totalCount > rows.length) {
        document.getElementById('tableBody').innerHTML +=
            `<tr><td colspan="8" style="text-align:center;color:#aaa;
            font-size:12px;padding:12px 0;">
            · · · 총 ${totalCount}세대 · · ·</td></tr>`;
    }
}

/* ================================================================
   필터 패널
================================================================ */
function buildDongGrid() {
    const dongs = [...new Set(validRows.map(r => r.unit.split('-')[0]))].sort();
    const grid  = document.getElementById('dongGrid');
    grid.innerHTML =
        `<button class="chip full${!selDong ? ' selected' : ''}"
            onclick="pickDong(null)">전체 동</button>`
        + dongs.map(d =>
            `<button class="chip${selDong === d ? ' selected' : ''}"
                onclick="pickDong('${d}')">${d}동</button>`
        ).join('');
}

function buildYearGrid() {
    document.getElementById('yearGrid').innerHTML =
        `<button class="chip full${selYear === null ? ' selected' : ''}"
            onclick="pickYear(null)">전체</button>`
        + YEARS.map(y =>
            `<button class="chip${y === selYear ? ' selected' : ''}"
                onclick="pickYear(${y})">${y}년</button>`
        ).join('');
}

function buildMonthGrid() {
    document.getElementById('monthGrid').innerHTML =
        `<button class="chip full${selMonth === null ? ' selected' : ''}"
            onclick="pickMonth(null)">전체</button>`
        + MONTHS.map((m, i) =>
            `<button class="chip${selMonth === i + 1 ? ' selected' : ''}"
                onclick="pickMonth(${i + 1})">${m}</button>`
        ).join('');
}

function togglePanel(name) {
    if (openPanel === name) { closeAllPanels(); return; }
    closeAllPanels();
    openPanel = name;

    if (name === 'year')  buildYearGrid();
    if (name === 'month') buildMonthGrid();

    const panelMap = { year:'panelYear', month:'panelMonth', dong:'panelDong' };
    const btnMap   = { year:'btnYear',   month:'btnMonth',   dong:'btnDong'  };
    document.getElementById(panelMap[name]).style.display = 'block';
    document.getElementById(btnMap[name]).classList.add('active');
}

function closeAllPanels() {
    ['panelYear','panelMonth','panelDong'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.style.display = 'none';
    });
    ['btnYear','btnMonth','btnDong'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.classList.remove('active');
    });
    openPanel = null;
}

function pickYear(y)  {
    selYear = y;
    document.getElementById('lblYear').textContent  = y ? y + '년' : '전체';
    closeAllPanels(); renderTable();
}
function pickMonth(m) {
    selMonth = m;
    document.getElementById('lblMonth').textContent = m ? MONTHS[m - 1] : '전체 월';
    closeAllPanels(); renderTable();
}
function pickDong(d)  {
    selDong = d;
    document.getElementById('lblDong').textContent  = d ? d + '동' : '전체 동';
    closeAllPanels(); renderTable();
}

/* ================================================================
   정렬
================================================================ */
function sortBy(key) {
    if (sortKey === key) sortDir *= -1;
    else { sortKey = key; sortDir = 1; }
    renderTable();
}

/* ================================================================
   미리보기 모달
   - 업로드 전: 파싱된 로컬 데이터로 렌더
   - 업로드 후: billingId 기반 API 호출 (TODO)
================================================================ */
function openPreview(hid, month) {
    const row = validRows.find(r => r.household_id === hid && r.billing_month === month);
    if (!row || row.valid !== '정상') return;

    const [y, m] = month.split('-').map(Number);
    const lastDay = new Date(y, m, 0).getDate();

    document.getElementById('modalHeaderTitle').textContent =
        `고지서 미리보기 — ${row.unit} (${month})`;
    document.getElementById('modalPeriod').textContent =
        `부과월: ${month} · 납부기한: ${month.replace('-', '.')}.${lastDay}`;

    document.getElementById('modalRows').innerHTML = row.details.length
        ? row.details.map(d =>
            `<div class="bill-row">
                <span>${d.item_name}</span>
                <span>${Number(d.item_amount).toLocaleString()}원</span>
            </div>`).join('')
        : '<div style="font-size:13px;color:#aaa;text-align:center;padding:16px 0;">항목 정보 없음</div>';

    document.getElementById('modalTotal').textContent =
        Number(row.total_amount).toLocaleString() + '원';

    document.getElementById('previewOverlay').style.display = 'flex';
}

function closePreview(e) {
    if (e && e.target !== document.getElementById('previewOverlay')) return;
    closePreviewBtn();
}
function closePreviewBtn() {
    document.getElementById('previewOverlay').style.display = 'none';
}

/* ================================================================
   업로드 확정 — POST /hometop/api/billing/admin/upload
================================================================ */
async function confirmUpload() {
    const errorCount = validRows.filter(r => r.valid !== '정상').length;
    if (errorCount > 0 || uploadDone) return;

    const fileInput = document.getElementById('fileInput');
    if (!fileInput.files[0]) {
        alert('업로드할 파일이 없습니다.');
        return;
    }

    const formData = new FormData();
    formData.append('file', fileInput.files[0]);

    try {
        const res  = await fetch(`${CONTEXT_PATH}/api/billing/admin/upload`, {
            method: 'POST',
            headers: { [CSRF_HEADER]: CSRF_TOKEN },
            body:    formData,
        });
        const data = await res.json();

        uploadDone = true;

        // 완료 배너
        const banner = document.getElementById('uploadDoneBanner');
        banner.style.display = 'flex';
        document.getElementById('uploadDoneMsg').textContent =
            `업로드 완료 — 총 ${validRows.length}세대 `
            + `(신규 ${data.insertCount}건 · 업데이트 ${data.updateCount}건)`
            + (data.errorCount > 0 ? ` · 오류 ${data.errorCount}건` : '');

        // 확정 버튼 비활성화
        const btn = document.getElementById('btnConfirm');
        btn.textContent = '처리완료';
        btn.disabled    = true;
        btn.classList.add('disabled');

    } catch (err) {
        console.error('업로드 확정 실패', err);
        alert('업로드 중 오류가 발생했습니다. 다시 시도해 주세요.');
    }
}

/* ── 취소 ── */
function cancelUpload() {
    validRows = [];
    uploadDone = false;
    document.getElementById('filterBar').style.display       = 'none';
    document.getElementById('tableSection').style.display    = 'none';
    document.getElementById('uploadDoneBanner').style.display = 'none';
    document.getElementById('tableBody').innerHTML           = '';
    document.getElementById('fileInput').value               = '';

    // 확정 버튼 초기화
    const btn = document.getElementById('btnConfirm');
    btn.textContent = '업로드 확정 ↑';
    btn.disabled    = false;
    btn.classList.remove('disabled');
}