document.addEventListener('DOMContentLoaded', function () {
    var form = document.getElementById('add-form');
    var mainImg = document.getElementById('main-img');
    var emptyImage = document.getElementById('main-img-empty');
    var defaultSrc = mainImg ? mainImg.dataset.defaultSrc : '';
    var baseThumbs = Array.prototype.slice.call(document.querySelectorAll('.thumb'));
    var variantStrip = document.getElementById('variant-thumbs');
    var variantThumbs = variantStrip
        ? Array.prototype.slice.call(variantStrip.querySelectorAll('.variant-thumb')) : [];

    function setMainImage(src) {
        if (!mainImg) return;
        if (src) {
            mainImg.src = src;
            mainImg.style.display = '';
            if (emptyImage) emptyImage.style.display = 'none';
        } else {
            mainImg.removeAttribute('src');
            mainImg.style.display = 'none';
            if (emptyImage) emptyImage.style.display = '';
        }
    }

    function showVariantImages(colorId) {
        if (!variantStrip) return;
        variantThumbs.forEach(function (thumb) {
            thumb.hidden = false;
            thumb.classList.remove('active');
        });
        variantStrip.style.display = variantThumbs.length ? 'flex' : 'none';

        var selectedImages = colorId ? variantThumbs.filter(function (thumb) {
            return thumb.dataset.color === colorId;
        }) : [];
        if (selectedImages.length) {
            selectedImages[0].classList.add('active');
            baseThumbs.forEach(function (thumb) { thumb.classList.remove('active'); });
            setMainImage(selectedImages[0].dataset.src);
        } else {
            setMainImage(defaultSrc);
            baseThumbs.forEach(function (thumb) {
                thumb.classList.toggle('active', thumb.dataset.src === defaultSrc);
            });
        }
    }

    baseThumbs.forEach(function (thumb) {
        thumb.addEventListener('click', function () {
            setMainImage(thumb.dataset.src);
            baseThumbs.forEach(function (item) { item.classList.toggle('active', item === thumb); });
            variantThumbs.forEach(function (item) { item.classList.remove('active'); });
        });
    });
    variantThumbs.forEach(function (thumb) {
        thumb.addEventListener('click', function () {
            setMainImage(thumb.dataset.src);
            variantThumbs.forEach(function (item) {
                item.classList.toggle('active', item === thumb);
            });
            baseThumbs.forEach(function (item) { item.classList.remove('active'); });
        });
    });

    if (!form) return;

    var variants = Array.prototype.map.call(document.querySelectorAll('#variant-data i'), function (el) {
        return { id: el.dataset.id, color: el.dataset.color, size: el.dataset.size,
            price: parseFloat(el.dataset.price) };
    });
    var colorBtns = Array.prototype.slice.call(document.querySelectorAll('.color-btn'));
    var sizeBtns = Array.prototype.slice.call(document.querySelectorAll('.size-btn'));
    var priceEl = document.getElementById('price');
    var prefixEl = document.getElementById('price-prefix');
    var defaultPrice = priceEl.textContent;
    var variantInput = document.getElementById('variantId');
    var colorName = document.getElementById('color-name');
    var sizeName = document.getElementById('size-name');
    var message = document.getElementById('select-msg');
    var needColor = colorBtns.length > 0;
    var needSize = sizeBtns.length > 0;
    var selectedColor = null;
    var selectedSize = null;

    function formatPrice(value) {
        return value.toLocaleString('vi-VN') + ' ₫';
    }

    function matches(variant) {
        return (!needColor || variant.color === selectedColor)
            && (!needSize || variant.size === selectedSize);
    }

    function refreshSelection() {
        sizeBtns.forEach(function (button) {
            var available = !selectedColor || variants.some(function (variant) {
                return variant.color === selectedColor && variant.size === button.dataset.id;
            });
            button.disabled = !available;
            if (!available && selectedSize === button.dataset.id) {
                selectedSize = null;
                button.classList.remove('active');
                sizeName.textContent = '—';
            }
        });
        colorBtns.forEach(function (button) {
            var available = !selectedSize || variants.some(function (variant) {
                return variant.size === selectedSize && variant.color === button.dataset.id;
            });
            button.disabled = !available;
            if (!available && selectedColor === button.dataset.id) {
                selectedColor = null;
                button.classList.remove('active');
                colorName.textContent = '—';
                showVariantImages(null);
            }
        });

        var ready = (!needColor || selectedColor) && (!needSize || selectedSize);
        var variant = ready ? variants.find(matches) : null;
        if (variant) {
            priceEl.textContent = formatPrice(variant.price);
            variantInput.value = variant.id;
            if (prefixEl) prefixEl.style.display = 'none';
            message.textContent = '';
        } else {
            priceEl.textContent = defaultPrice;
            variantInput.value = '';
            if (prefixEl) prefixEl.style.display = '';
        }
    }

    colorBtns.forEach(function (button) {
        button.addEventListener('click', function () {
            if (button.disabled) return;
            if (selectedColor === button.dataset.id) {
                selectedColor = null;
                button.classList.remove('active');
                colorName.textContent = '—';
            } else {
                selectedColor = button.dataset.id;
                colorBtns.forEach(function (item) {
                    item.classList.toggle('active', item === button);
                });
                colorName.textContent = button.dataset.name;
            }
            showVariantImages(selectedColor);
            refreshSelection();
        });
    });

    sizeBtns.forEach(function (button) {
        button.addEventListener('click', function () {
            if (button.disabled) return;
            if (selectedSize === button.dataset.id) {
                selectedSize = null;
                button.classList.remove('active');
                sizeName.textContent = '—';
            } else {
                selectedSize = button.dataset.id;
                sizeBtns.forEach(function (item) {
                    item.classList.toggle('active', item === button);
                });
                sizeName.textContent = button.dataset.name;
            }
            refreshSelection();
        });
    });

    if (colorBtns.length === 1) colorBtns[0].click();
    if (sizeBtns.length === 1) sizeBtns[0].click();
    if (variants.length === 1) variantInput.value = variants[0].id;

    var quantity = document.getElementById('qty');
    document.getElementById('qty-minus').addEventListener('click', function () {
        quantity.value = Math.max(1, (parseInt(quantity.value, 10) || 1) - 1);
    });
    document.getElementById('qty-plus').addEventListener('click', function () {
        quantity.value = Math.min(99, (parseInt(quantity.value, 10) || 1) + 1);
    });

    form.addEventListener('submit', function (event) {
        if (!variantInput.value) {
            event.preventDefault();
            message.textContent = 'Vui lòng chọn ' + (needColor && !selectedColor ? 'màu sắc' : 'kích thước')
                + ' trước khi tiếp tục.';
        }
    });
});