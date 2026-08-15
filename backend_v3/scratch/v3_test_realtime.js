const io = require('socket.io-client');

const testRealtime = () => {
    console.log('📡 Testing V3 Real-time Sockets...');
    const socket = io('http://localhost:3000');

    socket.on('connect', () => {
        console.log('✅ Connected to v3 Socket Server');

        // Test Admin Notification
        console.log('📤 Sending mock user message to trigger admin alert...');
        socket.emit('send_message', {
            sender_id: 1,
            sender_type: 'USER',
            content: 'Hello, I need help with my delivery!',
            conversation_id: '550e8400-e29b-41d4-a716-446655440000'
        });
    });

    socket.on('admin_notification', (data) => {
        console.log('🔔 RECEIVED ADMIN ALERT:', data);
        console.log('✅ Real-time alert system is functional.');
        socket.disconnect();
        process.exit(0);
    });

    socket.on('connect_error', (err) => {
        console.error('❌ Socket Connection Failed:', err.message);
        process.exit(1);
    });

    setTimeout(() => {
        console.error('⏱️ Timeout: No alert received.');
        process.exit(1);
    }, 5000);
};

testRealtime();
