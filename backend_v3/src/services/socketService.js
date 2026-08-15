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
    }
  });

  io.on("connection", (socket) => {
    console.log(`[Socket] Connected: ${socket.id}`);

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
        const { rows } = await db.query(
          `INSERT INTO messages (conversation_id, order_id, sender_id, sender_type, content)
           VALUES ($1, $2, $3, $4, $5) RETURNING id, created_at`,
          [conversation_id || null, order_id || null, sender_id, sender_type, content]
        );

        const savedMsg = {
            ...data,
            id: rows[0].id,
            created_at: rows[0].created_at
        };

        // Broadcast to room
        io.to(room).emit("receive_message", savedMsg);

        // Global Alert for Admin Dashboard (Real-time sync)
        if (sender_type !== 'ADMIN') {
            io.emit("admin_notification", {
                type: conversation_id ? 'SUPPORT' : 'ORDER_CHAT',
                id: conversation_id || order_id,
                preview: content.substring(0, 40)
            });
        }

      } catch (e) {
        console.error("[Socket] Message Error:", e.message);
      }
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
