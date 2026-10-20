// Ẩn/hiện mật khẩu
document.querySelectorAll('.toggle-pw').forEach(function(btn) {
	btn.addEventListener('click', function() {
		var input = document.getElementById(btn.dataset.target);
		var show = input.type === 'password';
		input.type = show ? 'text' : 'password';
		btn.textContent = show ? 'Ẩn' : 'Hiện';
	});
});

// Kiểm tra nhanh mật khẩu xác nhận phía client (server vẫn kiểm tra lại)
var registerForm = document.querySelector('form[action$="/register"]');
if (registerForm) {
	registerForm.addEventListener('submit', function(e) {
		var pw = document.getElementById('password').value;
		var cf = document.getElementById('confirmPassword').value;
		if (pw !== cf) {
			e.preventDefault();
			alert('Mật khẩu xác nhận không khớp');
		}
	});
}