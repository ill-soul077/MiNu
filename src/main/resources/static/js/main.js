// MiNu Store — Main JS

document.addEventListener('DOMContentLoaded', () => {

  // ── Auto-dismiss alerts after 4s ──
  document.querySelectorAll('.alert[data-auto-dismiss]').forEach(el => {
    setTimeout(() => {
      el.style.transition = 'opacity 0.5s ease';
      el.style.opacity = '0';
      setTimeout(() => el.remove(), 500);
    }, 4000);
  });

  // ── Quantity buttons ──
  document.querySelectorAll('.qty-input').forEach(wrap => {
    const input = wrap.querySelector('.qty-val');
    wrap.querySelector('.qty-minus')?.addEventListener('click', () => {
      const v = parseInt(input.value) || 1;
      if (v > 1) input.value = v - 1;
    });
    wrap.querySelector('.qty-plus')?.addEventListener('click', () => {
      const v = parseInt(input.value) || 1;
      const max = parseInt(input.max) || 9999;
      if (v < max) input.value = v + 1;
    });
  });

  // ── Mobile nav toggle ──
  const toggle = document.querySelector('.nav-mobile-toggle');
  const navLinks = document.querySelector('.nav-links');
  toggle?.addEventListener('click', () => navLinks?.classList.toggle('open'));

  // ── Confirm delete dialogs ──
  document.querySelectorAll('[data-confirm]').forEach(el => {
    el.addEventListener('click', (e) => {
      if (!confirm(el.dataset.confirm)) e.preventDefault();
    });
  });

  // ── Active nav link ──
  const path = window.location.pathname;
  document.querySelectorAll('.sidebar-link, .nav-links a').forEach(a => {
    if (a.getAttribute('href') && path.startsWith(a.getAttribute('href')) && a.getAttribute('href') !== '/') {
      a.classList.add('active');
    }
  });

  // ── Fade-in animation on cards ──
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(e => {
      if (e.isIntersecting) {
        e.target.style.opacity = '1';
        e.target.style.transform = 'translateY(0)';
        observer.unobserve(e.target);
      }
    });
  }, { threshold: 0.1 });

  document.querySelectorAll('.product-card, .stat-card').forEach((el, i) => {
    el.style.opacity = '0';
    el.style.transform = 'translateY(16px)';
    el.style.transition = `opacity 0.4s ease ${i * 0.05}s, transform 0.4s ease ${i * 0.05}s`;
    observer.observe(el);
  });
});