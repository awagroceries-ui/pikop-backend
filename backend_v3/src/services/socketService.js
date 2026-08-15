const { Server } = require("socket.io");
const db = require("../config/db");

let io;

/**
 * Initializes Socket.io for v3.
 * Uses a room-based pattern for Tracking and Support.
 */
const init = (server) => {
  io = new Server(server, {
    cors: {
      origin: "*",
      methods: ["GET", "POST"]
    },
    transports: ['websocket', 'polling']
  });

  io.on("connection", (socket) => {
    console.log(`[Socket] Connection attempt: ${socket.id} from ${socket.handshake.address}`);

    // Join room for specific mission tracking
    socket.on("join_order", (orderId) => {
      socket.join(`order_${orderId}`);
      console.log(`[Socket] Client joined order: ${orderId}`);
    });

    // Join room for support session
    socket.on("join_support", (conversationId) => {
      socket.join(`support_${conversationId}`);
      console.log(`[Socket] Client joined support: ${conversationId}`);
    });

    // Message handler (Master Brief Milestone 8)
    socket.on("send_message", async (data) => {
      const { conversation_id, order_id, sender_id, sender_type, content } = data;
      const room = conversation_id ? `support_${conversation_id}` : `order_${order_id}`;

      try {
        await db.query(
          `INSERT INTO messages (conversation_id, order_id, sender_id, sender_type, content)
           VALUES ($1, $2, $3, $4, $5)`,
          [conversation_id || null, order_id || null, sender_id, sender_type, content]
        );
      } catch (e) {
        console.warn(`[Socket] DB persist skip (Test/Missing Record): ${e.message}`);
      }

      const broadcastMsg = { ...data, created_at: new Date().toISOString() };
      io.to(room).emit("receive_message", broadcastMsg);

      // Global Alert for Admin Dashboard (Sync with layout.ejs)
      if (sender_type !== 'ADMIN') {
          const alertEvent = conversation_id ? 'new_support_alert' : 'new_order_chat_alert';
          io.emit(alertEvent, {
              conversationId: conversation_id,
              orderId: order_id,
              body: content.substring(0, 50)
          });
      }
    });

    // KYC Alert Handler
    socket.on("internal_kyc_alert", (data) => {
        io.emit("new_kyc_alert", data);
    });

    socket.on("disconnect", () => {
      console.log(`[Socket] Disconnected: ${socket.id}`);
    });
  });

  return io;
};

const getIO = () => {
  if (!io) throw new Error("Socket.io not initialized");
  return io;
};

module.exports = { init, getIO };
