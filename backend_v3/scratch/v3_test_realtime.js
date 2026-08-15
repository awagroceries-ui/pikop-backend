const io = require('socket.io-client');

/**
 * V3 Real-time Diagnostic - Refined for VPS Networking
 */
const testRealtime = () => {
    console.log('📡 Testing V3 Real-time Sockets (v3.0.10)...');

    // Force WebSocket transport and use IPv4 loopback to avoid polling issues
    const socket = io('http://127.0.0.1:3000', {
        transports: ['websocket'],
        reconnection: false
    });

    socket.on('connect', () => {
        console.log('✅ Connected to v3 Socket Server via WebSocket');

        console.log('📤 Sending mock user message to trigger admin alert...');
        socket.emit('send_message', {
            sender_id: 1,
            sender_type: 'USER',
            content: 'V3 Real-time Test Message',
            conversation_id: '550e8400-e29b-41d4-a716-446655440000'
        });
    });

    socket.on('new_support_alert', (data) => {
        console.log('\n🔔 RECEIVED ADMIN ALERT:', data);
        console.log('✅ Real-time alert system is 100% functional.');
        socket.disconnect();
        process.exit(0);
    });

    socket.on('connect_error', (err) => {
        console.error('❌ Socket Connection Failed:', err.message);
        console.log('👉 Tip: Ensure pm2 logs show "🚀 PIKOP V3 API RESTORED"');
        process.exit(1);
    });

    setTimeout(() => {
        console.error('⏱️ Timeout: No alert received after 10 seconds.');
        console.log('This usually means the event name or room logic has a mismatch.');
        process.exit(1);
    }, 10000);
};

testRealtime();
