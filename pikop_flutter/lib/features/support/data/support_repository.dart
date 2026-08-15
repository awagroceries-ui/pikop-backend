import 'package:dio/dio.dart';
import '../../../core/network/api_client.dart';

class SupportRepository {
  final ApiClient _apiClient = ApiClient();

  Future<Response> getKnowledgeBase() async {
    return await _apiClient.instance.get('/support/kb');
  }

  Future<Response> getOrCreateConversation() async {
    return await _apiClient.instance.post('/support/conversations');
  }

  Future<Response> getMessages(String conversationId) async {
    return await _apiClient.instance.get('/support/conversations/$conversationId/messages');
  }
}
