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
      console.log(`Socket joined order_${orderId}`);
    });

    // Join a support conversation
    socket.on("join_support", (conversationId) => {
      socket.join(`support_${conversationId}`);
      console.log(`Socket joined support_${conversationId}`);
    });

    // Send a message (Unified for Support & Order)
    socket.on("send_message", async (data) => {
      const { conversationId, orderId, senderId, senderType, content } = data;
      const room = conversationId ? `support_${conversationId}` : `order_${orderId}`;

      try {
        // 1. Persist to DB
        const { rows } = await db.query(
          `INSERT INTO messages (conversation_id, order_id, sender_id, sender_type, content)
           VALUES ($1, $2, $3, $4, $5) RETURNING id, created_at`,
          [conversationId || null, orderId || null, senderId, senderType, content]
        );

        const savedMsg = { ...data, id: rows[0].id, created_at: rows[0].created_at };

        // 2. Broadcast to room
        io.to(room).emit("new_message", savedMsg);

        // 3. Optional: Trigger Notifications if recipient is offline
        // (Logic to determine recipient and check if room is empty would go here)
        if (conversationId && senderType === 'ADMIN') {
            // Push to user/fulfiller
            const convRes = await db.query("SELECT participant_id FROM conversations WHERE id = $1", [conversationId]);
            if (convRes.rows.length > 0) {
                await fcmService.sendNotification(convRes.rows[0].participant_id, "New Support Message", content);
            }
        }

        // 4. Update Conversation timestamp
        if (conversationId) {
            await db.query("UPDATE conversations SET last_message_at = CURRENT_TIMESTAMP WHERE id = $1", [conversationId]);
        }

      } catch (e) {
        console.error("Failed to process socket message:", e.message);
      }
    });

    // Fulfiller updates location
    socket.on("update_location", (data) => {
      const { orderId, fulfillerId, lat, lng } = data;
      io.to(`order_${orderId}`).emit("location_changed", {
        lat,
        lng,
        fulfillerId,
        timestamp: new Date().toISOString()
      });
    });

    socket.on("disconnect", () => {
      console.log("Socket disconnected:", socket.id);
    });
  });

  return io;
};

const getIO = () => {
  if (!io) {
    throw new Error("Socket.io not initialized!");
  }
  return io;
};

module.exports = { init, getIO };

