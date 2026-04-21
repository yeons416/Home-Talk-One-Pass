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
    household_id:  item.householdId || '—',
    dong:          item.dong || '—',
    unit:          (item.dong && item.ho) ? `${item.dong} ${item.ho}` : '—',
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
   DB 테이블 표시 (UPSERT 컬럼 숨김)
================================================================ */
function showDbSection() {
    document.getElementById('filterBar').style.display       = 'flex';
    document.getElementById('tableSection').style.display    = 'block';
    document.getElementById('thUpsert').style.display        = 'none';
    document.getElementById('btnDeleteMonth').style.display  = '';
    document.getElementById('btnConfirm').style.display      = 'none';
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
        </tr>`).join('')
        : `<tr><td colspan="7" style="text-align:center;padding:36px;color:#aaa;">조회된 데이터가 없습니다.</td></tr>`;
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

    zone.addEventListener('click', () => fileInput.click());
    zone.addEventListener('dragover',  e  => { e.preventDefault(); zone.classList.add('drag-over'); });
    zone.addEventListener('dragleave', () => zone.classList.remove('drag-over'));
    zone.addEventListener('drop', e => {
        e.preventDefault();
        zone.classList.remove('drag-over');
        if (e.dataTransfer.files[0]) handleFile(e.dataTransfer.files[0]);
    });
    fileInput.addEventListener('change', e => {
        if (e.target.files[0]) handleFile(e.target.files[0]);
        e.target.value = '';
    });
}

/* ================================================================
   파일 처리
================================================================ */
function handleFile(file) {
    // 기존 데이터가 있는 경우 안전장치
    if (validRows.length > 0) {
        if (!confirm("현재 검증 중인 목록을 지우고 새로운 파일을 업로드하시겠습니까?")) {
            return;
        }
        validRows = []; // 초기화
    }

    const nameMatch = file.name.match(/(\d{4})[_-](\d{2})/);
    billingMonth = nameMatch ? `${nameMatch[1]}-${nameMatch[2]}` : null;
    processFile(file);
}

function processFile(file) {
    const reader = new FileReader();
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
================================================================ */
async function runValidation(rows) {
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
            upsertType:    null,   // ← fetchAndSetUpsertTypes에서 결정
            details,
            fromDb:        false,
        };
    });

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
   UPSERT 타입 확정 (실제 값 비교)
   - DB에 없음       → INSERT
   - DB에 있고 다름  → UPDATE
   - DB에 있고 같음  → SKIP (변경없음, 서버 미전송)
================================================================ */
/* UPSERT 타입 확정
   - DB에 데이터 없음: INSERT
   - DB에 데이터 있음 + 금액/항목 다름: UPDATE
   - DB에 데이터 있음 + 금액/항목 동일: SKIP (변경없음)
*/
async function fetchAndSetUpsertTypes() {
    if (!billingMonth) return;

    const dbBillingsById = new Map();

    try {
        // 해당 월의 데이터를 가져옴 (전체 삭제 전이라면 데이터가 존재함)
        const listRes = await fetch(`${CONTEXT_PATH}/api/billing/admin/list?month=${billingMonth}&size=500`);
        if (listRes.ok) {
            const listData = await listRes.json();
            const dbItems  = listData.content || [];

            await Promise.all(dbItems.map(async item => {
                const key = item.householdId; // DTO에서 받은 "101-102" 형태
                if (!key) return;

                // 상세 항목 비교를 위해 상세 API 호출
                const detRes = await fetch(`${CONTEXT_PATH}/api/billing/${item.billingId}/detail`);
                if (detRes.ok) {
                    const detail = await detRes.json();
                    dbBillingsById.set(key, {
                        totalAmount: Number(item.totalAmount),
                        items: detail.items // [{itemName, itemAmount}, ...]
                    });
                }
            }));
        }
    } catch (err) { console.warn('비교 대상 조회 실패', err); }

    validRows = validRows.map(r => {
        if (r.valid !== '정상') return { ...r, upsertType: null };

        const dbRow = dbBillingsById.get(r.household_id);

        // 1. DB에 없으면 신규
        if (!dbRow) return { ...r, upsertType: 'INSERT' };

        // 2. DB에 있으면 데이터 비교
        const isSame = isSameBillingData(
            { totalAmount: r.total_amount, items: r.details.map(d => ({
                itemName: d.item_name, itemAmount: d.item_amount
            })) },
            dbRow
        );

        // 같으면 SKIP(변경없음), 다르면 UPDATE(수정됨)
        return { ...r, upsertType: isSame ? 'SKIP' : 'UPDATE' };
    });
}

/* ================================================================
   두 고지서 데이터가 실질적으로 같은지 비교
   - totalAmount 다르면 다름
   - items 개수 다르면 다름
   - itemName별 itemAmount 다르면 다름
================================================================ */
function isSameBillingData(excel, db) {
    if (Number(excel.totalAmount) !== Number(db.totalAmount)) return false;
    if (excel.items.length !== db.items.length) return false;

    const dbMap = new Map(db.items.map(i => [i.itemName, Number(i.itemAmount)]));
    for (const it of excel.items) {
        if (dbMap.get(it.itemName) !== Number(it.itemAmount)) return false;
    }
    return true;
}

/* ================================================================
   엑셀 테이블 표시
================================================================ */
function showExcelSection() {
    document.getElementById('filterBar').style.display       = 'flex';
    document.getElementById('tableSection').style.display    = 'block';
    document.getElementById('thUpsert').style.display        = '';
    document.getElementById('btnDeleteMonth').style.display  = 'none';
    document.getElementById('btnConfirm').style.display      = '';
    buildDongGrid();
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
    const insertCount = validRows.filter(r => r.upsertType === 'INSERT').length;
    const updateCount = validRows.filter(r => r.upsertType === 'UPDATE').length;
    const skipCount   = validRows.filter(r => r.upsertType === 'SKIP').length;

    document.getElementById('tableMeta').innerHTML =
        `총 ${totalCount}세대 (필터: ${rows.length}건) · <span class="meta-error">오류 ${errorCount}건</span>`;
    document.getElementById('tableSummary').innerHTML =
        `신규 ${insertCount} · 변경 ${updateCount} · 변경없음 ${skipCount} · 오류 ${errorCount}`;

    // 저장할 것(INSERT + UPDATE)이 0건이면 확정 버튼 비활성화
    const savableCount = insertCount + updateCount;
    const btnConfirm   = document.getElementById('btnConfirm');
    const disabled     = errorCount > 0 || savableCount === 0;
    btnConfirm.disabled = disabled;
    btnConfirm.classList.toggle('disabled', disabled);

    document.getElementById('tableBody').innerHTML = rows.map(r => {
        const isError = r.valid !== '정상';
        const validCell = isError
            ? `<span class="badge badge-error">${r.valid}</span>`
            : `<span class="badge badge-ok">정상</span>`;
        const previewCell = isError
            ? `<button class="btn-preview disabled-preview" disabled>미리보기 불가</button>`
            : `<button class="btn-preview" onclick="openPreview('${r.household_id}','${r.billing_month}')">미리보기 →</button>`;

        let upsertCell;
        if (!r.upsertType)                      upsertCell = '—';
        else if (r.upsertType === 'INSERT')     upsertCell = `<span class="badge badge-insert">신규 INSERT</span>`;
        else if (r.upsertType === 'UPDATE')     upsertCell = `<span class="badge badge-update">기존 UPDATE</span>`;
        else /* SKIP */                         upsertCell = `<span class="badge badge-skip">변경없음</span>`;

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

function sortBy(key) {
    if (sortKey === key) sortDir *= -1;
    else { sortKey = key; sortDir = 1; }
    renderTable();
}

/* ================================================================
   미리보기 모달
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
   업로드 확정 → 팝업
================================================================ */
function confirmUpload() {
    const errorCount  = validRows.filter(r => r.valid !== '정상').length;
    const insertCount = validRows.filter(r => r.upsertType === 'INSERT').length;
    const updateCount = validRows.filter(r => r.upsertType === 'UPDATE').length;
    const skipCount   = validRows.filter(r => r.upsertType === 'SKIP').length;
    const savableCount = insertCount + updateCount;

    if (errorCount > 0) return;

    // 저장할 게 0건 → 팝업 없이 알림만
    if (savableCount === 0) {
        alert(`변경된 내용이 없습니다.\n(${skipCount}세대 모두 DB와 동일)`);
        return;
    }

    const hasUpdate = updateCount > 0;

    document.getElementById('confirmTitle').textContent =
        hasUpdate ? '업로드 확정 (변경 포함)' : '업로드 확정';
    document.getElementById('confirmMsg').textContent =
        `${billingMonth} 관리비 고지서를 저장하시겠습니까?`;
    document.getElementById('confirmMsgWarn').textContent = hasUpdate
        ? `신규 ${insertCount}건 · 변경 ${updateCount}건 · 변경없음 ${skipCount}건 (생략)`
        : `총 ${insertCount}세대 신규 등록`;

    document.getElementById('confirmOverlay').style.display = 'flex';
}

function cancelConfirm() {
    document.getElementById('confirmOverlay').style.display = 'none';
}

/* ================================================================
   업로드 확정 API 호출 → DB 모드 즉시 전환
   ★ SKIP 행은 서버로 안 보냄 (DB 쿼리 절약)
================================================================ */
async function doConfirmUpload() {
    const confirmBtn = document.querySelector('#confirmOverlay .btn-point');
    if (confirmBtn) confirmBtn.disabled = true;

    const uploadRows = validRows
        .filter(r => r.valid === '정상' && (r.upsertType === 'INSERT' || r.upsertType === 'UPDATE'))
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
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [CSRF_HEADER]: CSRF_TOKEN
                },
                body: JSON.stringify(uploadRows),
            }
        );
        if (!res.ok) throw new Error('업로드 실패');
        const data = await res.json();

        document.getElementById('confirmOverlay').style.display = 'none';

        showToast(`업로드 완료 — 신규 ${data.insertCount}건 · 변경 ${data.updateCount}건`);

        validRows = [];
        sortKey   = null;
        sortDir   = 1;
        mode      = 'db';
        await fetchDbList();

    } catch (err) {
        console.error('업로드 확정 실패', err);
        alert('업로드 중 오류가 발생했습니다. 다시 시도해 주세요.');
    } finally {
        if (confirmBtn) confirmBtn.disabled = false;
    }
}

/* ================================================================
   월별 전체 삭제
================================================================ */
async function deleteCurrentMonth() {
    const monthToDelete = selMonth && selYear
        ? `${selYear}-${String(selMonth).padStart(2,'0')}`
        : null;

    if (!monthToDelete) {
        alert('삭제할 월을 먼저 필터로 선택해 주세요.\n(연도 + 월 둘 다 선택 필요)');
        return;
    }

    const rowCount = dbRows.filter(r => r.billing_month === monthToDelete).length;
    if (rowCount === 0) {
        alert(`${monthToDelete} 데이터가 없습니다.`);
        return;
    }

    const confirmed = confirm(
        `정말 ${monthToDelete} 관리비 데이터를 전체 삭제하시겠습니까?\n\n`
        + `총 ${rowCount}세대의 고지서가 삭제됩니다.\n`
        + `이 작업은 되돌릴 수 없습니다.`
    );
    if (!confirmed) return;

    try {
        const res = await fetch(
            `${CONTEXT_PATH}/api/billing/admin/month/${monthToDelete}?adminId=1`,
            {
                method: 'DELETE',
                headers: { [CSRF_HEADER]: CSRF_TOKEN }
            }
        );
        if (!res.ok) throw new Error('삭제 실패');
        const data = await res.json();

        alert(`${monthToDelete} 데이터 ${data.deleted}건이 삭제되었습니다.`);
        fetchDbList();
    } catch (err) {
        console.error('월별 삭제 실패', err);
        alert('삭제 중 오류가 발생했습니다.');
    }
}

/* ================================================================
   토스트
================================================================ */
function showToast(msg) {
    const toast = document.createElement('div');
    toast.textContent = msg;
    toast.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: #2c8a3e;
        color: white;
        padding: 12px 20px;
        border-radius: 6px;
        box-shadow: 0 2px 8px rgba(0,0,0,0.15);
        z-index: 10000;
        font-size: 13px;
    `;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 3000);
}

/* ================================================================
   유틸
================================================================ */
function lastDayOfMonth(billingMonth) {
    const [y, m] = billingMonth.split('-').map(Number);
    const last   = new Date(y, m, 0).getDate();
    return `${billingMonth}-${String(last).padStart(2, '0')}`;
}