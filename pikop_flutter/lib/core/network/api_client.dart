import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class ApiClient {
  final Dio _dio = Dio();
  final FlutterSecureStorage _storage = const FlutterSecureStorage();

  ApiClient() {
    _dio.options.baseUrl = 'https://api.pikop.com.ng/api/v1';
    _dio.options.connectTimeout = const Duration(seconds: 15);
    _dio.options.receiveTimeout = const Duration(seconds: 15);

    _dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) async {
        final token = await _storage.read(key: 'accessToken');
        if (token != null) {
          options.headers['Authorization'] = 'Bearer $token';
        }
        return handler.next(options);
      },
      onError: (DioException e, handler) async {
        // 1. Handle Gateway Errors (502, 503, 504) with Retry
        if ([502, 503, 504].contains(e.response?.statusCode)) {
          final requestOptions = e.requestOptions;
          // Simple retry once after 2 seconds
          await Future.delayed(const Duration(seconds: 2));
          try {
            final response = await _dio.request(
              requestOptions.path,
              data: requestOptions.data,
              queryParameters: requestOptions.queryParameters,
              options: Options(
                method: requestOptions.method,
                headers: requestOptions.headers,
              ),
            );
            return handler.resolve(response);
          } catch (retryError) {
            return handler.next(e);
          }
        }

        // 2. Handle Token Expiry (401)
        if (e.response?.statusCode == 401) {
          // TODO: Implement Token Refresh Logic
        }
        return handler.next(e);
      },
    ));
  }

  Dio get instance => _dio;
}
