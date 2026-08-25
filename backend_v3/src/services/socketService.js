const { Server } = require("socket.io");
const db = require("../config/db");
const fcmService = require("./fcmService");

let io;

/**
 * Initializes Socket.io for v3.
 * Pattern: Server-Authoritative Mission Control.
 */
const init = (server) => {
  io = new Server(server, {
    cors: { origin: "*", methods: ["GET", "POST"] },
    transports: ['websocket', 'polling'],
    allowEIO3: true,
    pingTimeout: 60000,
    pingInterval: 25000
  });

  io.on("connection", async (socket) => {
    // MILSTONE: Support both modern 'auth' object and legacy query params
    const authUserId = socket.handshake.auth?.userId;
    const queryUserId = socket.handshake.query?.userId;
    const userId = authUserId || queryUserId;

    const clientIp = socket.handshake.headers['x-forwarded-for'] || socket.handshake.address;

    console.log(`[Socket] Connection attempt: id=${socket.id} | userId=${userId}`);

    if (!userId || userId === 'null' || userId === 'undefined') {
        console.warn(`[Socket] Connection attempt without valid userId from ${clientIp}. Limited functionality.`);
        return;
    }

    console.log(`[Socket] User ${userId} connected from ${clientIp}. Performing Auto-Room Join...`);

    try {
        // 1. Join Personal Room
        socket.join(`user_${userId}`);

        // 2. Auto-Join Active Missions (as customer or fulfiller)
        const missionRes = await db.query(
            "SELECT id FROM orders WHERE (user_id = $1 OR fulfiller_id = (SELECT id FROM fulfillers WHERE user_id = $1)) AND status NOT IN ('DELIVERED', 'CANCELLED')",
            [userId]
        );
        missionRes.rows.forEach(order => {
            socket.join(`order_${order.id}`);
            console.log(`[Socket] Auto-joined Order Room: ${order.id}`);
        });

        // 3. Auto-Join Open Support Conversations
        const supportRes = await db.query(
            "SELECT id FROM conversations WHERE participant_id = $1 AND status = 'OPEN'",
            [userId]
        );
        supportRes.rows.forEach(conv => {
            socket.join(`support_${conv.id}`);
            console.log(`[Socket] Auto-joined Support Room: ${conv.id}`);
        });

    } catch (e) {
        console.error(`[Socket] Auto-join failed for User ${userId}:`, e.message);
    }

    // Manual Joins (Legacy support)
    socket.on("join_order", (orderId) => { if(orderId && orderId !== 'null') socket.join(`order_${orderId}`); });
    socket.on("join_support", (conversationId) => { if(conversationId && conversationId !== 'null') socket.join(`support_${conversationId}`); });

    // Location Stream (Fulfiller -> Room)
    socket.on("update_mission_location", async (data) => {
        const { orderId, lat, lng } = data;
        if (!orderId || orderId === 'null') return;

        io.to(`order_${orderId}`).emit("location_updated", { lat, lng });

        // Throttle updates to DB (Milestone 6)
        try {
            await db.query(
                "UPDATE fulfillers SET current_location = ST_SetSRID(ST_MakePoint($1, $2), 4326), last_ping_at = CURRENT_TIMESTAMP WHERE id = (SELECT fulfiller_id FROM orders WHERE id = $3)",
                [lng, lat, orderId]
            );
        } catch (e) {}
    });

    // Typing Indicators (Pro-Tier UX)
    socket.on("typing_start", (data) => {
        const { conversation_id, order_id } = data;
        const room = conversation_id ? `support_${conversation_id}` : `order_${order_id}`;
        socket.to(room).emit("user_typing", { userId, isTyping: true });
    });

    socket.on("typing_stop", (data) => {
        const { conversation_id, order_id } = data;
        const room = conversation_id ? `support_${conversation_id}` : `order_${order_id}`;
        socket.to(room).emit("user_typing", { userId, isTyping: false });
    });

    // Read Receipts
    socket.on("mark_read", async (data) => {
        const { conversation_id, order_id } = data;
        const room = conversation_id ? `support_${conversation_id}` : `order_${order_id}`;
        try {
            await db.query(
                "UPDATE messages SET is_read = true WHERE (conversation_id = $1 OR order_id = $2) AND sender_id != $3",
                [conversation_id || null, order_id || null, userId]
            );
            io.to(room).emit("messages_read", { room, readBy: userId });
        } catch (e) {}
    });

    // Unified Messaging Handler
    socket.on("send_message", async (data) => {
      let { conversation_id, order_id, sender_id, sender_type, content } = data;

      if (conversation_id === "null") conversation_id = null;
      if (order_id === "null") order_id = null;

      const room = conversation_id ? `support_${conversation_id}` : `order_${order_id}`;

      try {
        const insertRes = await db.query(
          `INSERT INTO messages (conversation_id, order_id, sender_id, sender_type, content)
           VALUES ($1, $2, $3, $4, $5) RETURNING id, created_at`,
          [conversation_id || null, order_id || null, sender_id, sender_type, content]
        );

        if (conversation_id) {
            await db.query("UPDATE conversations SET last_message_at = CURRENT_TIMESTAMP WHERE id = $1", [conversation_id]);
        }

        // UNIFIED PAYLOAD
        const pikopMessage = {
            id: insertRes.rows[0].id,
            conversation_id,
            order_id,
            sender_id,
            sender_type,
            text: content, // Standardized field
            content,       // Backward compatibility
            body: content,  // Legacy compatibility
            created_at: insertRes.rows[0].created_at
        };

        io.to(room).emit("receive_message", pikopMessage);

        // Global Alert (Admin dashboard)
        if (sender_type !== 'ADMIN') {
            const event = conversation_id ? 'new_support_alert' : 'new_order_chat_alert';
            io.emit(event, { conversationId: conversation_id, orderId: order_id, body: content.substring(0, 50) });
        }

        // PUSH Notification for Support
        if (conversation_id && sender_type === 'ADMIN') {
            const convRes = await db.query("SELECT participant_id FROM conversations WHERE id = $1", [conversation_id]);
            if (convRes.rows.length > 0) {
                await fcmService.sendNotification(convRes.rows[0].participant_id, "Pikop Support", content);
            }
        }
      } catch (e) {
        console.warn(`[Socket] Send failed: ${e.message}`);
      }
    });

    socket.on("disconnect", () => {
      console.log(`[Socket] User disconnected.`);
    });
  });

  return io;
};

const getIO = () => {
  if (!io) throw new Error("Socket.io not initialized");
  return io;
};

module.exports = { init, getIO };
