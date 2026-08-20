const { Server } = require("socket.io");
const db = require("../config/db");
const fcmService = require("./fcmService");

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
    transports: ['websocket', 'polling'],
    allowEIO3: true,
    pingTimeout: 60000,
    pingInterval: 25000
  });

  io.on("connection", (socket) => {
    const clientIp = socket.handshake.headers['x-forwarded-for'] || socket.handshake.address;
    console.log(`[Socket] Connection attempt: ${socket.id} from ${clientIp} using ${socket.conn.transport.name}`);

    // Milestone 2/4: Join self-room for targeted dispatch
    const userId = socket.handshake.query.userId;
    if (userId) {
        socket.join(`user_${userId}`);
        console.log(`[Socket] User ${userId} joined their private channel.`);
    }

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

    // Mission Location Stream (Fulfiller -> Room)
    socket.on("update_mission_location", async (data) => {
        const { orderId, lat, lng } = data;

        // 1. Broadcast to participants
        io.to(`order_${orderId}`).emit("location_updated", { lat, lng });

        // 2. Persist to DB for dispatch visibility (Milestone 6)
        // Note: Using throttled/efficient update to fulfillers table
        try {
            await db.query(
                "UPDATE fulfillers SET current_location = ST_SetSRID(ST_MakePoint($1, $2), 4326), last_ping_at = CURRENT_TIMESTAMP WHERE id = (SELECT fulfiller_id FROM orders WHERE id = $3)",
                [lng, lat, orderId]
            );
        } catch (e) {
            // Silently fail persistence in socket stream to maintain latency
        }
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

        // Update last_active_at for support conversations
        if (conversation_id) {
            await db.query(
                "UPDATE conversations SET last_message_at = CURRENT_TIMESTAMP WHERE id = $1",
                [conversation_id]
            );
        }
      } catch (e) {
        console.warn(`[Socket] DB persist error: ${e.message}`);
      }

      const broadcastMsg = {
        id: require('uuid').v4(),
        conversation_id,
        order_id,
        sender_id,
        sender_type,
        content,
        body: content, // Backward compatibility for app
        created_at: new Date().toISOString()
      };
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

      // PUSH Notification for Support (v3.5.1)
      if (conversation_id && sender_type === 'ADMIN') {
          const convRes = await db.query("SELECT participant_id FROM conversations WHERE id = $1", [conversation_id]);
          if (convRes.rows.length > 0) {
              await fcmService.sendNotification(convRes.rows[0].participant_id, "Support Message", content);
          }
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
