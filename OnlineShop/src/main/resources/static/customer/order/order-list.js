document.addEventListener('DOMContentLoaded', () => {
    getUserInfo();
    getOrderList();
});

async function getUserInfo() {
    const token = localStorage.getItem('token');
    if (!token || token.trim() === '') {
        alert('Vui lòng đăng nhập để xem đơn hàng!');
        window.location.href = '/customer/login/login.html';
        return;
    }

    try {
        const response = await fetch('http://localhost:8080/api/auth/user/info', {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        if (response.ok) {
            const userInfo = await response.json();
            if (userInfo && userInfo.fullName) {
                document.getElementById('userInfo').textContent = `Xin chào, ${userInfo.fullName}`;
            } else {
                document.getElementById('userInfo').textContent = 'Xin chào';
            }
        } else {
            const errorText = await response.text();
            if (response.status === 401) {
                alert('Token không hợp lệ hoặc hết hạn. Vui lòng đăng nhập lại!');
                window.location.href = '/auth/login.html';
            } else {
                alert(`Lỗi khi lấy thông tin người dùng: ${errorText}`);
            }
        }
    } catch (error) {
        alert('Lỗi kết nối đến server. Vui lòng thử lại sau!');
    }
}

let allOrders = [];

async function getOrderList() {
    const token = localStorage.getItem('token');
    if (!token) {
        alert('Vui lòng đăng nhập để xem đơn hàng!');
        window.location.href = '/auth/login.html';
        return;
    }

    try {
        const response = await fetch('http://localhost:8080/api/orders/user', {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        if (response.ok) {
            allOrders = await response.json();
            displayOrders(allOrders);
        } else {
            const errorText = await response.text();
            alert(`Lỗi khi lấy danh sách đơn hàng: ${errorText}`);
        }
    } catch (error) {
        alert('Lỗi kết nối đến server. Vui lòng thử lại sau!');
    }
}

function displayOrders(orders) {
    const orderList = document.getElementById('order-list');
    orderList.innerHTML = '';

    if (orders.length === 0) {
        orderList.innerHTML = '<tr><td colspan="5" style="text-align: center;">Không có đơn hàng nào phù hợp.</td></tr>';
        return;
    }

    orders.forEach(order => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td><span class="order-id">${order.id}</span></td>
            <td>${new Date(order.createdAt).toLocaleDateString('vi-VN')}</td>
            <td>${order.totalPrice.toLocaleString('vi-VN')} VNĐ</td>
            <td>${order.status}</td>
            <td><span class="action-button" onclick="viewOrderDetail(${order.id})">Xem chi tiết</span></td>
        `;
        orderList.appendChild(row);
    });
}

function viewOrderDetail(orderId) {
    window.location.href = `/customer/order/order-detail.html?orderId=${orderId}`;
}

function filterOrders() {
    const statusFilter = document.getElementById('status-filter').value;
    let filteredOrders = allOrders;
    document.addEventListener('DOMContentLoaded', () => {
        getUserInfo();
        getOrderDetail();
    });

    async function getUserInfo() {
        const token = localStorage.getItem('token');
        if (!token || token.trim() === '') {
            alert('Vui lòng đăng nhập để xem chi tiết đơn hàng!');
            window.location.href = '/auth/login.html';
            return;
        }

        try {
            const response = await fetch('http://localhost:8080/api/auth/user/info', {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });

            if (response.ok) {
                const userInfo = await response.json();
                if (userInfo && userInfo.fullName) {
                    document.getElementById('userInfo').textContent = `Xin chào, ${userInfo.fullName}`;
                } else {
                    document.getElementById('userInfo').textContent = 'Xin chào';
                }
            } else {
                const errorText = await response.text();
                if (response.status === 401) {
                    alert('Token không hợp lệ hoặc hết hạn. Vui lòng đăng nhập lại!');
                    window.location.href = '/auth/login.html';
                } else {
                    alert(`Lỗi khi lấy thông tin người dùng: ${errorText}`);
                }
            }
        } catch (error) {
            alert('Lỗi kết nối đến server. Vui lòng thử lại sau!');
        }
    }

    async function getOrderDetail() {
        const token = localStorage.getItem('token');
        if (!token) {
            alert('Vui lòng đăng nhập để xem chi tiết đơn hàng!');
            window.location.href = '/auth/login.html';
            return;
        }

        const urlParams = new URLSearchParams(window.location.search);
        const orderId = urlParams.get('orderId');
        if (!orderId) {
            alert('Không tìm thấy mã đơn hàng!');
            window.location.href = '/customer/order/order-list.html';
            return;
        }

        try {
            const response = await fetch(`http://localhost:8080/api/orders/${orderId}`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });

            if (response.ok) {
                const order = await response.json();
                displayOrderDetail(order);
            } else {
                const errorText = await response.text();
                alert(`Lỗi khi lấy chi tiết đơn hàng: ${errorText}`);
                window.location.href = '/customer/order/order-list.html';
            }
        } catch (error) {
            alert('Lỗi kết nối đến server. Vui lòng thử lại sau!');
            window.location.href = '/customer/order/order-list.html';
        }
    }

    let currentProductId = null;
    let selectedRating = 0;

    function displayOrderDetail(order) {
        // Hiển thị thông tin đơn hàng
        document.getElementById('order-id').textContent = order.id;
        document.getElementById('order-date').textContent = new Date(order.createdAt).toLocaleDateString('vi-VN');
        document.getElementById('total-price').textContent = order.totalPrice.toLocaleString('vi-VN') + ' VNĐ';
        document.getElementById('order-status').textContent = order.status;

        // Hiển thị danh sách sản phẩm
        const orderItems = document.getElementById('order-items');
        orderItems.innerHTML = '';

        const canReview = order.status === 'SHIPPED';
        document.getElementById('review-header').style.display = canReview ? 'table-cell' : 'none';

        if (order.items.length === 0) {
            orderItems.innerHTML = `<tr><td colspan="${canReview ? 5 : 4}" style="text-align: center;">Không có sản phẩm nào trong đơn hàng.</td></tr>`;
            return;
        }

        order.items.forEach(item => {
            const row = document.createElement('tr');
            row.innerHTML = `
            <td>${item.productName}</td>
            <td>${item.price.toLocaleString('vi-VN')} VNĐ</td>
            <td>${item.quantity}</td>
            <td>${(item.price * item.quantity).toLocaleString('vi-VN')} VNĐ</td>
            ${canReview ? `<td><span class="review-link" onclick="openReviewDialog(${item.productId})">Review</span></td>` : ''}
        `;
            orderItems.appendChild(row);
        });

        // Hiển thị nút Hủy hoặc Mua lại
        const actionButtons = document.getElementById('action-buttons');
        actionButtons.innerHTML = '';

        if (order.status === 'PENDING') {
            const cancelBtn = document.createElement('button');
            cancelBtn.classList.add('cancel-btn');
            cancelBtn.textContent = 'Hủy Đơn Hàng';
            cancelBtn.onclick = () => cancelOrder(order.id);
            actionButtons.appendChild(cancelBtn);
        } else if (order.status === 'CANCELLED' || order.status === 'DELIVERED') {
            const reorderBtn = document.createElement('button');
            reorderBtn.classList.add('reorder-btn');
            reorderBtn.textContent = 'Mua Lại';
            reorderBtn.onclick = () => reorder(order.items);
            actionButtons.appendChild(reorderBtn);
        }
    }

    function openReviewDialog(productId) {
        currentProductId = productId;
        selectedRating = 0;
        document.getElementById('review-comment').value = '';

        const stars = document.querySelectorAll('.star');
        stars.forEach(star => {
            star.classList.remove('selected');
            star.addEventListener('click', () => {
                selectedRating = parseInt(star.getAttribute('data-value'));
                stars.forEach(s => s.classList.remove('selected'));
                for (let i = 0; i < selectedRating; i++) {
                    stars[i].classList.add('selected');
                }
            });
        });

        document.getElementById('review-dialog').style.display = 'flex';
    }

    function closeReviewDialog() {
        document.getElementById('review-dialog').style.display = 'none';
        currentProductId = null;
        selectedRating = 0;
    }

    async function submitReview() {
        if (!currentProductId) {
            alert('Lỗi: Không xác định được sản phẩm để review!');
            return;
        }

        if (selectedRating === 0) {
            alert('Vui lòng chọn số sao để đánh giá!');
            return;
        }

        const comment = document.getElementById('review-comment').value.trim();
        if (!comment) {
            alert('Vui lòng nhập bình luận!');
            return;
        }

        const token = localStorage.getItem('token');
        if (!token) {
            alert('Vui lòng đăng nhập để gửi review!');
            window.location.href = '/auth/login.html';
            return;
        }

        let userId;
        try {
            userId = parseInt(await extractIdFromToken(token));
        } catch (error) {
            alert('Lỗi khi lấy thông tin người dùng. Vui lòng thử lại!');
            return;
        }

        const reviewData = {
            userId: userId,
            rating: selectedRating,
            comment: comment
        };

        try {
            const response = await fetch(`http://localhost:8080/api/products/${currentProductId}/reviews`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(reviewData)
            });

            if (response.ok) {
                alert('Gửi review thành công!');
                closeReviewDialog();
            } else {
                const errorText = await response.text();
                alert(`Lỗi khi gửi review: ${errorText}`);
            }
        } catch (error) {
            alert('Lỗi kết nối đến server. Vui lòng thử lại sau!');
        }
    }

    async function extractIdFromToken(token) {
        return new Promise((resolve) => {
            const mockUserId = "1";
            resolve(mockUserId);
        });
    }

    async function cancelOrder(orderId) {
        const token = localStorage.getItem('token');
        if (!token) {
            alert('Vui lòng đăng nhập để hủy đơn hàng!');
            window.location.href = '/auth/login.html';
            return;
        }

        if (!confirm('Bạn có chắc chắn muốn hủy đơn hàng này không?')) {
            return;
        }

        try {
            const response = await fetch(`http://localhost:8080/api/orders/${orderId}/cancel`, {
                method: 'PUT',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });

            if (response.ok) {
                alert('Hủy đơn hàng thành công!');
                getOrderDetail();
            } else {
                const errorText = await response.text();
                alert(`Lỗi khi hủy đơn hàng: ${errorText}`);
            }
        } catch (error) {
            alert('Lỗi kết nối đến server. Vui lòng thử lại sau!');
        }
    }

    async function reorder(items) {
        const token = localStorage.getItem('token');
        if (!token) {
            alert('Vui lòng đăng nhập để mua lại đơn hàng!');
            window.location.href = '/auth/login.html';
            return;
        }

        try {
            for (const item of items) {
                const response = await fetch(`http://localhost:8080/api/cart/add?productId=${item.productId}&quantity=${item.quantity}`, {
                    method: 'POST',
                    headers: {
                        'Authorization': `Bearer ${token}`,
                        'Content-Type': 'application/json'
                    }
                });

                if (!response.ok) {
                    const errorText = await response.text();
                    alert(`Lỗi khi thêm sản phẩm ${item.productName} vào giỏ hàng: ${errorText}`);
                    return;
                }
            }

            alert('Đã thêm tất cả sản phẩm vào giỏ hàng!');
            window.location.href = '/customer/cart/cart.html';
        } catch (error) {
            alert('Lỗi kết nối đến server. Vui lòng thử lại sau!');
        }
    }

    if (statusFilter !== 'ALL') {
        filteredOrders = allOrders.filter(order => order.status === statusFilter);
    }

    displayOrders(filteredOrders);
}

function log_out() {
    localStorage.removeItem('token');
    window.location.href = '/auth/login.html';
}