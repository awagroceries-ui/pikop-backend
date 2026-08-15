import 'package:flutter/material.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/pikop_theme.dart';
import '../../../../core/services/socket_service.dart';
import '../../../../core/services/location_service.dart';
import '../../data/delivery_repository.dart';
import '../bloc/delivery_bloc.dart';

class ActiveMissionScreen extends StatefulWidget {
  final int missionId;
  final bool isFulfiller;
  const ActiveMissionScreen({
    super.key,
    required this.missionId,
    this.isFulfiller = false
  });

  @override
  State<ActiveMissionScreen> createState() => _ActiveMissionScreenState();
}

class _ActiveMissionScreenState extends State<ActiveMissionScreen> {
  GoogleMapController? _mapController;
  final Map<MarkerId, Marker> _markers = {};
  final LocationService _locationService = LocationService();

  LatLng? _fulfillerPos;
  String _status = 'MATCHED';
  Map<String, dynamic>? _missionData;

  @override
  void initState() {
    super.initState();
    _initMission();
  }

  void _initMission() async {
    final socket = context.read<SocketService>();
    socket.joinOrder(widget.missionId);

    // Fetch Initial Data
    try {
      final res = await context.read<DeliveryRepository>().getOrderDetails(widget.missionId);
      if (mounted) {
        setState(() {
          _missionData = res.data['data'];
          _status = _missionData!['status'];
          // Initialize markers for pickup and delivery
          _markers[const MarkerId('pickup')] = Marker(
            markerId: const MarkerId('pickup'),
            position: LatLng(_missionData!['pickup_lat'], _missionData!['pickup_lng']),
            icon: BitmapDescriptor.defaultMarkerWithHue(BitmapDescriptor.hueYellow),
          );
          _markers[const MarkerId('delivery')] = Marker(
            markerId: const MarkerId('delivery'),
            position: LatLng(_missionData!['delivery_lat'], _missionData!['delivery_lng']),
            icon: BitmapDescriptor.defaultMarkerWithHue(BitmapDescriptor.hueGreen),
          );
        });
      }
    } catch (e) {}

    socket.on('location_updated', (data) {
      if (mounted) {
        setState(() {
          _fulfillerPos = LatLng(data['lat'], data['lng']);
          _updateFulfillerMarker();
        });
      }
    });

    socket.on('status_updated', (data) {
      if (mounted) {
        setState(() => _status = data['status']);
      }
    });

    if (widget.isFulfiller) {
      _startFulfillerTracking();
    }
  }

  void _startFulfillerTracking() async {
    if (await _locationService.handlePermission()) {
      _locationService.startTracking((pos) {
        context.read<SocketService>().updateLocation(
          widget.missionId,
          pos.latitude,
          pos.longitude
        );
      });
    }
  }

  void _updateFulfillerMarker() async {
    if (_fulfillerPos == null) return;

    // Logic for dynamic markers (Master Brief v3)
    String asset = 'assets/icons/marker_walking.png';
    if (_missionData != null) {
        final type = _missionData!['primary_class'];
        final mobility = _missionData!['mobility_type'];

        if (type == 'driver') asset = 'assets/icons/marker_car.png';
        else if (type == 'rider') asset = 'assets/icons/marker_bike.png';
        else if (mobility == 'bicycle') asset = 'assets/icons/marker_bicycle.png';
    }

    final icon = await BitmapDescriptor.fromAssetImage(
      const ImageConfiguration(size: Size(48, 48)),
      asset,
    );

    if (mounted) {
      setState(() {
        _markers[const MarkerId('fulfiller')] = Marker(
          markerId: const MarkerId('fulfiller'),
          position: _fulfillerPos!,
          icon: icon,
          anchor: const Offset(0.5, 0.5),
        );
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Mission: # ${widget.missionId}')),
      body: Stack(
        children: [
          GoogleMap(
            initialCameraPosition: const CameraPosition(target: LatLng(6.5244, 3.3792), zoom: 14),
            onMapCreated: (controller) => _mapController = controller,
            markers: _markers.values.toSet(),
            myLocationButtonEnabled: false,
            zoomControlsEnabled: false,
            mapStyle: _darkMapStyle,
          ),
          _buildStatusOverlay(),
        ],
      ),
      bottomSheet: _buildActionPanel(),
    );
  }

  Widget _buildStatusOverlay() {
    return Positioned(
      top: 16, left: 16, right: 16,
      child: Card(
        color: PikopTheme.black.withOpacity(0.8),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              const Icon(Icons.info_outline, color: PikopTheme.gold, size: 20),
              const SizedBox(width: 12),
              Text(
                'STATUS: $_status',
                style: const TextStyle(fontWeight: FontWeight.bold, letterSpacing: 1.2),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildActionPanel() {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: const BoxDecoration(
        color: PikopTheme.black,
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (widget.isFulfiller) ...[
            if (_status == 'MATCHED')
                ElevatedButton(
                  onPressed: () => _updateStatus('PICKED_UP'),
                  child: const Text('MARK AS PICKED UP'),
                ),
            if (_status == 'PICKED_UP')
                ElevatedButton(
                  onPressed: () => _updateStatus('DELIVERED'),
                  style: ElevatedButton.styleFrom(backgroundColor: PikopTheme.green),
                  child: const Text('MARK AS DELIVERED'),
                ),
          ] else ...[
            const Text('Your fulfiller is on the way.', style: TextStyle(color: PikopTheme.grey)),
            const SizedBox(height: 16),
            OutlinedButton(
              onPressed: () {},
              child: const Text('CONTACT SUPPORT')
            ),
          ]
        ],
      ),
    );
  }

  void _updateStatus(String newStatus) async {
    try {
        await context.read<DeliveryRepository>().updateStatus(widget.missionId, newStatus);
        setState(() => _status = newStatus);
    } catch (e) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Update failed: $e')));
    }
  }

  final String _darkMapStyle = '''[]'''; // Placeholder for Google Maps Dark Mode JSON
}
