const { Server } = require("socket.io");
const db = require("../config/db");
const fcmService = require("./fcmService");

let io;

const init = (server) => {
  io = new Server(server, {
    cors: {
      origin: "*",
      methods: ["GET", "POST"]
    }
  });

  io.on("connection", (socket) => {
    console.log("Socket connected:", socket.id);

    // Join a room for a specific order tracking
    socket.on("join_order", (orderId) => {
      socket.join(`order_${orderId}`);
    });

    // Join a support conversation
    socket.on("join_support", (conversationId) => {
      socket.join(`support_${conversationId}`);
    });

    // Join a public tracking session
    socket.on("join_tracking", (trackingToken) => {
        socket.join(`tracking_${trackingToken}`);
    });

    // Send a message (Unified for Support & Order)
    socket.on("send_message", async (data) => {
      const { conversationId, orderId, senderId, senderType, content } = data;
      const room = conversationId ? `support_${conversationId}` : `order_${orderId}`;

      try {
        const { rows } = await db.query(
          `INSERT INTO messages (conversation_id, order_id, sender_id, sender_type, content)
           VALUES ($1, $2, $3, $4, $5) RETURNING id, created_at`,
          [conversationId || null, orderId || null, senderId, senderType, content]
        );

        const savedMsg = { ...data, id: rows[0].id, created_at: rows[0].created_at };
        io.to(room).emit("new_message", savedMsg);

        if (conversationId && senderType === 'ADMIN') {
            const convRes = await db.query("SELECT participant_id FROM conversations WHERE id = $1", [conversationId]);
            if (convRes.rows.length > 0) {
                await fcmService.sendNotification(convRes.rows[0].participant_id, "New Support Message", content, { type: "SUPPORT_CHAT" });
            }
        }

        if (conversationId) {
            await db.query("UPDATE conversations SET last_message_at = CURRENT_TIMESTAMP WHERE id = $1", [conversationId]);
        }
      } catch (e) {
        console.error("Socket send_message error:", e.message);
      }
    });

    // Fulfiller updates location
    socket.on("update_location", async (data) => {
      const { orderId, fulfillerId, lat, lng } = data;

      // 1. App Room
      io.to(`order_${orderId}`).emit("location_changed", { lat, lng, fulfillerId, timestamp: new Date().toISOString() });

      // 2. Public Tracking Room
      try {
          const { rows } = await db.query("SELECT tracking_token FROM orders WHERE id = $1", [orderId]);
          if (rows.length > 0 && rows[0].tracking_token) {
              io.to(`tracking_${rows[0].tracking_token}`).emit("location_changed", { lat, lng });
          }
      } catch (e) {}
    });

    socket.on("disconnect", () => {
      console.log("Socket disconnected:", socket.id);
    });
  });

  return io;
};

const getIO = () => {
  if (!io) throw new Error("Socket.io not initialized!");
  return io;
};

module.exports = { init, getIO };
