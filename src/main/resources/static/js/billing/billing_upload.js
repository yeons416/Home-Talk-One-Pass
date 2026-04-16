/* ================================================================
   billing_upload.js
   관리자 고지서 업로드 페이지 동작
================================================================ */

'use strict';

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

let validRows    = [];
let dbRows       = [];
let mode         = 'db';
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
   초기화
================================================================ */
document.addEventListener('DOMContentLoaded', () => {
    initUploadZone();
    fetchDbList();
    document.addEventListener('click', e => {
        if (!e.target.closest('.panel-wrap')) closeAllPanels();
    });
});

/* ================================================================
   DB 데이터 조회
================================================================ */
async function fetchDbList() {
    try {
        const params = new URLSearchParams();
        if (selYear)  params.set('year', selYear);
        if (selMonth && selYear) {
            params.set('month', `${selYear}-${String(selMonth).padStart(2,'0')}`);
        } else if (selMonth && !selYear) {
            params.set('month', String(selMonth).padStart(2,'0'));
        }
        if (selDong)  params.set('dong', selDong);
        params.set('size', 200);

        const res  = await fetch(`${CONTEXT_PATH}/api/billing/admin/list?${params}`);
        const data = await res.json();

        const content = data.content || [];
        dbRows = content.map((item, idx) => ({
            num:           idx + 1,
            billingId:     item.billingId,
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

async function openDbPreview(billingId) {
    try {
        const res  = await fetch(`${CONTEXT_PATH}/api/billing/${billingId}/detail`);
        const data = await res.json();

        document.getElementById('modalHeaderTitle').textContent =
            `고지서 미리보기 — ${data.dongHo} (${data.billingMonth})`;
        document.getElementById('modalPeriod').textContent =
            `부과월: ${data.billingMonth} · 납부기한: ${data.dueDate}`;

        document.getElementById('modalRows').innerHTML = (data.items || []).length
            ? (data.items || []).map(d =>
                `<div class="bill-row">
                    <span>${d.itemName}</span>
                    <span>${Number(d.itemAmount).toLocaleString()}원</span>
                </div>`).join('')
            : '<div style="font-size:13px;color:#aaa;text-align:center;padding:16px 0;">항목 정보 없음</div>';

        document.getElementById('modalTotal').textContent =
            Number(data.totalAmount).toLocaleString() + '원';

        document.getElementById('previewOverlay').style.display = 'flex';
    } catch (err) {
        console.error('미리보기 조회 실패', err);
    }
}

/* ================================================================
   DB 테이블 표시
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

    rows = rows.sort((a, b) => a.unit.localeCompare(b.unit));
    rows = rows.map((r, i) => ({ ...r, num: i + 1 }));

    document.getElementById('tableMeta').innerHTML =
        `DB 저장 데이터 · 총 ${rows.length}건`;
    document.getElementById('tableSummary').innerHTML =
        `필터 조건으로 조회된 결과입니다.`;

    document.getElementById('btnConfirm').style.display = 'none';

    const cancelBtn = document.getElementById('btnCancel');
    if (cancelBtn) cancelBtn.style.display = 'none';

    document.getElementById('tableBody').innerHTML = rows.length
        ? rows.map(r => `
        <tr>
            <td>${r.num}</td>
            <td>${r.unit}</td>
            <td>${r.household_id}</td>
            <td>${r.billing_month}</td>
            <td>${Number(r.total_amount).toLocaleString()}원</td>
            <td><span class="badge badge-ok">DB저장</span></td>
            <td><button class="btn-preview" onclick="openDbPreview(${r.billingId})">미리보기 →</button></td>
            <td>—</td>
        </tr>`).join('')
        : `<tr><td colspan="8" style="text-align:center;padding:36px;color:#aaa;">조회된 데이터가 없습니다.</td></tr>`;
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
   파일 처리
================================================================ */
async function handleFile(file) {
    pendingFile = file;

    const nameMatch = file.name.match(/(\d{4})[_-](\d{2})/);
    billingMonth = nameMatch ? `${nameMatch[1]}-${nameMatch[2]}` : null;

    if (billingMonth) {
        try {
            const res  = await fetch(`${CONTEXT_PATH}/api/billing/admin/upload/check?billingMonth=${billingMonth}`);
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
    if (pendingFile) {
        processFile(pendingFile);
        pendingFile = null;
    } else {
        doConfirmUpload();
    }
}

/* ================================================================
   SheetJS 파싱
   ※ reader.onload → async 로 변경 (runValidation await 처리)
================================================================ */
function processFile(file) {
    const reader = new FileReader();

    // ★ async 추가: runValidation에서 await fetchAndSetUpsertTypes() 호출을 위해 필요
    reader.onload = async e => {
        const wb      = XLSX.read(e.target.result, { type: 'binary' });
        const allRows = [];

        wb.SheetNames.forEach(sheetName => {
            const ws   = wb.Sheets[sheetName];
            const rows = XLSX.utils.sheet_to_json(ws, { raw: true, defval: '' });
            const dong = sheetName.replace(/동$/, '') + '동';
            rows.forEach(row => allRows.push({ ...row, _dong: dong }));
        });

        await runValidation(allRows);
    };
    reader.readAsBinaryString(file);
}

/* ================================================================
   유효성 검사
   ★ async 추가: DB 조회로 INSERT/UPDATE 구분 처리
================================================================ */
async function runValidation(rows) {
    uploadDone = false;
    document.getElementById('uploadDoneBanner').style.display = 'none';

    const trimmedRows = rows.map(row => {
        const trimmed = {};
        Object.keys(row).forEach(key => { trimmed[key.trim()] = row[key]; });
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

        const details = ITEM_COLS
            .map(col => {
                const raw = row[col];
                const amt = parseFloat(String(raw).replace(/,/g, '')) || 0;
                return { item_name: col, item_amount: amt };
            })
            .filter(d => d.item_amount > 0);

        let valid = '정상';
        if (!dongHo || !month)         valid = '오류';
        else if (total === 0)          valid = '금액 누락';
        else if (details.length === 0) valid = '항목 누락';

        return {
            num:           idx + 1,
            household_id:  householdId,
            dong,
            unit,
            billing_month: month,
            total_amount:  total,
            valid,
            upsertType:    valid === '정상' ? 'INSERT' : null,  // 일단 INSERT로 초기화
            details,
            fromDb:        false,
        };
    });

    // ★ 핵심 수정: DB 조회로 INSERT / UPDATE 정확하게 구분
    await fetchAndSetUpsertTypes();

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
   ★ 신규 함수: DB 조회로 INSERT/UPDATE 구분
   - 해당 billingMonth의 DB 저장 데이터를 가져와 unit 매칭
   - DB에 이미 있는 세대 → UPDATE, 없는 세대 → INSERT
   - 오류 행(valid !== '정상')은 null 유지
================================================================ */
async function fetchAndSetUpsertTypes() {
    if (!billingMonth) return;

    try {
        const res = await fetch(
            `${CONTEXT_PATH}/api/billing/admin/list?month=${billingMonth}&size=500`
        );
        if (!res.ok) return;

        const data = await res.json();

        // DB에 이미 존재하는 세대 unit 집합 (예: "101동 1204호")
        const existingUnits = new Set(
            (data.content || [])
                .map(item => item.unit)
                .filter(Boolean)
        );

        validRows = validRows.map(r => ({
            ...r,
            upsertType: r.valid !== '정상'
                ? null                                        // 오류 행: 표시 없음
                : existingUnits.has(r.unit) ? 'UPDATE'        // DB 존재: 기존 UPDATE
                                            : 'INSERT'        // DB 없음: 신규 INSERT
        }));

    } catch (err) {
        // 조회 실패 시 기본값 INSERT 유지 (runValidation에서 초기화한 값)
        console.warn('UPSERT 타입 조회 실패, 기본 INSERT로 처리', err);
    }
}

/* ================================================================
   엑셀 테이블 표시
================================================================ */
function showExcelSection() {
    document.getElementById('filterBar').style.display    = 'flex';
    document.getElementById('tableSection').style.display = 'block';
    buildDongGrid();

    const btnConfirm = document.getElementById('btnConfirm');
    btnConfirm.style.display = '';

    const cancelBtn = document.getElementById('btnCancel');
    if (cancelBtn) cancelBtn.style.display = '';

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
            const av = a[sortKey] ?? '';
            const bv = b[sortKey] ?? '';
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
        `총 ${totalCount}세대 (필터: ${rows.length}건) · <span class="meta-error">오류 ${errorCount}건</span>`;
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
    selDong = null;
    document.getElementById('lblYear').textContent = y ? y + '년' : '전체';
    document.getElementById('lblDong').textContent = '전체 동';
    closeAllPanels();
    if (mode === 'db') fetchDbList();
    else { buildDongGrid(); renderTable(); }
}

function pickMonth(m) {
    selMonth = m;
    selDong  = null;
    document.getElementById('lblMonth').textContent = m ? MONTHS[m - 1] : '전체 월';
    document.getElementById('lblDong').textContent  = '전체 동';
    closeAllPanels();
    if (mode === 'db') fetchDbList();
    else { buildDongGrid(); renderTable(); }
}

function pickDong(d) {
    selDong = d;
    document.getElementById('lblDong').textContent = d ? d : '전체 동';
    closeAllPanels();
    if (mode === 'db') { buildDongGridFromDb(); renderDbTable(); }
    else               { buildDongGrid();        renderTable();   }
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
   미리보기 모달 (엑셀 모드 — 로컬 데이터)
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
   업로드 확정 — 확인 팝업 표시
================================================================ */
async function confirmUpload() {
    const errorCount = validRows.filter(r => r.valid !== '정상').length;
    if (errorCount > 0 || uploadDone) return;

    pendingFile = null;  // null이면 confirmDup()에서 doConfirmUpload() 호출
    document.getElementById('dupOverlay').style.display = 'flex';
}

/* ================================================================
   업로드 확정 — 실제 API 호출
================================================================ */
async function doConfirmUpload() {
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
                    [CSRF_HEADER]:  CSRF_TOKEN
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

        const cancelBtn = document.getElementById('btnCancel');
        if (cancelBtn) cancelBtn.style.display = 'none';

        document.getElementById('fileInput').value = '';

        // 업로드 완료 후 upsertType을 모두 UPDATE로 갱신
        mode      = 'excel';
        validRows = validRows.map(r => ({
            ...r,
            upsertType: r.valid === '정상' ? 'UPDATE' : null
        }));
        renderTable();

    } catch (err) {
        console.error('업로드 확정 실패', err);
        alert('업로드 중 오류가 발생했습니다. 다시 시도해 주세요.');
    }
}

/* ================================================================
   취소 → DB 모드로 복귀
================================================================ */
function cancelUpload() {
    validRows    = [];
    billingMonth = null;
    uploadDone   = false;
    sortKey      = null;
    sortDir      = 1;

    document.getElementById('uploadDoneBanner').style.display = 'none';
    document.getElementById('fileInput').value                = '';

    const btn = document.getElementById('btnConfirm');
    btn.textContent = '업로드 확정 ↑';
    btn.disabled    = false;
    btn.classList.remove('disabled');

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