/**
 * main.js — Day 1
 * Offline Certificate Verification Using Blockchain
 * Basic interactivity: mobile navigation toggle.
 */

document.addEventListener('DOMContentLoaded', function () {

    // ---- Mobile Navigation Toggle ----
    const navToggle = document.getElementById('nav-toggle');
    const mainNav   = document.getElementById('main-nav');

    if (navToggle && mainNav) {
        navToggle.addEventListener('click', function () {
            const isOpen = mainNav.classList.toggle('open');
            navToggle.setAttribute('aria-expanded', isOpen.toString());
        });

        // Close nav when a link is clicked (mobile)
        mainNav.querySelectorAll('.nav-link').forEach(function (link) {
            link.addEventListener('click', function () {
                mainNav.classList.remove('open');
                navToggle.setAttribute('aria-expanded', 'false');
            });
        });

        // Close nav when clicking outside
        document.addEventListener('click', function (event) {
            if (!mainNav.contains(event.target) && !navToggle.contains(event.target)) {
                mainNav.classList.remove('open');
                navToggle.setAttribute('aria-expanded', 'false');
            }
        });
    }

    // ---- Highlight active nav link based on current path ----
    const currentPath = window.location.pathname;
    document.querySelectorAll('.nav-link').forEach(function (link) {
        link.classList.remove('active');
        const href = link.getAttribute('href');
        if (href === currentPath || (currentPath === '/' && href === '/')) {
            link.classList.add('active');
        }
    });

    console.log('[CertVerify] Day 1 - Application initialized. Backend: Spring Boot + SQLite.');
});
