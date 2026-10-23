document.addEventListener('DOMContentLoaded', function() {
	var form = document.getElementById('add-form');
	if (!form) return;

	var data = Array.prototype.map.call(document.querySelectorAll('#variant-data i'), function(el) {
		return { id: el.dataset.id, color: el.dataset.color, size: el.dataset.size, price: parseFloat(el.dataset.price) };
	});

	var colorBtns = Array.prototype.slice.call(document.querySelectorAll('.color-btn'));
	var sizeBtns = Array.prototype.slice.call(document.querySelectorAll('.size-btn'));
	var priceEl = document.getElementById('price');
	var prefixEl = document.getElementById('price-prefix');
	var defaultPrice = priceEl.textContent;
	var variantInput = document.getElementById('variantId');
	var colorName = document.getElementById('color-name');
	var sizeName = document.getElementById('size-name');
	var msg = document.getElementById('select-msg');
	var needColor = colorBtns.length > 0;
	var needSize = sizeBtns.length > 0;
	var selColor = null, selSize = null;

	function fmt(n) { return n.toLocaleString('vi-VN') + ' ₫'; }

	function match(v) {
		return (!needColor || v.color === selColor) && (!needSize || v.size === selSize);
	}

	function refresh() {
		// Size không có cho màu đang chọn thì khóa lại
		sizeBtns.forEach(function(b) {
			var ok = !selColor || data.some(function(v) { return v.color === selColor && v.size === b.dataset.id; });
			b.disabled = !ok;
			if (!ok && selSize === b.dataset.id) { selSize = null; b.classList.remove('active'); sizeName.textContent = '—'; }
		});
		// Màu không có cho size đang chọn thì khóa lại
		colorBtns.forEach(function(b) {
			var ok = !selSize || data.some(function(v) { return v.size === selSize && v.color === b.dataset.id; });
			b.disabled = !ok;
			if (!ok && selColor === b.dataset.id) { selColor = null; b.classList.remove('active'); colorName.textContent = '—'; }
		});

		var ready = (!needColor || selColor) && (!needSize || selSize);
		var v = ready ? data.find(match) : null;
		if (v) {
			priceEl.textContent = fmt(v.price);
			variantInput.value = v.id;
			if (prefixEl) prefixEl.style.display = 'none';
			msg.textContent = '';
		} else {
			priceEl.textContent = defaultPrice;
			variantInput.value = '';
			if (prefixEl) prefixEl.style.display = '';
		}
	}

	colorBtns.forEach(function(b) {
		b.addEventListener('click', function() {
			if (b.disabled) return;
			selColor = b.dataset.id;
			colorBtns.forEach(function(x) { x.classList.toggle('active', x === b); });
			colorName.textContent = b.dataset.name;
			refresh();
		});
	});

	sizeBtns.forEach(function(b) {
		b.addEventListener('click', function() {
			if (b.disabled) return;
			selSize = b.dataset.id;
			sizeBtns.forEach(function(x) { x.classList.toggle('active', x === b); });
			sizeName.textContent = b.dataset.name;
			refresh();
		});
	});

	// Chỉ có 1 lựa chọn thì chọn sẵn
	if (colorBtns.length === 1) colorBtns[0].click();
	if (sizeBtns.length === 1) sizeBtns[0].click();
	if (data.length === 1) variantInput.value = data[0].id;

	// Đổi ảnh chính khi bấm ảnh nhỏ
	var mainImg = document.getElementById('main-img');
	var thumbs = document.querySelectorAll('.thumb');
	thumbs.forEach(function(t) {
		t.addEventListener('click', function() {
			if (mainImg) mainImg.src = t.dataset.src;
			thumbs.forEach(function(x) { x.classList.toggle('active', x === t); });
		});
	});

	// Số lượng
	var qty = document.getElementById('qty');
	document.getElementById('qty-minus').addEventListener('click', function() {
		qty.value = Math.max(1, (parseInt(qty.value, 10) || 1) - 1);
	});
	document.getElementById('qty-plus').addEventListener('click', function() {
		qty.value = Math.min(99, (parseInt(qty.value, 10) || 1) + 1);
	});

	// Bắt buộc chọn đủ màu và size
	form.addEventListener('submit', function(e) {
		if (!variantInput.value) {
			e.preventDefault();
			msg.textContent = 'Vui lòng chọn ' + (needColor && !selColor ? 'màu sắc' : 'kích thước') + ' trước khi tiếp tục.';
		}
	});
});