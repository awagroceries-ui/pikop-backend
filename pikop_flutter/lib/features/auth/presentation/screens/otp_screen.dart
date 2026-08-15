import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../bloc/auth_bloc.dart';
import '../../../../core/theme/pikop_theme.dart';

class OtpScreen extends StatefulWidget {
  final String email;
  const OtpScreen({super.key, required this.email});

  @override
  State<OtpScreen> createState() => _OtpScreenState();
}

class _OtpScreenState extends State<OtpScreen> {
  final _otpController = TextEditingController();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Verify Email')),
      body: BlocConsumer<AuthBloc, AuthState>(
        listener: (context, state) {
          if (state is AuthSuccess) {
            // TODO: Navigate to Home
          } else if (state is AuthFailure) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(content: Text(state.message), backgroundColor: Colors.red),
            );
          }
        },
        builder: (context, state) {
          return Padding(
            padding: const EdgeInsets.all(24.0),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text(
                  'Enter the code sent to ${widget.email}',
                  textAlign: TextAlign.center,
                  style: const TextStyle(color: PikopTheme.grey),
                ),
                const SizedBox(height: 32),
                TextField(
                  controller: _otpController,
                  decoration: const InputDecoration(
                    labelText: '6-Digit Code',
                    border: OutlineInputBorder(),
                  ),
                  keyboardType: TextInputType.number,
                  textAlign: TextAlign.center,
                  style: const TextStyle(fontSize: 24, letterSpacing: 8),
                  maxLength: 6,
                ),
                const SizedBox(height: 32),
                ElevatedButton(
                  onPressed: state is AuthLoading
                      ? null
                      : () {
                          context.read<AuthBloc>().add(
                                VerifyEmailRequested(
                                  widget.email,
                                  _otpController.text,
                                ),
                              );
                        },
                  child: state is AuthLoading
                      ? const CircularProgressIndicator()
                      : const Text('VERIFY'),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}
