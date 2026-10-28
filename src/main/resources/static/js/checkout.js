document.addEventListener('DOMContentLoaded', function () {
    var radios = document.querySelectorAll('.addr-radio');
    var box = document.getElementById('new-address');
    if (!box) return;

    function update() {
        var isNew = false;
        radios.forEach(function (r) {
            if (r.checked && r.value === '') isNew = true;
        });
        box.classList.toggle('hidden', !isNew);
    }

    radios.forEach(function (r) { r.addEventListener('change', update); });
    update();

    // Điền thông tin có sẵn của tài khoản
    var fillBtn = document.getElementById('fill-profile');
    if (fillBtn) {
        fillBtn.addEventListener('click', function () {
            // Chuyển sang "Dùng địa chỉ mới"
            radios.forEach(function (r) { if (r.value === '') r.checked = true; });
            update();

            if (fillBtn.dataset.name) document.getElementById('receiverName').value = fillBtn.dataset.name;
            if (fillBtn.dataset.phone) document.getElementById('phone').value = fillBtn.dataset.phone;

            // Đưa con trỏ tới ô còn trống đầu tiên
            var ids = ['receiverName', 'phone', 'province', 'district', 'ward', 'addressDetail'];
            for (var i = 0; i < ids.length; i++) {
                var el = document.getElementById(ids[i]);
                if (el && !el.value.trim()) { el.focus(); break; }
            }
        });
    }
});