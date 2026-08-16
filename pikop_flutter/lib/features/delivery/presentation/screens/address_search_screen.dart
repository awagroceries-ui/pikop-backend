import 'package:flutter/material.dart';
import '../../../../core/theme/pikop_theme.dart';
import '../../data/places_repository.dart';

class AddressSearchScreen extends StatefulWidget {
  final String title;
  const AddressSearchScreen({super.key, required this.title});

  @override
  State<AddressSearchScreen> createState() => _AddressSearchScreenState();
}

class _AddressSearchScreenState extends State<AddressSearchScreen> {
  final _searchController = TextEditingController();
  final _placesRepo = PlacesRepository();
  List<dynamic> _suggestions = [];
  bool _isLoading = false;

  void _onSearchChanged(String value) async {
    if (value.length < 3) {
      setState(() => _suggestions = []);
      return;
    }

    setState(() => _isLoading = true);
    final results = await _placesRepo.getAutocomplete(value);
    if (mounted) {
      setState(() {
        _suggestions = results;
        _isLoading = false;
      });
    }
  }

  void _onSuggestionSelected(dynamic suggestion) async {
    final placeId = suggestion['place_id'];
    final description = suggestion['description'];

    setState(() => _isLoading = true);
    final coords = await _placesRepo.getPlaceCoordinates(placeId);

    if (mounted && coords != null) {
      Navigator.pop(context, {
        'address': description,
        'lat': coords['lat'],
        'lng': coords['lng'],
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(60),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: TextField(
              controller: _searchController,
              autofocus: true,
              decoration: InputDecoration(
                hintText: 'Search for a location...',
                prefixIcon: const Icon(Icons.search, color: PikopTheme.gold),
                suffixIcon: _isLoading
                  ? const Padding(
                      padding: EdgeInsets.all(12),
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : null,
                filled: true,
                fillColor: Colors.white.withOpacity(0.05),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                  borderSide: BorderSide.none,
                ),
              ),
              onChanged: _onSearchChanged,
            ),
          ),
        ),
      ),
      body: Column(
        children: [
          if (_suggestions.isEmpty && !_isLoading && _searchController.text.length > 2)
            const Padding(
              padding: EdgeInsets.all(32),
              child: Text('No results found. Try a different search.', style: TextStyle(color: PikopTheme.grey)),
            ),
          Expanded(
            child: ListView.separated(
              itemCount: _suggestions.length,
              separatorBuilder: (context, index) => Divider(color: Colors.white.withOpacity(0.1)),
              itemBuilder: (context, index) {
                final suggestion = _suggestions[index];
                return ListTile(
                  leading: const Icon(Icons.location_on_outlined, color: PikopTheme.grey),
                  title: Text(suggestion['structured_formatting']['main_text'] ?? ''),
                  subtitle: Text(
                    suggestion['structured_formatting']['secondary_text'] ?? '',
                    style: const TextStyle(fontSize: 12, color: PikopTheme.grey),
                  ),
                  onTap: () => _onSuggestionSelected(suggestion),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
