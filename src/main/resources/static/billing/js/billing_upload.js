/* ================================================================
   billing_upload.js
   관리자 고지서 업로드 페이지 동작

   엑셀 구조:
   - 파일명 : 2026_03.xlsx (부과월 파싱)
   - 시트   : 동별 분리 (101동, 102동, ...)
   - 컬럼 A : 동/호 (예: 101-101)
   - 컬럼 B : 당월부과액 (= total_amount)
   - 컬럼 C~S: 항목별 금액 (17개)
   ================================================================ */

'use strict';

/* ── 항목 컬럼 목록 (엑셀 실제 컬럼명) ── */
const ITEM_COLS = [
    '일반관리비(과)', '수선유지비(과)', '청소비(과)', '승강기유지비(과)',
    '소방시설유지비(과)', '전기안전점검비(과)', '건물유지관리비(과)',
    '기본난방비(과)', '세대급탕비(과)', '세대전기료(과)', '공동전기료(과)',
    '일반관리비', '화재보험료', '생활폐기물수수료', '장기수선충당금',
    '세대수도료', '공동수도료'
];

const YEARS  = [2026,2025,2024,2023,2022,2021,2020,2019,2018];
const MONTHS = ['1월','2월','3월','4월','5월','6월','7월','8월',
                '9월','10월','11월','12월'];

/* ── 상태 ── */
let validRows    = [];   // 엑셀 파싱 데이터 (업로드 전 검증용)
let dbRows       = [];   // DB 조회 데이터
let mode         = 'db'; // 'db' | 'excel' — 현재 테이블 표시 모드
let billingMonth = null;
let selYear      = new Date().getFullYear();
let selMonth     = null;
let selDong      = null;
let openPanel    = null;
let sortKey      = null;
let sortDir      = 1;
let pendingFile  = null;
let uploadDone   = false;

/* ================================================================
   초기화 — 페이지 진입 시 DB 데이터 자동 조회
================================================================ */
document.addEventListener('DOMContentLoaded', () => {
    initUploadZone();
    fetchDbList(); // 페이지 진입 시 DB 데이터 조회
    document.addEventListener('click', e => {
        if (!e.target.closest('.panel-wrap')) closeAllPanels();
    });
});

/* ================================================================
   DB 데이터 조회 — GET /api/billing/admin/list
================================================================ */
async function fetchDbList() {
    try {
        const params = new URLSearchParams();
        if (selYear)  params.set('year',  selYear);
        if (selMonth) params.set('month', `${selYear}-${String(selMonth).padStart(2,'0')}`);
        if (selDong)  params.set('dong',  selDong);
        params.set('size', 200);

        const res  = await fetch(`${CONTEXT_PATH}/api/billing/admin/list?${params}`);
        const data = await res.json();

        // Page 응답에서 content 추출
        const content = data.content || [];
        dbRows = content.map((item, idx) => ({
            num:           idx + 1,
            household_id:  item.unit || '—',
            dong:          item.unit ? item.unit.split(' ')[0] : '—',
            unit:          item.unit || '—',
            billing_month: item.billingMonth || '—',
            total_amount:  item.totalAmount || 0,
            status:        item.status || '—',
            valid:         '정상',
            upsertType:    null,
            details:       [],
            fromDb:        true,
        }));

        mode = 'db';
        showDbSection();

    } catch (err) {
        console.warn('DB 목록 조회 실패', err);
    }
}

/* ================================================================
   DB 데이터 테이블 표시
================================================================ */
function showDbSection() {
    document.getElementById('filterBar').style.display    = 'flex';
    document.getElementById('tableSection').style.display = 'block';
    buildDongGridFromDb();
    renderDbTable();
}

function renderDbTable() {
    let rows = dbRows.filter(r => {
        if (!r.billing_month || r.billing_month === '—') return true;
        const [y, m] = r.billing_month.split('-').map(Number);
        if (selYear  && y !== selYear)  return false;
        if (selMonth && m !== selMonth) return false;
        if (selDong  && r.dong !== selDong) return false;
        return true;
    });

    rows = rows.map((r, i) => ({ ...r, num: i + 1 }));

    document.getElementById('tableMeta').innerHTML =
        `DB 저장 데이터 · 총 ${rows.length}건`;
    document.getElementById('tableSummary').innerHTML =
        `필터 조건으로 조회된 결과입니다.`;

    // 업로드 확정 버튼 숨기기 (DB 모드에서는 불필요)
    const btnConfirm = document.getElementById('btnConfirm');
    btnConfirm.style.display = 'none';
    document.getElementById('tableBody').innerHTML = rows.length
        ? rows.map(r => `
        <tr>
            <td>${r.num}</td>
            <td>${r.unit}</td>
            <td>${r.household_id}</td>
            <td>${r.billing_month}</td>
            <td>${Number(r.total_amount).toLocaleString()}원</td>
            <td><span class="badge badge-ok">DB저장</span></td>
            <td>—</td>
            <td>—</td>
        </tr>`).join('')
        : `<tr><td colspan="8" style="text-align:center;padding:36px;color:#aaa;">
            조회된 데이터가 없습니다.</td></tr>`;
}

function buildDongGridFromDb() {
    const dongs = [...new Set(dbRows.map(r => r.dong).filter(d => d && d !== '—'))].sort();
    const grid  = document.getElementById('dongGrid');
    grid.innerHTML =
        `<button class="chip full${!selDong ? ' selected' : ''}"
            onclick="pickDong(null)">전체 동</button>`
        + dongs.map(d =>
            `<button class="chip${selDong === d ? ' selected' : ''}"
                onclick="pickDong('${d}')">${d}</button>`
        ).join('');
}

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

/* ================================================================
   파일 처리 → 중복 확인 → 파싱
================================================================ */
async function handleFile(file) {
    pendingFile  = file;

    const nameMatch = file.name.match(/(\d{4})[_-](\d{2})/);
    billingMonth = nameMatch ? `${nameMatch[1]}-${nameMatch[2]}` : null;

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

/* ================================================================
   SheetJS 파싱 — 시트(동) 전체 순회
================================================================ */
function processFile(file) {
    const reader = new FileReader();
    reader.onload = e => {
        const wb      = XLSX.read(e.target.result, { type: 'binary' });
        const allRows = [];

        wb.SheetNames.forEach(sheetName => {
            const ws   = wb.Sheets[sheetName];
            const rows = XLSX.utils.sheet_to_json(ws, { raw: true, defval: '' });
            const dong = sheetName.replace(/동$/, '') + '동';
            rows.forEach(row => allRows.push({ ...row, _dong: dong }));
        });

        runValidation(allRows);
    };
    reader.readAsBinaryString(file);
}

/* ================================================================
   유효성 검사
================================================================ */
function runValidation(rows) {
    uploadDone = false;
    document.getElementById('uploadDoneBanner').style.display = 'none';

    const trimmedRows = rows.map(row => {
        const trimmed = {};
        Object.keys(row).forEach(key => {
            trimmed[key.trim()] = row[key];
        });
        return trimmed;
    });

    validRows = trimmedRows.map((row, idx) => {
        const dongHo      = String(row['동/호'] ?? '').trim();
        const total       = parseFloat(String(row['당월부과액']).replace(/,/g, '')) || 0;
        const dong        = row._dong || '';
        const hoPart      = dongHo.split('-')[1] || '';
        const unit        = dongHo ? `${dong} ${hoPart}호` : dongHo;
        const householdId = dongHo;
        const month       = billingMonth || '';

        let valid = '정상';
        if (!dongHo || !month) valid = '오류';
        else if (total === 0)  valid = '금액 누락';

        const details = ITEM_COLS
            .map(col => {
                const raw = row[col];
                const amt = parseFloat(String(raw).replace(/,/g, '')) || 0;
                return { item_name: col, item_amount: amt };
            })
            .filter(d => d.item_amount > 0);

        return {
            num:           idx + 1,
            household_id:  householdId,
            dong,
            unit,
            billing_month: month,
            total_amount:  total,
            valid,
            upsertType:    valid === '정상' ? 'INSERT' : null,
            details,
            fromDb:        false,
        };
    });

    if (billingMonth) {
        const parts = billingMonth.split('-');
        selYear  = Number(parts[0]);
        selMonth = Number(parts[1]);
        document.getElementById('lblYear').textContent  = selYear + '년';
        document.getElementById('lblMonth').textContent = MONTHS[selMonth - 1];
    }

    mode = 'excel';
    showExcelSection();
}

/* ================================================================
   엑셀 데이터 테이블 표시
================================================================ */
function showExcelSection() {
    document.getElementById('filterBar').style.display    = 'flex';
    document.getElementById('tableSection').style.display = 'block';
    buildDongGrid();

    // 업로드 확정 버튼 다시 표시
    const btnConfirm = document.getElementById('btnConfirm');
    btnConfirm.style.display = '';

    renderTable();
}

function renderTable() {
    if (mode === 'db') { renderDbTable(); return; }

    let rows = validRows.filter(r => {
        const [y, m] = r.billing_month.split('-').map(Number);
        if (selYear  && y !== selYear)  return false;
        if (selMonth && m !== selMonth) return false;
        if (selDong  && r.dong !== selDong) return false;
        return true;
    });

    if (sortKey) {
        rows = [...rows].sort((a, b) => {
            const av = a[sortKey], bv = b[sortKey];
            if (av < bv) return -sortDir;
            if (av > bv) return  sortDir;
            return 0;
        });
    }

    rows = rows.map((r, i) => ({ ...r, num: i + 1 }));

    const totalCount  = validRows.length;
    const errorCount  = validRows.filter(r => r.valid !== '정상').length;
    const normalCount = totalCount - errorCount;
    const insertCount = validRows.filter(r => r.upsertType === 'INSERT').length;
    const updateCount = validRows.filter(r => r.upsertType === 'UPDATE').length;

    document.getElementById('tableMeta').innerHTML =
        `총 ${totalCount}세대 · <span class="meta-error">오류 ${errorCount}건</span>`;
    document.getElementById('tableSummary').innerHTML =
        `정상 ${normalCount}건 (신규 ${insertCount} · 업데이트 ${updateCount}) · 오류 ${errorCount}건`;

    const btnConfirm = document.getElementById('btnConfirm');
    btnConfirm.disabled = errorCount > 0 || uploadDone;
    btnConfirm.classList.toggle('disabled', errorCount > 0 || uploadDone);

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
}

/* ================================================================
   필터 패널
================================================================ */
function buildDongGrid() {
    const dongs = [...new Set(validRows.map(r => r.dong))].sort();
    const grid  = document.getElementById('dongGrid');
    grid.innerHTML =
        `<button class="chip full${!selDong ? ' selected' : ''}"
            onclick="pickDong(null)">전체 동</button>`
        + dongs.map(d =>
            `<button class="chip${selDong === d ? ' selected' : ''}"
                onclick="pickDong('${d}')">${d}</button>`
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

function pickYear(y) {
    selYear = y;
    document.getElementById('lblYear').textContent = y ? y + '년' : '전체';
    closeAllPanels();
    if (mode === 'db') fetchDbList();
    else renderTable();
}
function pickMonth(m) {
    selMonth = m;
    document.getElementById('lblMonth').textContent = m ? MONTHS[m - 1] : '전체 월';
    closeAllPanels();
    if (mode === 'db') fetchDbList();
    else renderTable();
}
function pickDong(d) {
    selDong = d;
    document.getElementById('lblDong').textContent = d ? d : '전체 동';
    closeAllPanels();
    if (mode === 'db') fetchDbList();
    else renderTable();
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
   미리보기 모달 (엑셀 모드 전용)
================================================================ */
function openPreview(hid, month) {
    const row = validRows.find(r => r.household_id === hid && r.billing_month === month);
    if (!row || row.valid !== '정상') return;

    const [y, m] = month.split('-').map(Number);
    const lastDay = new Date(y, m, 0).getDate();

    document.getElementById('modalHeaderTitle').textContent =
        `고지서 미리보기 — ${row.unit} (${month})`;
    document.getElementById('modalPeriod').textContent =
        `부과월: ${month} · 납부기한: ${month.replace('-', '.')}.${String(lastDay).padStart(2,'0')}`;

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
   업로드 확정 — POST /api/billing/admin/upload/confirm
================================================================ */
async function confirmUpload() {
    const errorCount = validRows.filter(r => r.valid !== '정상').length;
    if (errorCount > 0 || uploadDone) return;

    const uploadRows = validRows
        .filter(r => r.valid === '정상')
        .map(r => ({
            householdId:  r.household_id,
            billingMonth: r.billing_month,
            dueDate:      lastDayOfMonth(r.billing_month),
            totalAmount:  r.total_amount,
            items: r.details.map(d => ({
                itemName:   d.item_name,
                itemAmount: d.item_amount
            }))
        }));

    try {
        const res = await fetch(
            `${CONTEXT_PATH}/api/billing/admin/upload/confirm?adminId=1`,
            {
                method:  'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [CSRF_HEADER]: CSRF_TOKEN
                },
                body: JSON.stringify(uploadRows),
            }
        );
        const data = await res.json();

        uploadDone = true;

        const banner = document.getElementById('uploadDoneBanner');
        banner.style.display = 'flex';
        document.getElementById('uploadDoneMsg').textContent =
            `업로드 완료 — 총 ${uploadRows.length}세대 `
            + `(신규 ${data.insertCount}건 · 업데이트 ${data.updateCount}건)`;

        const btn = document.getElementById('btnConfirm');
        btn.textContent = '처리완료';
        btn.disabled    = true;
        btn.classList.add('disabled');

        // 업로드 확정 후 DB 데이터 재조회
        await fetchDbList();

    } catch (err) {
        console.error('업로드 확정 실패', err);
        alert('업로드 중 오류가 발생했습니다. 다시 시도해 주세요.');
    }
}

/* ── 취소 → DB 모드로 복귀 ── */
function cancelUpload() {
    validRows    = [];
    billingMonth = null;
    uploadDone   = false;
    document.getElementById('uploadDoneBanner').style.display = 'none';
    document.getElementById('fileInput').value                = '';
    const btn = document.getElementById('btnConfirm');
    btn.textContent = '업로드 확정 ↑';
    btn.disabled    = false;
    btn.classList.remove('disabled');

    // DB 모드로 복귀
    mode = 'db';
    fetchDbList();
}

/* ================================================================
   유틸
================================================================ */
function lastDayOfMonth(billingMonth) {
    const [y, m] = billingMonth.split('-').map(Number);
    const last   = new Date(y, m, 0).getDate();
    return `${billingMonth}-${String(last).padStart(2, '0')}`;
}