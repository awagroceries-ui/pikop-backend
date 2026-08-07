const app = require('./app');
const http = require('http');
const socketService = require('./services/socketService');
require('dotenv').config();

const PORT = process.env.PORT || 3000;
const server = http.createServer(app);

// Initialize Socket.io
socketService.init(server);

server.listen(PORT, () => {
  console.log(`Pikop API Server running on port ${PORT}`);
});
