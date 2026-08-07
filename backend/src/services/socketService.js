const { Server } = require("socket.io");

let io;

const init = (server) => {
  io = new Server(server, {
    cors: {
      origin: "*",
      methods: ["GET", "POST"]
    }
  });

  io.on("connection", (socket) => {
    console.log("A user connected:", socket.id);

    // Join a room for a specific order tracking
    socket.on("join_order", (orderId) => {
      socket.join(`order_${orderId}`);
      console.log(`Socket ${socket.id} joined order_${orderId}`);
    });

    // Fulfiller updates location
    socket.on("update_location", (data) => {
      const { orderId, fulfillerId, lat, lng } = data;
      // Broadcast to everyone tracking this order
      io.to(`order_${orderId}`).emit("location_changed", {
        lat,
        lng,
        fulfillerId,
        timestamp: new Date().toISOString()
      });
    });

    socket.on("disconnect", () => {
      console.log("User disconnected:", socket.id);
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
