document.addEventListener('DOMContentLoaded', function () {
    var form = document.getElementById('import-form');
    if (!form) return;

    var select = document.getElementById('variant-select');
    var filter = document.getElementById('variant-filter');
    var qtyInput = document.getElementById('add-qty');
    var costInput = document.getElementById('add-cost');
    var addBtn = document.getElementById('add-row');
    var tbody = document.getElementById('rows');
    var emptyRow = document.getElementById('empty-row');
    var totalEl = document.getElementById('grand-total');

    function fmt(n) { return n.toLocaleString('vi-VN') + ' ₫'; }

    // Lọc danh sách sản phẩm khi gõ
    var allOptions = Array.prototype.map.call(select.options, function (o) {
        return { value: o.value, text: o.text };
    });
    filter.addEventListener('input', function () {
        var q = filter.value.trim().toLowerCase();
        select.innerHTML = '';
        allOptions.forEach(function (o) {
            if (!q || o.text.toLowerCase().indexOf(q) !== -1) {
                var opt = document.createElement('option');
                opt.value = o.value;
                opt.textContent = o.text;
                select.appendChild(opt);
            }
        });
    });

    function recalc() {
        var rows = tbody.querySelectorAll('tr[data-row]');
        var total = 0;
        rows.forEach(function (tr) {
            var qty = parseInt(tr.querySelector('input[name="quantity"]').value, 10) || 0;
            var cost = parseFloat(tr.querySelector('input[name="unitCost"]').value) || 0;
            var sub = qty * cost;
            tr.querySelector('.sub').textContent = fmt(sub);
            total += sub;
        });
        totalEl.textContent = fmt(total);
        emptyRow.style.display = rows.length ? 'none' : '';
    }

    function addRow(id, label, qty, cost) {
        var tr = document.createElement('tr');
        tr.setAttribute('data-row', '1');

        var tdName = document.createElement('td');
        tdName.textContent = label;
        var hidden = document.createElement('input');
        hidden.type = 'hidden';
        hidden.name = 'variantId';
        hidden.value = id;
        tdName.appendChild(hidden);

        var tdQty = document.createElement('td');
        var qtyEl = document.createElement('input');
        qtyEl.type = 'number'; qtyEl.name = 'quantity'; qtyEl.min = '1'; qtyEl.value = qty;
        qtyEl.className = 'qty-input';
        tdQty.appendChild(qtyEl);

        var tdCost = document.createElement('td');
        var costEl = document.createElement('input');
        costEl.type = 'number'; costEl.name = 'unitCost'; costEl.min = '0'; costEl.step = '1000'; costEl.value = cost;
        costEl.className = 'qty-input cost-input';
        tdCost.appendChild(costEl);

        var tdSub = document.createElement('td');
        tdSub.className = 'sub line-total';

        var tdDel = document.createElement('td');
        var del = document.createElement('button');
        del.type = 'button'; del.className = 'link-danger'; del.textContent = 'Xóa';
        del.addEventListener('click', function () { tr.remove(); recalc(); });
        tdDel.appendChild(del);

        qtyEl.addEventListener('input', recalc);
        costEl.addEventListener('input', recalc);

        [tdName, tdQty, tdCost, tdSub, tdDel].forEach(function (td) { tr.appendChild(td); });
        tbody.appendChild(tr);
        recalc();
    }

    addBtn.addEventListener('click', function () {
        var opt = select.options[select.selectedIndex];
        if (!opt) { alert('Vui lòng chọn một sản phẩm'); return; }
        var qty = parseInt(qtyInput.value, 10);
        var cost = parseFloat(costInput.value);
        if (!(qty > 0)) { alert('Số lượng phải lớn hơn 0'); return; }
        if (!(cost >= 0)) { alert('Đơn giá nhập không hợp lệ'); return; }
        addRow(opt.value, opt.text, qty, cost);
        qtyInput.value = 1;
    });

	// Chống gửi 2 lần: khóa nút sau lần bấm đầu tiên
	var submitted = false;
	form.addEventListener('submit', function (e) {
	    if (!tbody.querySelector('tr[data-row]')) {
	        e.preventDefault();
	        alert('Vui lòng thêm ít nhất một sản phẩm vào phiếu');
	        return;
	    }
	    if (submitted) {
	        e.preventDefault();
	        return;
	    }
	    submitted = true;
	    // Nút bị khóa sẽ không gửi giá trị của nó, nên chép sang trường ẩn trước
	    if (e.submitter && e.submitter.name) {
	        var h = document.createElement('input');
	        h.type = 'hidden';
	        h.name = e.submitter.name;
	        h.value = e.submitter.value;
	        form.appendChild(h);
	    }
	    form.querySelectorAll('button[type="submit"]').forEach(function (b) { b.disabled = true; });
	});

	// Quay lại trang bằng nút Back: tải lại để có form trống và mã chống gửi lặp mới
	window.addEventListener('pageshow', function (e) {
	    if (e.persisted) {
	        window.location.reload();
	    }
	});
});