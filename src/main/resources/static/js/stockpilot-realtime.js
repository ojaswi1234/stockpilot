// StockPilot Real-Time STOMP Client
(function () {
    let stompClient = null;
    let reconnectAttempts = 0;
    const MAX_RECONNECT_DELAY = 10000;

    const statusDot = document.getElementById('wsStatusDot');
    const statusText = document.getElementById('wsStatusText');
    const toastContainer = document.getElementById('toastContainer');

    function updateConnectionStatus(connected, message) {
        if (statusDot && statusText) {
            if (connected) {
                statusDot.style.backgroundColor = '#28a745';
                statusDot.style.boxShadow = '0 0 8px #28a745';
                statusText.textContent = 'Live Sync';
            } else {
                statusDot.style.backgroundColor = '#dc3545';
                statusDot.style.boxShadow = '0 0 8px #dc3545';
                statusText.textContent = message || 'Disconnected';
            }
        }
    }

    function showToast(title, body, bgClass = 'bg-primary') {
        if (!toastContainer) return;
        const toastId = 'toast-' + Date.now();
        const toastHtml = `
            <div id="${toastId}" class="toast align-items-center text-white ${bgClass} border-0 mb-2 shadow" role="alert" aria-live="assertive" aria-atomic="true">
                <div class="d-flex">
                    <div class="toast-body">
                        <strong><i class="bi bi-bell-fill me-1"></i>${title}</strong><br>
                        ${body}
                    </div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
                </div>
            </div>
        `;
        toastContainer.insertAdjacentHTML('beforeend', toastHtml);
        const toastElement = document.getElementById(toastId);
        if (toastElement) {
            const toast = new bootstrap.Toast(toastElement, { delay: 6000 });
            toast.show();
            toastElement.addEventListener('hidden.bs.toast', () => toastElement.remove());
        }
    }

    function connect() {
        updateConnectionStatus(false, 'Connecting...');
        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);
        stompClient.debug = null; // Suppress verbose console logs

        stompClient.connect({}, function (frame) {
            reconnectAttempts = 0;
            updateConnectionStatus(true);
            console.log('[StockPilot STOMP] Connected');

            // 1. Subscribe to inventory updates
            stompClient.subscribe('/topic/inventory', function (message) {
                try {
                    const payload = JSON.parse(message.body);
                    handleInventoryEvent(payload);
                } catch (e) {
                    console.error('Failed to parse inventory message:', e);
                }
            });

            // 2. Subscribe to order lifecycle events
            stompClient.subscribe('/topic/orders', function (message) {
                try {
                    const payload = JSON.parse(message.body);
                    handleOrderEvent(payload);
                } catch (e) {
                    console.error('Failed to parse order message:', e);
                }
            });

        }, function (error) {
            console.warn('[StockPilot STOMP] Disconnected:', error);
            updateConnectionStatus(false, 'Reconnecting...');
            scheduleReconnect();
        });
    }

    function scheduleReconnect() {
        const delay = Math.min(1000 * Math.pow(2, reconnectAttempts), MAX_RECONNECT_DELAY);
        reconnectAttempts++;
        setTimeout(connect, delay);
    }

    function handleInventoryEvent(event) {
        if (event.type === 'LOW_STOCK_ALERT') {
            showToast('CRITICAL: Low Stock Alert', event.message, 'bg-danger');
        } else {
            showToast('Stock Updated', event.message, 'bg-info');
        }

        // Live update inventory row in table if present
        if (event.sku && event.warehouseCode) {
            const row = document.getElementById(`inv-row-${event.sku}-${event.warehouseCode}`);
            if (row) {
                const onHandCell = row.querySelector('.inv-on-hand');
                const availableCell = row.querySelector('.inv-available');
                const statusBadge = row.querySelector('.inv-status');

                if (onHandCell && event.quantityOnHand !== undefined) {
                    onHandCell.textContent = event.quantityOnHand;
                }
                if (availableCell && event.availableQuantity !== undefined) {
                    availableCell.textContent = event.availableQuantity;
                }

                // Row flash animation
                row.classList.remove('stock-highlight');
                void row.offsetWidth; // Trigger reflow
                row.classList.add('stock-highlight');
            }
        }
    }

    function handleOrderEvent(event) {
        showToast('Order Status Updated', `Order ${event.orderNumber} is now ${event.orderStatus}`, 'bg-success');

        if (event.orderNumber) {
            const row = document.getElementById(`order-row-${event.orderNumber}`);
            if (row) {
                const badge = row.querySelector('.order-status-badge');
                if (badge) {
                    badge.innerHTML = `<span class="badge bg-primary">${event.orderStatus}</span>`;
                }
            }
        }
    }

    // Auto-connect on page load
    document.addEventListener('DOMContentLoaded', connect);
})();
