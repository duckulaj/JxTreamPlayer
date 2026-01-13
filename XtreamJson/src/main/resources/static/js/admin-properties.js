document.addEventListener("DOMContentLoaded", function () {
    const checkboxes = document.querySelectorAll(".prefix-checkbox");
    const includedInput = document.getElementById("includedCountries");
    const dropdownButton = document.getElementById("prefixDropdownButton");

    // Only proceed if elements exist
    if (!includedInput || !dropdownButton) return;

    function updateDropdownText() {
        const checkedCount = document.querySelectorAll(".prefix-checkbox:checked").length;
        if (checkedCount === 0) {
            dropdownButton.textContent = "Select Prefixes";
        } else {
            dropdownButton.textContent = checkedCount + " Selected";
        }
    }

    // Initial update
    updateDropdownText();

    checkboxes.forEach((cb) => {
        cb.addEventListener("change", function () {
            let currentValues = includedInput.value
                .split(",")
                .map((s) => s.trim())
                .filter((s) => s.length > 0);

            const val = this.value;

            if (this.checked) {
                if (!currentValues.includes(val)) {
                    currentValues.push(val);
                }
            } else {
                currentValues = currentValues.filter((v) => v !== val);
            }

            includedInput.value = currentValues.join(",");
            updateDropdownText();
        });
    });

    // Also monitor manual changes to input to update checkboxes
    includedInput.addEventListener("input", function () {
        const currentValues = includedInput.value
            .split(",")
            .map((s) => s.trim());

        checkboxes.forEach((cb) => {
            cb.checked = currentValues.includes(cb.value);
        });
        updateDropdownText();
    });
});

// Initialize Bootstrap Dropdown Manually if needed
(function() {
    setTimeout(function() {
        const dropdownButton = document.getElementById('prefixDropdownButton');
        
        if (dropdownButton && typeof bootstrap !== 'undefined') {
        try {
            const dropdownInstance = new bootstrap.Dropdown(dropdownButton);
            
            // Manually handle toggle
            dropdownButton.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            dropdownInstance.toggle();
            });
        } catch(e) {
            console.error('Error initializing dropdown:', e);
        }
        }
    }, 100);
})();
