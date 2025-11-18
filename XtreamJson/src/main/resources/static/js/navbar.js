// Navbar search initializer (extracted from fragments/navbar.html)
(function() {
  'use strict';
  const KEY = '__navbarSearchInitialized';

  function getByIdInContext(context, id) {
    if (!context) return null;
    if (context.querySelector) return context.querySelector('#' + id);
    return document.getElementById(id);
  }

  function initNavbarSearch(context) {
    context = context || document;

    // mark the document-level initializer once
    if (context === document && document[KEY]) return;
    if (context !== document && context[KEY]) return;
    if (context === document) document[KEY] = true;
    else context[KEY] = true;

    const searchBtn = getByIdInContext(context, 'search-btn') || document.getElementById('search-btn');
    const searchBoxContainer = getByIdInContext(context, 'search-box-container') || document.getElementById('search-box-container');
    const searchBox = getByIdInContext(context, 'search-box') || document.getElementById('search-box');

    // fallback page target: prefer movie-category-container, then movie-category-items, then body
    const pageTarget = document.getElementById('movie-category-container')
      || document.getElementById('movie-category-items')
      || document.body;

    let debounceTimeout;
    let previousContent = null;
    let lastQuery = '';

    const spinnerHtml = '<div class="d-flex justify-content-center p-4"><div class="spinner-border" role="status"><span class="visually-hidden">Loading...</span></div></div>';

    function clearOtherContainers() {
      try { document.getElementById('admin-properties-container').innerHTML=''; } catch(e){}
      try { document.getElementById('providers-container').innerHTML=''; } catch(e){}
      try { document.getElementById('movie-category-dropdown').innerHTML=''; } catch(e){}
      try { document.getElementById('series-category-dropdown').innerHTML=''; } catch(e){}
      try { document.getElementById('series-list-container').innerHTML=''; } catch(e){}
      try { document.getElementById('seasons-list').innerHTML=''; } catch(e){}
      try { document.getElementById('movie-category-items').innerHTML=''; } catch(e){}
      try { document.getElementById('live-category-items').innerHTML=''; } catch(e){}
    }

    function performSearch(query) {
      if (!pageTarget) return;
      const normalized = (query || '').trim();
      if (normalized.length < 2) {
        if (lastQuery && lastQuery.length >= 2) {
          pageTarget.innerHTML = previousContent === null ? '' : previousContent;
          previousContent = null;
        }
        lastQuery = normalized;
        return;
      }

      if (previousContent === null) {
        previousContent = pageTarget.innerHTML;
      }
      lastQuery = normalized;

      pageTarget.innerHTML = spinnerHtml;

      fetch(`/searchTitles?query=${encodeURIComponent(normalized)}`)
        .then(resp => {
          if (!resp.ok) throw new Error('Network response was not ok');
          return resp.text();
        })
        .then(html => {
          clearOtherContainers();
          pageTarget.innerHTML = html;
        })
        .catch(err => {
          console.warn('Search fetch failed', err);
          pageTarget.innerHTML = previousContent === null ? '' : previousContent;
          previousContent = null;
        });
    }

    // Attach handlers if elements exist. Use dataset to avoid double-attaching.
    if (searchBtn && !searchBtn.dataset.navSearchAttached) {
      searchBtn.addEventListener('click', function(e) {
        // Toggle visibility
        if (!searchBoxContainer) return;
        searchBoxContainer.style.display = searchBoxContainer.style.display === 'none' ? 'block' : 'none';
        if (searchBoxContainer.style.display === 'block' && searchBox) {
          searchBox.focus();
          searchBox.select();
        }
      });
      searchBtn.dataset.navSearchAttached = '1';
    }

    if (searchBox && !searchBox.dataset.navSearchAttached) {
      searchBox.addEventListener('input', function() {
        clearTimeout(debounceTimeout);
        const query = searchBox.value;
        debounceTimeout = setTimeout(function() {
          performSearch(query);
        }, 300);
      });

      searchBox.addEventListener('keydown', function(e) {
        if (e.key === 'Enter') {
          e.preventDefault();
          clearTimeout(debounceTimeout);
          performSearch(searchBox.value);
        } else if (e.key === 'Escape') {
          if (searchBoxContainer) searchBoxContainer.style.display = 'none';
          if (previousContent !== null) {
            pageTarget.innerHTML = previousContent;
            previousContent = null;
            lastQuery = '';
          }
        }
      });

      searchBox.dataset.navSearchAttached = '1';
    }

    // global document click handler to hide input when user clicks outside
    if (!document.body.dataset.navSearchDocClickAttached) {
      document.addEventListener('click', function(e) {
        // if click target is search button, do nothing (searchBtn check covers both cases)
        if (searchBoxContainer && !searchBoxContainer.contains(e.target) && e.target !== searchBtn) {
          // hide the input but keep results intact
          try { searchBoxContainer.style.display = 'none'; } catch(e){}
        }
      });
      document.body.dataset.navSearchDocClickAttached = '1';
    }
  }

  // Init on initial load
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function() { initNavbarSearch(document); });
  } else {
    initNavbarSearch(document);
  }

  // If HTMX is used and swaps fragments into the page, initialize for swapped nodes as well
  if (window.htmx) {
    document.addEventListener('htmx:afterSwap', function(evt) {
      try { initNavbarSearch(evt.target || evt.detail?.target); } catch(e){}
    });
  }

  // For other insertion mechanisms, a MutationObserver as a last resort: watch for the search button being added
  const mo = new MutationObserver(function(mutations) {
    for (const m of mutations) {
      for (const node of m.addedNodes) {
        if (!(node instanceof Element)) continue;
        if (node.querySelector && node.querySelector('#search-btn')) {
          initNavbarSearch(node);
          return;
        }
      }
    }
  });
  mo.observe(document.documentElement || document.body, { childList: true, subtree: true });

})();
