// Hỏi xác nhận trước khi thực hiện thao tác có thuộc tính data-confirm
document.addEventListener('click', function (e) {
    var el = e.target.closest('[data-confirm]');
    if (el && el.dataset.confirm && !window.confirm(el.dataset.confirm)) {
        e.preventDefault();
    }
});