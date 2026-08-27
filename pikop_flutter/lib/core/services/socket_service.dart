import 'package:socket_io_client/socket_io_client.dart' as io;

class SocketService {
  late io.Socket _socket;
  final String _baseUrl = 'https://api.pikop.com.ng';

  void connect(int userId) {
    _socket = io.io(
      _baseUrl,
      io.OptionBuilder()
          .setTransports(['websocket'])
          .setQuery({'userId': userId})
          .disableAutoConnect()
          .build(),
    );

    _socket.connect();

    _socket.onConnect((_) {
      print('[Socket] Connected to server');
    });

    _socket.onDisconnect((_) {
      print('[Socket] Disconnected from server');
    });
  }

  void joinOrder(int orderId) {
    _socket.emit('join_order', orderId);
  }

  void updateLocation(int orderId, double lat, double lng) {
    _socket.emit('update_mission_location', {
      'orderId': orderId,
      'lat': lat,
      'lng': lng,
    });
  }

  void on(String event, Function(dynamic) handler) {
    _socket.on(event, handler);
  }

  void emit(String event, dynamic data) {
    _socket.emit(event, data);
  }

  void disconnect() {
    _socket.disconnect();
  }
}
