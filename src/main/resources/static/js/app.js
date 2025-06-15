// Sample user ID for testing
const currentUserId = "user123";

// Charts for the dashboard
let priceChart = null;
let riskChart = null;

// Load the market view by default
document.addEventListener('DOMContentLoaded', function() {
    setupNavHighlighting();
    loadMarketView();
    
    // Set up auto-refresh for market data (every 5 seconds)
    setInterval(refreshMarketData, 5000);
});

/**
 * Set up navigation highlighting
 */
function setupNavHighlighting() {
    const navLinks = document.querySelectorAll('.nav-link');
    
    navLinks.forEach(link => {
        link.addEventListener('click', function() {
            // Remove active class from all links
            navLinks.forEach(l => l.classList.remove('active'));
            
            // Add active class to clicked link
            this.classList.add('active');
        });
    });
    
    // Set market as default active
    document.querySelector('.nav-link').classList.add('active');
}

/**
 * Refresh market data without full page reload
 */
function refreshMarketData() {
    // Only refresh if we're on the market view
    if (!document.querySelector('.nav-link.active').textContent.includes('Market')) {
        return;
    }
    
    axios.get('/api/market/prices')
        .then(response => {
            const prices = response.data;
            
            // Update prices in the table
            Object.entries(prices).forEach(([symbol, data]) => {
                const priceCell = document.querySelector(`tr[data-symbol="${symbol}"] .price-value`);
                if (priceCell) {
                    const oldPrice = parseFloat(priceCell.dataset.price);
                    const newPrice = data.price;
                    
                    // Update price with color indication
                    priceCell.textContent = `$${newPrice.toFixed(2)}`;
                    priceCell.dataset.price = newPrice;
                    
                    // Add color flash effect based on price movement
                    if (newPrice > oldPrice) {
                        priceCell.classList.remove('price-down');
                        priceCell.classList.add('price-up');
                    } else if (newPrice < oldPrice) {
                        priceCell.classList.remove('price-up');
                        priceCell.classList.add('price-down');
                    }
                    
                    // Remove classes after animation
                    setTimeout(() => {
                        priceCell.classList.remove('price-up', 'price-down');
                    }, 1000);
                }
            });
            
            // Update chart if it exists
            if (priceChart) {
                const activeSymbol = priceChart.data.datasets[0].label.split(' ')[0];
                updatePriceChart(activeSymbol);
            }
        })
        .catch(error => {
            console.error('Error refreshing market data:', error);
        });
}

/**
 * Load the market view
 */
function loadMarketView() {
    const container = document.getElementById('app-container');
    
    // Show loading indicator
    container.innerHTML = '<div class="text-center"><div class="spinner-border" role="status"></div></div>';
    
    // Fetch market data
    axios.get('/api/market/prices')
        .then(response => {
            const prices = response.data;
            
            let content = `
                <h2>Market Overview</h2>
                <div class="row">
                    <div class="col-md-8">
                        <div class="card mb-4">
                            <div class="card-header d-flex justify-content-between align-items-center">
                                <span>Price Chart</span>
                                <div class="btn-group">
                                    <button class="btn btn-sm btn-outline-secondary" onclick="changeTimeframe('1H')">1H</button>
                                    <button class="btn btn-sm btn-outline-secondary" onclick="changeTimeframe('1D')">1D</button>
                                    <button class="btn btn-sm btn-outline-secondary" onclick="changeTimeframe('1W')">1W</button>
                                </div>
                            </div>
                            <div class="card-body">
                                <canvas id="priceChart"></canvas>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="card mb-4">
                            <div class="card-header d-flex justify-content-between align-items-center">
                                <span>Assets</span>
                                <input type="text" class="form-control form-control-sm" style="max-width: 150px;" 
                                    placeholder="Search assets" id="assetSearch" onkeyup="filterAssets()">
                            </div>
                            <div class="card-body">
                                <div class="table-responsive">
                                    <table class="table table-hover table-sm" id="assetsTable">
                                        <thead>
                                            <tr>
                                                <th onclick="sortTable(0)">Symbol <i class="bi bi-arrow-down-up"></i></th>
                                                <th onclick="sortTable(1)">Price <i class="bi bi-arrow-down-up"></i></th>
                                                <th onclick="sortTable(2)">Change <i class="bi bi-arrow-down-up"></i></th>
                                                <th>Action</th>
                                            </tr>
                                        </thead>
                                        <tbody>
            `;
            
            // Sort assets by symbol
            const sortedAssets = Object.entries(prices).sort((a, b) => a[0].localeCompare(b[0]));
            
            sortedAssets.forEach(([symbol, data]) => {
                // Calculate a fake percent change (in reality this would come from API)
                const percentChange = (Math.random() * 10 - 5).toFixed(2);
                const changeClass = percentChange >= 0 ? 'text-success' : 'text-danger';
                const changeIcon = percentChange >= 0 ? 'bi-arrow-up' : 'bi-arrow-down';
                
                content += `
                    <tr data-symbol="${symbol}">
                        <td>${symbol}</td>
                        <td class="price-value" data-price="${data.price}">$${data.price.toFixed(2)}</td>
                        <td class="${changeClass}"><i class="bi ${changeIcon}"></i> ${Math.abs(percentChange)}%</td>
                        <td>
                            <div class="btn-group">
                                <button class="btn btn-sm btn-primary" onclick="showTradeModal('${symbol}', ${data.price})">Trade</button>
                                <button class="btn btn-sm btn-outline-secondary" onclick="updatePriceChart('${symbol}')">Chart</button>
                            </div>
                        </td>
                    </tr>
                `;
            });
            
            content += `
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </div>
                        
                        <div class="card">
                            <div class="card-header">Market Summary</div>
                            <div class="card-body">
                                <p><strong>Total Assets:</strong> ${sortedAssets.length}</p>
                                <p><strong>Market Update:</strong> <span id="lastUpdate">${new Date().toLocaleTimeString()}</span></p>
                                <p><strong>Trading Volume:</strong> $${formatLargeNumber(calculateTotalVolume(prices))}</p>
                            </div>
                        </div>
                    </div>
                </div>
            `;
            
            // Add trade modal
            content += `
                <div class="modal fade" id="tradeModal" tabindex="-1">
                    <div class="modal-dialog">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title">Place Order</h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                            </div>
                            <div class="modal-body" id="tradeModalContent">
                                <!-- Trade form will be here -->
                            </div>
                        </div>
                    </div>
                </div>
            `;
            
            container.innerHTML = content;
            
            // Initialize price chart with the first symbol
            updatePriceChart(sortedAssets[0][0]);
            
            // Update the last update time every 5 seconds
            setInterval(() => {
                const lastUpdateElement = document.getElementById('lastUpdate');
                if (lastUpdateElement) {
                    lastUpdateElement.textContent = new Date().toLocaleTimeString();
                }
            }, 5000);
        })
        .catch(error => {
            container.innerHTML = `
                <div class="alert alert-danger">
                    <h4>Error loading market data</h4>
                    <p>${error.message}</p>
                    <button class="btn btn-outline-danger" onclick="loadMarketView()">Retry</button>
                </div>
            `;
        });
}

/**
 * Format large numbers with K, M, B suffixes
 */
function formatLargeNumber(num) {
    if (num >= 1000000000) {
        return (num / 1000000000).toFixed(1) + 'B';
    }
    if (num >= 1000000) {
        return (num / 1000000).toFixed(1) + 'M';
    }
    if (num >= 1000) {
        return (num / 1000).toFixed(1) + 'K';
    }
    return num.toFixed(0);
}

/**
 * Calculate total market volume
 */
function calculateTotalVolume(prices) {
    return Object.values(prices).reduce((sum, asset) => sum + asset.volume, 0);
}

/**
 * Filter assets in the table based on search input
 */
function filterAssets() {
    const input = document.getElementById('assetSearch');
    const filter = input.value.toUpperCase();
    const table = document.getElementById('assetsTable');
    const rows = table.getElementsByTagName('tr');
    
    for (let i = 1; i < rows.length; i++) {
        const symbolCell = rows[i].getElementsByTagName('td')[0];
        if (symbolCell) {
            const symbol = symbolCell.textContent || symbolCell.innerText;
            if (symbol.toUpperCase().indexOf(filter) > -1) {
                rows[i].style.display = '';
            } else {
                rows[i].style.display = 'none';
            }
        }
    }
}

/**
 * Sort the assets table
 */
function sortTable(columnIndex) {
    const table = document.getElementById('assetsTable');
    let switching = true;
    let shouldSwitch = false;
    let direction = 'asc';
    let switchcount = 0;
    
    while (switching) {
        switching = false;
        const rows = table.rows;
        
        for (let i = 1; i < rows.length - 1; i++) {
            shouldSwitch = false;
            
            const x = rows[i].getElementsByTagName('td')[columnIndex];
            const y = rows[i + 1].getElementsByTagName('td')[columnIndex];
            
            let xValue, yValue;
            
            // Handle different column types
            if (columnIndex === 1) { // Price column
                xValue = parseFloat(x.getAttribute('data-price'));
                yValue = parseFloat(y.getAttribute('data-price'));
            } else if (columnIndex === 2) { // Change column
                xValue = parseFloat(x.textContent.replace('%', '').trim());
                yValue = parseFloat(y.textContent.replace('%', '').trim());
                
                // Consider direction (positive/negative)
                if (x.classList.contains('text-danger')) xValue = -xValue;
                if (y.classList.contains('text-danger')) yValue = -yValue;
            } else {
                xValue = x.textContent.toLowerCase();
                yValue = y.textContent.toLowerCase();
            }
            
            if (direction === 'asc') {
                if (xValue > yValue) {
                    shouldSwitch = true;
                    break;
                }
            } else {
                if (xValue < yValue) {
                    shouldSwitch = true;
                    break;
                }
            }
        }
        
        if (shouldSwitch) {
            rows[i].parentNode.insertBefore(rows[i + 1], rows[i]);
            switching = true;
            switchcount++;
        } else {
            if (switchcount === 0 && direction === 'asc') {
                direction = 'desc';
                switching = true;
            }
        }
    }
}

/**
 * Change chart timeframe
 */
function changeTimeframe(timeframe) {
    if (!priceChart) return;
    
    const symbol = priceChart.data.datasets[0].label.split(' ')[0];
    updatePriceChart(symbol, timeframe);
}

/**
 * Update or create price chart for a symbol
 */
function updatePriceChart(symbol, timeframe = '1H') {
    // Fetch historical data for the symbol
    axios.get(`/api/market/history/${symbol}`)
        .then(response => {
            const history = response.data;
            
            // Filter based on timeframe
            let filteredHistory = history;
            const now = new Date();
            
            if (timeframe === '1H') {
                const oneHourAgo = new Date(now.getTime() - 60 * 60 * 1000);
                filteredHistory = history.filter(item => new Date(item.timestamp) >= oneHourAgo);
            } else if (timeframe === '1D') {
                const oneDayAgo = new Date(now.getTime() - 24 * 60 * 60 * 1000);
                filteredHistory = history.filter(item => new Date(item.timestamp) >= oneDayAgo);
            }
            
            // Extract timestamps and prices
            const timestamps = filteredHistory.map(item => new Date(item.timestamp).toLocaleTimeString());
            const prices = filteredHistory.map(item => item.price);
            
            const ctx = document.getElementById('priceChart').getContext('2d');
            
            // Destroy existing chart if it exists
            if (priceChart) {
                priceChart.destroy();
            }
            
            // Create new chart
            priceChart = new Chart(ctx, {
                type: 'line',
                data: {
                    labels: timestamps,
                    datasets: [{
                        label: `${symbol} Price`,
                        data: prices,
                        borderColor: 'rgb(75, 192, 192)',
                        backgroundColor: 'rgba(75, 192, 192, 0.1)',
                        borderWidth: 2,
                        fill: true,
                        tension: 0.2
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        title: {
                            display: true,
                            text: `${symbol} Price Chart (${timeframe})`
                        },
                        tooltip: {
                            mode: 'index',
                            intersect: false
                        },
                        legend: {
                            display: false
                        }
                    },
                    scales: {
                        y: {
                            beginAtZero: false,
                            ticks: {
                                callback: function(value) {
                                    return '$' + value.toFixed(2);
                                }
                            }
                        }
                    },
                    interaction: {
                        mode: 'nearest',
                        axis: 'x',
                        intersect: false
                    }
                }
            });
        })
        .catch(error => {
            console.error('Error updating chart:', error);
        });
}

/**
 * Load the portfolio view
 */
function loadPortfolioView() {
    const container = document.getElementById('app-container');
    container.innerHTML = '<div class="text-center"><div class="spinner-border" role="status"></div></div>';
    
    // Fetch user portfolio
    axios.get(`/api/market/portfolio/${currentUserId}`)
        .then(response => {
            const portfolio = response.data;
            
            // Fetch portfolio value for performance calculation
            axios.get(`/api/market/portfolio/${currentUserId}/value`)
                .then(valueResponse => {
                    const totalValue = valueResponse.data;
                    const initialInvestment = 10000; // This would come from the backend in a real app
                    const performance = ((totalValue - initialInvestment) / initialInvestment) * 100;
                    const performanceClass = performance >= 0 ? 'text-success' : 'text-danger';
                    
                    let content = `
                        <h2>Your Portfolio</h2>
                        <div class="row">
                            <div class="col-lg-8">
                                <div class="card mb-4">
                                    <div class="card-header">Account Overview</div>
                                    <div class="card-body">
                                        <div class="row">
                                            <div class="col-md-6">
                                                <h3>$${totalValue.toFixed(2)}</h3>
                                                <p>Total Portfolio Value</p>
                                                <p class="${performanceClass}">
                                                    <i class="bi ${performance >= 0 ? 'bi-arrow-up' : 'bi-arrow-down'}"></i>
                                                    ${Math.abs(performance).toFixed(2)}% overall
                                                </p>
                                            </div>
                                            <div class="col-md-6">
                                                <h3>$${portfolio.cashBalance.toFixed(2)}</h3>
                                                <p>Available Cash</p>
                                                <div class="btn-group">
                                                    <button class="btn btn-success" onclick="showDepositModal()">Deposit</button>
                                                    <button class="btn btn-outline-primary" onclick="showWithdrawModal()">Withdraw</button>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                                
                                <div class="card mb-4">
                                    <div class="card-header">Holdings</div>
                                    <div class="card-body">
                                        <div class="table-responsive">
                                            <table class="table table-hover">
                                                <thead>
                                                    <tr>
                                                        <th>Asset</th>
                                                        <th>Quantity</th>
                                                        <th>Current Price</th>
                                                        <th>Value</th>
                                                        <th>Action</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                    `;
                    
                    // Fetch prices to calculate current values
                    axios.get('/api/market/prices')
                        .then(priceResponse => {
                            const prices = priceResponse.data;
                            
                            if (Object.keys(portfolio.holdings).length === 0) {
                                content += `<tr><td colspan="5" class="text-center">No assets in your portfolio</td></tr>`;
                            } else {
                                Object.entries(portfolio.holdings).forEach(([symbol, quantity]) => {
                                    const price = prices[symbol] ? prices[symbol].price : 0;
                                    const value = price * quantity;
                                    
                                    content += `
                                        <tr>
                                            <td>${symbol}</td>
                                            <td>${quantity.toFixed(4)}</td>
                                            <td>$${price.toFixed(2)}</td>
                                            <td>$${value.toFixed(2)}</td>
                                            <td>
                                                <div class="btn-group">
                                                    <button class="btn btn-sm btn-success" onclick="showTradeModal('${symbol}', ${price}, 'BUY')">Buy</button>
                                                    <button class="btn btn-sm btn-danger" onclick="showTradeModal('${symbol}', ${price}, 'SELL')">Sell</button>
                                                </div>
                                            </td>
                                        </tr>
                                    `;
                                });
                            }
                            
                            content += `
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div class="col-lg-4">
                                <div class="card mb-4">
                                    <div class="card-header">Asset Allocation</div>
                                    <div class="card-body">
                                        <canvas id="allocationChart" height="250"></canvas>
                                    </div>
                                </div>
                                
                                <div class="card">
                                    <div class="card-header">Recent Orders</div>
                                    <div class="card-body">
                                        <div class="table-responsive">
                                            <table class="table table-sm">
                                                <thead>
                                                    <tr>
                                                        <th>Symbol</th>
                                                        <th>Type</th>
                                                        <th>Status</th>
                                                    </tr>
                                                </thead>
                                                <tbody id="recentOrdersBody">
                                                    <tr><td colspan="3" class="text-center">Loading orders...</td></tr>
                                                </tbody>
                                            </table>
                                        </div>
                                        <button class="btn btn-sm btn-outline-primary w-100" onclick="loadOrderHistory()">View All Orders</button>
                                    </div>
                                </div>
                            </div>
                        </div>
                        
                        <!-- Modals -->
                        <div class="modal fade" id="depositModal" tabindex="-1">
                            <div class="modal-dialog">
                                <div class="modal-content">
                                    <div class="modal-header">
                                        <h5 class="modal-title">Deposit Funds</h5>
                                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                                    </div>
                                    <div class="modal-body">
                                        <form id="depositForm">
                                            <div class="mb-3">
                                                <label class="form-label">Amount</label>
                                                <div class="input-group">
                                                    <span class="input-group-text">$</span>
                                                    <input type="number" class="form-control" id="depositAmount" min="10" step="10" required>
                                                </div>
                                            </div>
                                            <button type="button" class="btn btn-primary" onclick="depositFunds()">Deposit</button>
                                        </form>
                                    </div>
                                </div>
                            </div>
                        </div>
                        
                        <div class="modal fade" id="withdrawModal" tabindex="-1">
                            <div class="modal-dialog">
                                <div class="modal-content">
                                    <div class="modal-header">
                                        <h5 class="modal-title">Withdraw Funds</h5>
                                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                                    </div>
                                    <div class="modal-body">
                                        <form id="withdrawForm">
                                            <div class="mb-3">
                                                <label class="form-label">Amount</label>
                                                <div class="input-group">
                                                    <span class="input-group-text">$</span>
                                                    <input type="number" class="form-control" id="withdrawAmount" min="10" step="10" max="${portfolio.cashBalance}" required>
                                                </div>
                                                <small class="form-text text-muted">Available: $${portfolio.cashBalance.toFixed(2)}</small>
                                            </div>
                                            <button type="button" class="btn btn-primary" onclick="withdrawFunds()">Withdraw</button>
                                        </form>
                                    </div>
                                </div>
                            </div>
                        </div>
                        
                        <div class="modal fade" id="tradeModal" tabindex="-1">
                            <div class="modal-dialog">
                                <div class="modal-content">
                                    <div class="modal-header">
                                        <h5 class="modal-title">Place Order</h5>
                                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                                    </div>
                                    <div class="modal-body" id="tradeModalContent">
                                        <!-- Trade form will be here -->
                                    </div>
                                </div>
                            </div>
                        </div>
                    `;
                    
                    container.innerHTML = content;
                    
                    // Create allocation chart
                    createAllocationChart(portfolio, prices);
                    
                    // Load recent orders
                    loadRecentOrders();
                        });
                });
        })
        .catch(error => {
            container.innerHTML = `
                <div class="alert alert-danger">
                    <h4>Error loading portfolio</h4>
                    <p>${error.message}</p>
                    <button class="btn btn-outline-danger" onclick="loadPortfolioView()">Retry</button>
                </div>
            `;
        });
}

/**
 * Create portfolio allocation chart
 */
function createAllocationChart(portfolio, prices) {
    const ctx = document.getElementById('allocationChart').getContext('2d');
    
    // Calculate asset values
    const assets = [];
    const values = [];
    const colors = [
        'rgba(255, 99, 132, 0.8)',
        'rgba(54, 162, 235, 0.8)',
        'rgba(255, 206, 86, 0.8)',
        'rgba(75, 192, 192, 0.8)',
        'rgba(153, 102, 255, 0.8)',
        'rgba(255, 159, 64, 0.8)'
    ];
    
    // Add cash
    assets.push('Cash');
    values.push(portfolio.cashBalance);
    
    // Add holdings
    Object.entries(portfolio.holdings).forEach(([symbol, quantity]) => {
        const price = prices[symbol] ? prices[symbol].price : 0;
        const value = price * quantity;
        
        assets.push(symbol);
        values.push(value);
    });
    
    // Create chart
    new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: assets,
            datasets: [{
                data: values,
                backgroundColor: colors.slice(0, assets.length),
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            plugins: {
                legend: {
                    position: 'right',
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            const value = context.raw;
                            const total = context.dataset.data.reduce((a, b) => a + b, 0);
                            const percentage = ((value / total) * 100).toFixed(1);
                            return `${context.label}: $${value.toFixed(2)} (${percentage}%)`;
                        }
                    }
                }
            }
        }
    });
}

/**
 * Load recent orders
 */
function loadRecentOrders() {
    const ordersContainer = document.getElementById('recentOrdersBody');
    
    axios.get(`/api/market/orders/${currentUserId}`)
        .then(response => {
            const orders = response.data;
            
            if (orders.length === 0) {
                ordersContainer.innerHTML = `<tr><td colspan="3" class="text-center">No recent orders</td></tr>`;
                return;
            }
            
            let ordersHtml = '';
            
            // Show up to 5 most recent orders
            orders.slice(0, 5).forEach(order => {
                let statusClass = '';
                
                switch(order.status) {
                    case 'FILLED':
                        statusClass = 'text-success';
                        break;
                    case 'REJECTED':
                        statusClass = 'text-danger';
                        break;
                    case 'OPEN':
                        statusClass = 'text-primary';
                        break;
                    case 'CANCELLED':
                        statusClass = 'text-secondary';
                        break;
                }
                
                ordersHtml += `
                    <tr>
                        <td>${order.symbol}</td>
                        <td>${order.side} ${order.type}</td>
                        <td class="${statusClass}">${order.status}</td>
                    </tr>
                `;
            });
            
            ordersContainer.innerHTML = ordersHtml;
        })
        .catch(error => {
            ordersContainer.innerHTML = `<tr><td colspan="3" class="text-danger">Error loading orders</td></tr>`;
            console.error('Error loading orders:', error);
        });
}

/**
 * Load full order history view
 */
function loadOrderHistory() {
    const container = document.getElementById('app-container');
    container.innerHTML = '<div class="text-center"><div class="spinner-border" role="status"></div></div>';
    
    axios.get(`/api/market/orders/${currentUserId}`)
        .then(response => {
            const orders = response.data;
            
            let content = `
                <div class="d-flex justify-content-between align-items-center mb-4">
                    <h2>Order History</h2>
                    <button class="btn btn-outline-primary" onclick="loadPortfolioView()">Back to Portfolio</button>
                </div>
                
                <div class="card">
                    <div class="card-header">
                        <div class="row">
                            <div class="col-md-6">
                                <input type="text" class="form-control" id="orderSearchInput" 
                                    placeholder="Search orders" onkeyup="filterOrders()">
                            </div>
                            <div class="col-md-6 text-end">
                                <div class="btn-group">
                                    <button class="btn btn-outline-secondary" onclick="filterOrdersByStatus('all')">All</button>
                                    <button class="btn btn-outline-secondary" onclick="filterOrdersByStatus('OPEN')">Open</button>
                                    <button class="btn btn-outline-secondary" onclick="filterOrdersByStatus('FILLED')">Filled</button>
                                    <button class="btn btn-outline-secondary" onclick="filterOrdersByStatus('CANCELLED')">Cancelled</button>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="card-body">
                        <div class="table-responsive">
                            <table class="table table-hover" id="ordersTable">
                                <thead>
                                    <tr>
                                        <th>Date/Time</th>
                                        <th>Symbol</th>
                                        <th>Type</th>
                                        <th>Side</th>
                                        <th>Quantity</th>
                                        <th>Price</th>
                                        <th>Status</th>
                                        <th>Action</th>
                                    </tr>
                                </thead>
                                <tbody>
            `;
            
            if (orders.length === 0) {
                content += `<tr><td colspan="8" class="text-center">No orders found</td></tr>`;
            } else {
                orders.forEach(order => {
                    let statusClass = '';
                    
                    switch(order.status) {
                        case 'FILLED':
                            statusClass = 'text-success';
                            break;
                        case 'REJECTED':
                            statusClass = 'text-danger';
                            break;
                        case 'OPEN':
                            statusClass = 'text-primary';
                            break;
                        case 'CANCELLED':
                            statusClass = 'text-secondary';
                            break;
                    }
                    
                    const orderDate = new Date(order.createdAt);
                    
                    content += `
                        <tr data-status="${order.status}">
                            <td>${orderDate.toLocaleString()}</td>
                            <td>${order.symbol}</td>
                            <td>${order.type}</td>
                            <td>${order.side}</td>
                            <td>${order.quantity}</td>
                            <td>$${order.price.toFixed(2)}</td>
                            <td class="${statusClass}">${order.status}</td>
                            <td>
                                ${order.status === 'OPEN' ? 
                                    `<button class="btn btn-sm btn-outline-danger" onclick="cancelOrder('${order.id}')">Cancel</button>` : 
                                    ''}
                            </td>
                        </tr>
                    `;
                });
            }
            
            content += `
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            `;
            
            container.innerHTML = content;
        })
        .catch(error => {
            container.innerHTML = `
                <div class="alert alert-danger">
                    <h4>Error loading order history</h4>
                    <p>${error.message}</p>
                    <button class="btn btn-outline-danger" onclick="loadOrderHistory()">Retry</button>
                </div>
            `;
        });
}

/**
 * Filter orders in the table by keyword
 */
function filterOrders() {
    const input = document.getElementById('orderSearchInput');
    const filter = input.value.toUpperCase();
    const table = document.getElementById('ordersTable');
    const rows = table.getElementsByTagName('tr');
    
    for (let i = 1; i < rows.length; i++) {
        let visible = false;
        
        // Check all cells except the last one (action)
        for (let j = 0; j < rows[i].cells.length - 1; j++) {
            const cell = rows[i].cells[j];
            if (cell) {
                const content = cell.textContent || cell.innerText;
                if (content.toUpperCase().indexOf(filter) > -1) {
                    visible = true;
                    break;
                }
            }
        }
        
        rows[i].style.display = visible ? '' : 'none';
    }
}

/**
 * Filter orders by status
 */
function filterOrdersByStatus(status) {
    const table = document.getElementById('ordersTable');
    const rows = table.getElementsByTagName('tr');
    
    for (let i = 1; i < rows.length; i++) {
        if (status === 'all') {
            rows[i].style.display = '';
        } else {
            const rowStatus = rows[i].getAttribute('data-status');
            rows[i].style.display = rowStatus === status ? '' : 'none';
        }
    }
}

/**
 * Cancel an order
 */
function cancelOrder(orderId) {
    if (!confirm('Are you sure you want to cancel this order?')) {
        return;
    }
    
    axios.delete(`/api/market/order/${currentUserId}/${orderId}`)
        .then(response => {
            const result = response.data;
            
            if (result.success) {
                alert('Order cancelled successfully');
                loadOrderHistory(); // Refresh the view
            } else {
                alert(`Error: ${result.message}`);
            }
        })
        .catch(error => {
            alert(`Error cancelling order: ${error.message}`);
        });
}

/**
 * Show deposit modal
 */
function showDepositModal() {
    const modal = new bootstrap.Modal(document.getElementById('depositModal'));
    modal.show();
}

/**
 * Show withdraw modal
 */
function showWithdrawModal() {
    const modal = new bootstrap.Modal(document.getElementById('withdrawModal'));
    modal.show();
}

/**
 * Deposit funds to user's account
 */
function depositFunds() {
    const amount = parseFloat(document.getElementById('depositAmount').value);
    
    if (isNaN(amount) || amount <= 0) {
        alert('Please enter a valid amount');
        return;
    }
    
    axios.post(`/api/market/portfolio/${currentUserId}/deposit`, { amount })
        .then(() => {
            alert(`Successfully deposited $${amount.toFixed(2)}`);
            bootstrap.Modal.getInstance(document.getElementById('depositModal')).hide();
            loadPortfolioView(); // Refresh the view
        })
        .catch(error => {
            alert(`Error depositing funds: ${error.message}`);
        });
}

/**
 * Withdraw funds from user's account
 */
function withdrawFunds() {
    const amount = parseFloat(document.getElementById('withdrawAmount').value);
    
    if (isNaN(amount) || amount <= 0) {
        alert('Please enter a valid amount');
        return;
    }
    
    axios.post(`/api/market/portfolio/${currentUserId}/withdraw`, { amount })
        .then(response => {
            alert(`Successfully withdrew $${amount.toFixed(2)}`);
            bootstrap.Modal.getInstance(document.getElementById('withdrawModal')).hide();
            loadPortfolioView(); // Refresh the view
        })
        .catch(error => {
            alert(`Error withdrawing funds: ${error.response?.data || error.message}`);
        });
}

/**
 * Show trade modal for placing orders
 */
function showTradeModal(symbol, price, defaultSide = 'BUY') {
    const modal = new bootstrap.Modal(document.getElementById('tradeModal'));
    const modalContent = document.getElementById('tradeModalContent');
    
    modalContent.innerHTML = `
        <form id="tradeForm">
            <div class="mb-3">
                <label class="form-label">Asset</label>
                <input type="text" class="form-control" value="${symbol}" readonly>
            </div>
            <div class="mb-3">
                <label class="form-label">Current Price</label>
                <input type="text" class="form-control" value="$${price.toFixed(2)}" readonly>
            </div>
            <div class="mb-3">
                <label class="form-label">Order Type</label>
                <select class="form-select" id="orderType">
                    <option value="MARKET">Market Order</option>
                    <option value="LIMIT">Limit Order</option>
                </select>
            </div>
            <div class="mb-3">
                <label class="form-label">Side</label>
                <select class="form-select" id="orderSide">
                    <option value="BUY" ${defaultSide === 'BUY' ? 'selected' : ''}>Buy</option>
                    <option value="SELL" ${defaultSide === 'SELL' ? 'selected' : ''}>Sell</option>
                </select>
            </div>
            <div class="mb-3">
                <label class="form-label">Quantity</label>
                <input type="number" class="form-control" id="quantity" min="0.01" step="0.01" required>
            </div>
            <div class="mb-3" id="limitPriceGroup">
                <label class="form-label">Limit Price</label>
                <input type="number" class="form-control" id="limitPrice" value="${price.toFixed(2)}" min="0.01" step="0.01">
            </div>
            
            <div class="d-flex justify-content-between">
                <div>
                    <span class="text-muted" id="orderEstimate"></span>
                </div>
                <button type="button" class="btn btn-primary" onclick="submitOrder('${symbol}', ${price})">Place Order</button>
            </div>
        </form>
    `;
    
    // Show/hide limit price based on order type
    document.getElementById('orderType').addEventListener('change', function() {
        const limitPriceGroup = document.getElementById('limitPriceGroup');
        limitPriceGroup.style.display = this.value === 'LIMIT' ? 'block' : 'none';
        updateOrderEstimate(symbol, price);
    });
    
    // Update estimate when quantity changes
    document.getElementById('quantity').addEventListener('input', function() {
        updateOrderEstimate(symbol, price);
    });
    
    // Update estimate when price changes (for limit orders)
    document.getElementById('limitPrice').addEventListener('input', function() {
        updateOrderEstimate(symbol, price);
    });
    
    // Update estimate when side changes
    document.getElementById('orderSide').addEventListener('change', function() {
        updateOrderEstimate(symbol, price);
    });
    
    // Initialize visibility
    document.getElementById('limitPriceGroup').style.display = 'none';
    
    // Show modal
    modal.show();
    
    // Initialize estimate
    setTimeout(() => updateOrderEstimate(symbol, price), 100);
}

/**
 * Update order estimate
 */
function updateOrderEstimate(symbol, currentPrice) {
    const orderType = document.getElementById('orderType').value;
    const side = document.getElementById('orderSide').value;
    const quantityInput = document.getElementById('quantity');
    const quantity = parseFloat(quantityInput.value) || 0;
    const limitPriceInput = document.getElementById('limitPrice');
    const limitPrice = parseFloat(limitPriceInput.value) || currentPrice;
    
    const effectivePrice = orderType === 'MARKET' ? currentPrice : limitPrice;
    const totalValue = quantity * effectivePrice;
    
    const estimateElement = document.getElementById('orderEstimate');
    
    if (quantity > 0) {
        estimateElement.textContent = `Est. total: $${totalValue.toFixed(2)}`;
    } else {
        estimateElement.textContent = '';
    }
}

/**
 * Submit an order
 */
function submitOrder(symbol, currentPrice) {
    const orderType = document.getElementById('orderType').value;
    const side = document.getElementById('orderSide').value;
    const quantity = parseFloat(document.getElementById('quantity').value);
    const limitPrice = orderType === 'LIMIT' ? 
        parseFloat(document.getElementById('limitPrice').value) : 
        currentPrice;
    
    if (isNaN(quantity) || quantity <= 0) {
        alert('Please enter a valid quantity');
        return;
    }
    
    if (orderType === 'LIMIT' && (isNaN(limitPrice) || limitPrice <= 0)) {
        alert('Please enter a valid limit price');
        return;
    }
    
    const orderData = {
        userId: currentUserId,
        symbol: symbol,
        side: side,
        quantity: quantity,
        price: limitPrice,
        type: orderType
    };
    
    axios.post('/api/market/order', orderData)
        .then(response => {
            const result = response.data;
            
            if (result.success) {
                alert(`Order placed successfully: ${result.message}`);
                bootstrap.Modal.getInstance(document.getElementById('tradeModal')).hide();
                
                // Refresh the view based on current page
                if (document.querySelector('.nav-link.active').textContent.includes('Market')) {
                    loadMarketView();
                } else {
                    loadPortfolioView();
                }
            } else {
                alert(`Error: ${result.message}`);
            }
        })
        .catch(error => {
            alert(`Error placing order: ${error.message}`);
        });
}

/**
 * Load the risk analysis view
 */
function loadRiskView() {
    const container = document.getElementById('app-container');
    container.innerHTML = '<div class="text-center"><div class="spinner-border" role="status"></div></div>';
    
    // Fetch user portfolio for risk analysis
    axios.get(`/api/market/portfolio/${currentUserId}`)
        .then(response => {
            const portfolio = response.data;
            
            // Fetch current prices
            axios.get('/api/market/prices')
                .then(priceResponse => {
                    const prices = priceResponse.data;
                    
                    let content = `
                        <h2>Risk Analysis</h2>
                        <div class="row">
                            <div class="col-md-6">
                                <div class="card mb-4">
                                    <div class="card-header">Portfolio Risk</div>
                                    <div class="card-body">
                                        <div class="mb-3">
                                            <label class="form-label">Select Risk Metric</label>
                                            <select class="form-select" id="riskMetricSelect" onchange="updateRiskChart()">
                                                <option value="volatility">Volatility</option>
                                                <option value="drawdown">Max Drawdown</option>
                                                <option value="var">Value at Risk (VaR)</option>
                                            </select>
                                        </div>
                                        <canvas id="riskChart" height="250"></canvas>
                                    </div>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div class="card mb-4">
                                    <div class="card-header">Risk Exposure</div>
                                    <div class="card-body">
                                        <table class="table">
                                            <tbody>
                                                <tr>
                                                    <th>Portfolio Value</th>
                                                    <td id="portfolioValueCell">Calculating...</td>
                                                </tr>
                                                <tr>
                                                    <th>Daily Volatility</th>
                                                    <td id="volatilityCell">Calculating...</td>
                                                </tr>
                                                <tr>
                                                    <th>Value at Risk (95%)</th>
                                                    <td id="varCell">Calculating...</td>
                                                </tr>
                                                <tr>
                                                    <th>Risk Level</th>
                                                    <td id="riskLevelCell">
                                                        <div class="progress">
                                                            <div class="progress-bar bg-success" role="progressbar" style="width: 25%">Low</div>
                                                            <div class="progress-bar bg-warning" role="progressbar" style="width: 25%">Medium</div>
                                                            <div class="progress-bar bg-danger" role="progressbar" style="width: 25%">High</div>
                                                        </div>
                                                    </td>
                                                </tr>
                                            </tbody>
                                        </table>
                                    </div>
                                </div>
                            </div>
                        </div>
                        
                        <div class="card mb-4">
                            <div class="card-header">Transaction Risk History</div>
                            <div class="card-body">
                                <div class="table-responsive">
                                    <table class="table table-hover">
                                        <thead>
                                            <tr>
                                                <th>Date/Time</th>
                                                <th>Transaction</th>
                                                <th>Amount</th>
                                                <th>Risk Score</th>
                                                <th>Risk Level</th>
                                                <th>Anomalies</th>
                                            </tr>
                                        </thead>
                                        <tbody id="transactionRiskTableBody">
                                            <tr><td colspan="6" class="text-center">Loading transactions...</td></tr>
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </div>
                    `;
                    
                    container.innerHTML = content;
                    
                    // Initialize risk chart
                    createRiskChart('volatility');
                    
                    // Calculate portfolio risk metrics
                    calculateRiskMetrics(portfolio, prices);
                    
                    // Load transaction risk data
                    loadTransactionRiskData();
                });
        })
        .catch(error => {
            container.innerHTML = `
                <div class="alert alert-danger">
                    <h4>Error loading risk analysis</h4>
                    <p>${error.message}</p>
                    <button class="btn btn-outline-danger" onclick="loadRiskView()">Retry</button>
                </div>
            `;
        });
}

/**
 * Update risk chart based on selected metric
 */
function updateRiskChart() {
    const metricType = document.getElementById('riskMetricSelect').value;
    createRiskChart(metricType);
}

/**
 * Create risk chart based on selected metric
 */
function createRiskChart(metricType) {
    const ctx = document.getElementById('riskChart').getContext('2d');
    
    // Destroy existing chart if it exists
    if (riskChart) {
        riskChart.destroy();
    }
    
    // Generate sample data based on metric type
    let labels, data, color, title;
    
    switch (metricType) {
        case 'volatility':
            labels = ['BTC-USD', 'ETH-USD', 'AAPL', 'MSFT', 'AMZN', 'GOLD'];
            data = [0.035, 0.042, 0.015, 0.012, 0.018, 0.008];
            color = 'rgba(255, 99, 132, 0.7)';
            title = 'Asset Volatility (Daily)';
            break;
        case 'drawdown':
            labels = ['BTC-USD', 'ETH-USD', 'AAPL', 'MSFT', 'AMZN', 'GOLD'];
            data = [0.25, 0.32, 0.08, 0.07, 0.12, 0.05];
            color = 'rgba(255, 159, 64, 0.7)';
            title = 'Maximum Drawdown';
            break;
        case 'var':
            labels = ['BTC-USD', 'ETH-USD', 'AAPL', 'MSFT', 'AMZN', 'GOLD'];
            data = [0.08, 0.11, 0.03, 0.025, 0.04, 0.015];
            color = 'rgba(153, 102, 255, 0.7)';
            title = 'Value at Risk (95%)';
            break;
    }
    
    // Create chart
    riskChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: title,
                data: data,
                backgroundColor: color,
                borderColor: color.replace('0.7', '1'),
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            plugins: {
                legend: {
                    display: false
                },
                title: {
                    display: true,
                    text: title
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            const value = context.raw;
                            return `${(value * 100).toFixed(2)}%`;
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        callback: function(value) {
                            return (value * 100).toFixed(0) + '%';
                        }
                    }
                }
            }
        }
    });
}

/**
 * Calculate risk metrics for portfolio
 */
function calculateRiskMetrics(portfolio, prices) {
    // Calculate portfolio value
    let portfolioValue = portfolio.cashBalance;
    Object.entries(portfolio.holdings).forEach(([symbol, quantity]) => {
        const price = prices[symbol] ? prices[symbol].price : 0;
        portfolioValue += price * quantity;
    });
    
    // Update portfolio value
    document.getElementById('portfolioValueCell').textContent = `$${portfolioValue.toFixed(2)}`;
    
    // Calculate weighted volatility (simplified)
    let weightedVolatility = 0;
    let totalInvested = 0;
    
    Object.entries(portfolio.holdings).forEach(([symbol, quantity]) => {
        const price = prices[symbol] ? prices[symbol].price : 0;
        const value = price * quantity;
        totalInvested += value;
        
        // Assign volatility based on asset type (simplified)
        let volatility;
        if (symbol.includes('BTC') || symbol.includes('ETH')) {
            volatility = 0.035; // 3.5% daily for crypto
        } else if (symbol === 'GOLD') {
            volatility = 0.008; // 0.8% for gold
        } else {
            volatility = 0.015; // 1.5% for stocks
        }
        
        weightedVolatility += (value / portfolioValue) * volatility;
    });
    
    // Add cash component (zero volatility)
    weightedVolatility += (portfolio.cashBalance / portfolioValue) * 0;
    
    // Update volatility
    document.getElementById('volatilityCell').textContent = `${(weightedVolatility * 100).toFixed(2)}% daily`;
    
    // Calculate simplified VaR (95%)
    const valueAtRisk = portfolioValue * weightedVolatility * 1.65; // 95% confidence = 1.65 std deviations
    document.getElementById('varCell').textContent = `$${valueAtRisk.toFixed(2)} (${(valueAtRisk / portfolioValue * 100).toFixed(2)}%)`;
}

/**
 * Load transaction risk data
 */
function loadTransactionRiskData() {
    const tableBody = document.getElementById('transactionRiskTableBody');
    
    // In a real application, you would fetch this from an API
    // For demo purposes, we'll create sample data
    const sampleTransactions = [
        {
            timestamp: new Date(Date.now() - 1000 * 60 * 30).toISOString(), // 30 minutes ago
            type: 'TRADE_BUY',
            symbol: 'BTC-USD',
            amount: 3500.00,
            riskScore: 0.25,
            riskLevel: 'LOW',
            anomalies: []
        },
        {
            timestamp: new Date(Date.now() - 1000 * 60 * 60 * 2).toISOString(), // 2 hours ago
            type: 'TRADE_SELL',
            symbol: 'ETH-USD',
            amount: 1200.00,
            riskScore: 0.55,
            riskLevel: 'MEDIUM',
            anomalies: ['TIME']
        },
        {
            timestamp: new Date(Date.now() - 1000 * 60 * 60 * 5).toISOString(), // 5 hours ago
            type: 'TRADE_BUY',
            symbol: 'AAPL',
            amount: 5800.00,
            riskScore: 0.75,
            riskLevel: 'HIGH',
            anomalies: ['AMOUNT', 'FREQUENCY']
        }
    ];
    
    let tableHtml = '';
    
    if (sampleTransactions.length === 0) {
        tableHtml = '<tr><td colspan="6" class="text-center">No transactions found</td></tr>';
    } else {
        sampleTransactions.forEach(tx => {
            const txDate = new Date(tx.timestamp);
            let riskClass = '';
            
            switch(tx.riskLevel) {
                case 'LOW':
                    riskClass = 'text-success';
                    break;
                case 'MEDIUM':
                    riskClass = 'text-warning';
                    break;
                case 'HIGH':
                    riskClass = 'text-danger';
                    break;
            }
            
            let anomalyBadges = '';
            tx.anomalies.forEach(anomaly => {
                anomalyBadges += `<span class="badge bg-danger me-1">${anomaly}</span>`;
            });
            
            tableHtml += `
                <tr>
                    <td>${txDate.toLocaleString()}</td>
                    <td>${tx.type} ${tx.symbol}</td>
                    <td>$${tx.amount.toFixed(2)}</td>
                    <td>${(tx.riskScore * 100).toFixed(0)}%</td>
                    <td class="${riskClass}">${tx.riskLevel}</td>
                    <td>${anomalyBadges || 'None'}</td>
                </tr>
            `;
        });
    }
    
    tableBody.innerHTML = tableHtml;
}