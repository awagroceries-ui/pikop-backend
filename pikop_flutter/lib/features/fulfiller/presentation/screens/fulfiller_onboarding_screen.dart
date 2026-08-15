import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/pikop_theme.dart';
import '../bloc/fulfiller_bloc.dart';

class FulfillerOnboardingScreen extends StatefulWidget {
  const FulfillerOnboardingScreen({super.key});

  @override
  State<FulfillerOnboardingScreen> createState() => _FulfillerOnboardingScreenState();
}

class _FulfillerOnboardingScreenState extends State<FulfillerOnboardingScreen> {
  int _currentStep = 0;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Fulfiller Onboarding')),
      body: BlocConsumer<FulfillerBloc, FulfillerState>(
        listener: (context, state) {
          if (state is KycFailure) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(content: Text(state.message), backgroundColor: Colors.red),
            );
          }
        },
        builder: (context, state) {
          return Column(
            children: [
              _buildStepIndicator(),
              Expanded(
                child: SingleChildScrollView(
                  padding: const EdgeInsets.all(24),
                  child: _buildCurrentStep(state),
                ),
              ),
            ],
          );
        },
      ),
    );
  }

  Widget _buildStepIndicator() {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 24),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: List.generate(4, (index) {
          return Container(
            width: 40,
            height: 4,
            margin: const EdgeInsets.symmetric(horizontal: 4),
            decoration: BoxDecoration(
              color: index <= _currentStep ? PikopTheme.gold : Colors.white.withOpacity(0.1),
              borderRadius: BorderRadius.circular(2),
            ),
          );
        }),
      ),
    );
  }

  Widget _buildCurrentStep(FulfillerState state) {
    switch (_currentStep) {
      case 0:
        return _buildClassSelection();
      case 1:
        return _buildIdentityStep(state);
      case 2:
        return _buildVehicleStep();
      case 3:
        return _buildDocumentsStep();
      default:
        return const SizedBox.shrink();
    }
  }

  Widget _buildClassSelection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(
          'Choose your fleet category',
          style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 32),
        _buildClassCard('Agent', 'Deliver by foot, bicycle or transit', Icons.directions_walk),
        const SizedBox(height: 16),
        _buildClassCard('Rider', 'Deliver by Motorcycle', Icons.motorcycle),
        const SizedBox(height: 16),
        _buildClassCard('Driver', 'Deliver by Car or Van', Icons.directions_car),
      ],
    );
  }

  Widget _buildClassCard(String title, String subtitle, IconData icon) {
    return Card(
      color: Colors.white.withOpacity(0.05),
      child: ListTile(
        leading: Icon(icon, color: PikopTheme.gold),
        title: Text(title, style: const TextStyle(fontWeight: FontWeight.bold)),
        subtitle: Text(subtitle, style: const TextStyle(fontSize: 12)),
        trailing: const Icon(Icons.chevron_right),
        onTap: () {
          setState(() => _currentStep = 1);
        },
      ),
    );
  }

  Widget _buildIdentityStep(FulfillerState state) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Icon(Icons.face_retouching_natural, size: 80, color: PikopTheme.gold),
        const SizedBox(height: 24),
        const Text(
          'Identity Verification',
          style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: 16),
        const Text(
          'We use our partner Didit to securely verify your identity. This includes a liveness check and ID scan.',
          textAlign: TextAlign.center,
          style: TextStyle(color: PikopTheme.grey),
        ),
        const SizedBox(height: 40),
        ElevatedButton(
          onPressed: state is KycLoading
            ? null
            : () {
              context.read<FulfillerBloc>().add(KycSessionStarted());
            },
          child: state is KycLoading
            ? const CircularProgressIndicator()
            : const Text('START VERIFICATION'),
        ),
        if (state is KycSessionReady) ...[
          const SizedBox(height: 16),
          const Text('Verification Session Initialized...', style: TextStyle(color: PikopTheme.green)),
          const SizedBox(height: 16),
          ElevatedButton(
            onPressed: () {
              // TODO: Launch WebView with state.sessionData['url']
              setState(() => _currentStep = 2);
            },
            style: ElevatedButton.styleFrom(backgroundColor: PikopTheme.green),
            child: const Text('PROCEED TO DIDIT'),
          ),
        ]
      ],
    );
  }

  Widget _buildVehicleStep() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text('Vehicle Details', style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
        const SizedBox(height: 24),
        const TextField(
          decoration: InputDecoration(labelText: 'Plate Number', border: OutlineInputBorder()),
        ),
        const SizedBox(height: 16),
        const TextField(
          decoration: InputDecoration(labelText: 'Vehicle Make (e.g. Honda)', border: OutlineInputBorder()),
        ),
        const SizedBox(height: 32),
        ElevatedButton(
          onPressed: () => setState(() => _currentStep = 3),
          child: const Text('SAVE & CONTINUE'),
        ),
      ],
    );
  }

  Widget _buildDocumentsStep() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text('Final Documents', style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
        const SizedBox(height: 24),
        _buildDocUploadTile('Driver License'),
        _buildDocUploadTile('Insurance'),
        _buildDocUploadTile('Road Worthiness'),
        const SizedBox(height: 40),
        ElevatedButton(
          onPressed: () {
            Navigator.pop(context);
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text('Application Submitted for Review!')),
            );
          },
          child: const Text('SUBMIT APPLICATION'),
        ),
      ],
    );
  }

  Widget _buildDocUploadTile(String label) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      color: Colors.white.withOpacity(0.05),
      child: ListTile(
        title: Text(label),
        trailing: const Icon(Icons.upload_file, color: PikopTheme.gold),
        onTap: () {
          // TODO: Use ImagePicker
        },
      ),
    );
  }
}
