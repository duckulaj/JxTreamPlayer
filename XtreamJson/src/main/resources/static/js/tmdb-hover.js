(function () {
    'use strict';

    let hoverTimeout;
    let currentPopover = null;

    function initTmdbHover(context) {
        context = context || document;
        const movieCards = context.querySelectorAll('.clickable-movie-card');

        movieCards.forEach(card => {
            // Avoid double binding
            if (card.dataset.tmdbBound) return;
            card.dataset.tmdbBound = 'true';

            card.addEventListener('mouseenter', function () {
                const tmdbId = this.getAttribute('data-tmdb');
                if (!tmdbId || tmdbId === '0' || tmdbId === 'null') return;

                // Clear any existing timeout to avoid multiple triggers
                clearTimeout(hoverTimeout);

                hoverTimeout = setTimeout(() => {
                    showSummary(this, tmdbId);
                }, 1000); // 1 second delay
            });

            card.addEventListener('mouseleave', function () {
                clearTimeout(hoverTimeout);
                if (currentPopover) {
                    currentPopover.dispose();
                    currentPopover = null;
                }
            });
        });
    }

    function showSummary(element, tmdbId) {
        fetch(`/api/tmdb/summary?id=${tmdbId}`)
            .then(response => response.json())
            .then(data => {
                const overview = data && data.overview ? data.overview : "No summary available.";

                // Dispose previous if exists
                if (currentPopover) {
                    currentPopover.dispose();
                }

                // Create popover
                // We use Bootstrap 5 JS API
                // Ensure bootstrap is available
                if (typeof bootstrap === 'undefined') {
                    console.warn('Bootstrap 5 not loaded');
                    return;
                }

                currentPopover = new bootstrap.Popover(element, {
                    trigger: 'manual',
                    placement: 'bottom', // or auto
                    title: 'Movie Summary',
                    content: overview,
                    html: false,
                    customClass: 'tmdb-popover'
                });

                currentPopover.show();

                // Auto-hide after some time? maybe not needed if mouseleave handles it.
            })
            .catch(err => console.error('Error fetching summary:', err));
    }

    // Initial load
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => initTmdbHover(document));
    } else {
        initTmdbHover(document);
    }

    // Support for dynamic content (HTMX, manual fetch, etc.)
    const observer = new MutationObserver((mutations) => {
        mutations.forEach((mutation) => {
            mutation.addedNodes.forEach((node) => {
                if (node.nodeType === 1) { // Element node
                    if (node.classList.contains('clickable-movie-card')) {
                        initTmdbHover(node.parentElement || node);
                    } else if (node.querySelectorAll) {
                        const cards = node.querySelectorAll('.clickable-movie-card');
                        if (cards.length > 0) {
                            initTmdbHover(node);
                        }
                    }
                }
            });
        });
    });

    observer.observe(document.body, {
        childList: true,
        subtree: true
    });

})();
