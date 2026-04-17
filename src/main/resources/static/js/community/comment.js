console.log("댓글 JS 로드 완료");

function toggleEditForm(commentId) {
    console.log("댓글 수정 모드 전환 (ID: ", commentId);

    const editForm = document.getElementById('edit-form-' + commentId);   // 수정 폼 영역
    const reviewBody = document.getElementById('body-' + commentId);     // 본문 내용 영역
    const metaArea = document.getElementById('meta-' + commentId);       // 닉네임/버튼 영역

    if (editForm.style.display === 'none') {
        // 수정 모드 켜기
        editForm.style.display = 'block';
        reviewBody.style.display = 'none';
        metaArea.style.display = 'none';

        // 커서 자동 포커스 (선택 사항)
        editForm.querySelector('textarea').focus();
    } else {
        // 수정 모드 끄기 (취소)
        editForm.style.display = 'none';
        reviewBody.style.display = '';
        metaArea.style.display = '';
    }
}