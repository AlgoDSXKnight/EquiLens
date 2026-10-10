import 'dart:convert';

import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart' as firebase;
import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;
import 'package:provider/provider.dart';

const researchUrl = String.fromEnvironment('RESEARCH_API_URL',
    defaultValue: 'http://localhost:8080/api');
const portfolioUrl = String.fromEnvironment('PORTFOLIO_API_URL',
    defaultValue: 'http://localhost:8081/api');

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  var firebaseReady = false;
  try {
    await Firebase.initializeApp();
    await firebase.FirebaseAuth.instance.signInAnonymously();
    firebaseReady = true;
  } catch (_) {
    // Firebase is optional until the platform-specific Firebase config is added.
  }
  runApp(ChangeNotifierProvider(
    create: (_) => EquiLensState(firebaseReady: firebaseReady)
      ..restoreSession()
      ..restorePreferences(),
    child: const EquiLensApp(),
  ));
}

class ApiClient {
  ApiClient(this.baseUrl, {this.onUnauthorized, TokenStore? tokenStore})
      : _storage = tokenStore ?? SecureTokenStore();
  final String baseUrl;
  final Future<void> Function()? onUnauthorized;
  final TokenStore _storage;

  Future<http.Response> request(String path,
      {String method = 'GET', Object? body, bool auth = true}) async {
    final token = await _storage.read('equilens_token');
    final headers = <String, String>{'Content-Type': 'application/json'};
    if (auth && token != null) headers['Authorization'] = 'Bearer $token';
    final uri = Uri.parse('$baseUrl$path');
    final pending = switch (method) {
      'POST' => http.post(uri,
          headers: headers, body: body == null ? null : jsonEncode(body)),
      'DELETE' => http.delete(uri, headers: headers),
      _ => http.get(uri, headers: headers),
    };
    final response = await pending.timeout(const Duration(seconds: 15));
    if (response.statusCode == 401) {
      await _storage.delete('equilens_token');
      await onUnauthorized?.call();
      throw const UnauthorizedException('Session expired. Sign in again.');
    }
    if (response.statusCode >= 400) {
      Object? payload;
      try {
        payload = jsonDecode(response.body);
      } catch (_) {
        payload = null;
      }
      final message = payload is Map
          ? payload['error']?.toString() ?? payload['message']?.toString()
          : null;
      throw HttpException(message ?? 'Request failed (${response.statusCode})');
    }
    return response;
  }
}

class HttpException implements Exception {
  const HttpException(this.message);
  final String message;
  @override
  String toString() => message;
}

class UnauthorizedException extends HttpException {
  const UnauthorizedException(super.message);
}

abstract interface class TokenStore {
  Future<String?> read(String key);
  Future<void> write(String key, String value);
  Future<void> delete(String key);
}

class SecureTokenStore implements TokenStore {
  final _storage = const FlutterSecureStorage();

  @override
  Future<String?> read(String key) => _storage.read(key: key);

  @override
  Future<void> write(String key, String value) =>
      _storage.write(key: key, value: value);

  @override
  Future<void> delete(String key) => _storage.delete(key: key);
}

class FirebasePreferences {
  FirebasePreferences(this.enabled);
  final bool enabled;

  Future<String?> loadLastSection() async {
    if (!enabled) return null;
    final uid = firebase.FirebaseAuth.instance.currentUser?.uid;
    if (uid == null) return null;
    try {
      final snapshot =
          await FirebaseFirestore.instance.collection('users').doc(uid).get();
      final data = snapshot.data();
      final preferences = data?['preferences'];
      return preferences is Map ? preferences['lastSection'] as String? : null;
    } catch (_) {
      return null;
    }
  }

  Future<void> saveLastSection(String section) async {
    if (!enabled) return;
    final uid = firebase.FirebaseAuth.instance.currentUser?.uid;
    if (uid == null) return;
    try {
      await FirebaseFirestore.instance.collection('users').doc(uid).set(
        {
          'preferences': {'lastSection': section}
        },
        SetOptions(merge: true),
      );
    } catch (_) {
      // Preferences are optional; they must not interrupt backend features.
    }
  }
}

class AuthState extends ChangeNotifier {
  bool busy = false;
  String? error;
  String? email;

  bool get signedIn => email != null;

  void clear() {
    email = null;
    error = null;
    notifyListeners();
  }
}

class CompanyState extends ChangeNotifier {
  List<Map<String, dynamic>> companies = [];

  void replace(List<Map<String, dynamic>> value) {
    companies = value;
    notifyListeners();
  }
}

class WatchlistState extends ChangeNotifier {
  List<Map<String, dynamic>> watchlist = [];

  void replace(List<Map<String, dynamic>> value) {
    watchlist = value;
    notifyListeners();
  }
}

class PortfolioState extends ChangeNotifier {
  Map<String, dynamic>? portfolio;
  List<Map<String, dynamic>> transactions = [];

  void replace({
    required Map<String, dynamic> summary,
    required List<Map<String, dynamic>> history,
  }) {
    portfolio = summary;
    transactions = history;
    notifyListeners();
  }

  void clear() {
    portfolio = null;
    transactions = [];
    notifyListeners();
  }
}

class EquiLensState extends ChangeNotifier {
  EquiLensState({
    required this.firebaseReady,
    TokenStore? tokenStore,
    ApiClient? portfolioApi,
  })  : _storage = tokenStore ?? SecureTokenStore(),
        research = ApiClient(researchUrl),
        preferences = FirebasePreferences(firebaseReady) {
    this.portfolioApi = portfolioApi ??
        ApiClient(
          portfolioUrl,
          onUnauthorized: _clearSession,
          tokenStore: _storage,
        );
  }
  final AuthState auth = AuthState();
  final CompanyState companyState = CompanyState();
  final WatchlistState watchlistState = WatchlistState();
  final PortfolioState portfolioState = PortfolioState();
  final bool firebaseReady;
  final ApiClient research;
  late final ApiClient portfolioApi;
  final FirebasePreferences preferences;
  final TokenStore _storage;
  bool get busy => auth.busy;
  set busy(bool value) => auth.busy = value;
  String? get error => auth.error;
  set error(String? value) => auth.error = value;
  String? get email => auth.email;
  set email(String? value) => auth.email = value;
  int tab = 0;
  List<Map<String, dynamic>> get companies => companyState.companies;
  List<Map<String, dynamic>> get watchlist => watchlistState.watchlist;
  Map<String, dynamic>? get portfolio => portfolioState.portfolio;
  List<Map<String, dynamic>> get transactions => portfolioState.transactions;

  bool get signedIn => auth.signedIn;

  Future<void> _clearSession() async {
    auth.clear();
    portfolioState.clear();
    watchlistState.replace([]);
    notifyListeners();
  }

  Future<void> restorePreferences() async {
    final section = await preferences.loadLastSection();
    const sections = ['Research', 'Watchlist', 'Portfolio'];
    final restoredTab = sections.indexOf(section ?? '');
    if (restoredTab >= 0) {
      tab = restoredTab;
      notifyListeners();
    }
  }

  String companyLabel(dynamic companyId) {
    final matches = companies.where((company) => company['id'] == companyId);
    return matches.isEmpty
        ? 'Company #$companyId'
        : matches.first['symbol'] as String;
  }

  Future<void> restoreSession() async {
    final token = await _storage.read('equilens_token');
    if (token == null) return;
    dynamic me;
    try {
      me = jsonDecode((await portfolioApi.request('/auth/me')).body);
    } catch (exception) {
      if (exception is UnauthorizedException) {
        await _storage.delete('equilens_token');
        auth.clear();
      } else {
        error =
            'Unable to verify the saved session. The token was kept; try again when the service is reachable.';
      }
      notifyListeners();
      return;
    }
    email = me['email'] as String?;
    notifyListeners();
    try {
      await refresh();
    } catch (exception) {
      error = exception.toString();
    }
    notifyListeners();
  }

  Future<void> authenticate(String address, String password,
      {bool register = false}) async {
    busy = true;
    error = null;
    notifyListeners();
    try {
      final endpoint = register ? '/auth/register' : '/auth/login';
      final response = await portfolioApi.request(endpoint,
          method: 'POST',
          auth: false,
          body: {'email': address.trim(), 'password': password});
      final result = jsonDecode(response.body) as Map<String, dynamic>;
      await _storage.write('equilens_token', result['token'] as String);
      email = result['email'] as String;
      notifyListeners();
      try {
        await refresh();
      } catch (exception) {
        error = exception.toString();
      }
    } catch (exception) {
      await _storage.delete('equilens_token');
      email = null;
      error = exception.toString();
    } finally {
      busy = false;
      notifyListeners();
    }
  }

  Future<void> signOut() async {
    await _storage.delete('equilens_token');
    auth.clear();
    portfolioState.clear();
    watchlistState.replace([]);
    notifyListeners();
  }

  Future<void> refresh() async {
    final results = await Future.wait([
      research.request('/companies', auth: false),
      portfolioApi.request('/watchlist'),
      portfolioApi.request('/portfolio/summary'),
      portfolioApi.request('/portfolio/transactions'),
    ]);
    companyState.replace(
        (jsonDecode(results[0].body) as List).cast<Map<String, dynamic>>());
    watchlistState.replace(
        (jsonDecode(results[1].body) as List).cast<Map<String, dynamic>>());
    portfolioState.replace(
      summary: jsonDecode(results[2].body) as Map<String, dynamic>,
      history:
          (jsonDecode(results[3].body) as List).cast<Map<String, dynamic>>(),
    );
    notifyListeners();
  }

  Future<void> toggleWatchlist(int companyId) async {
    final exists = watchlist.any((item) => item['companyId'] == companyId);
    await portfolioApi.request('/watchlist/$companyId',
        method: exists ? 'DELETE' : 'POST');
    await refresh();
  }

  Future<void> createTransaction(
      {required int companyId,
      required String type,
      required double quantity,
      required double price}) async {
    await portfolioApi.request('/portfolio/transactions',
        method: 'POST',
        body: {
          'companyId': companyId,
          'type': type,
          'quantity': quantity,
          'price': price
        });
    await refresh();
  }

  Future<void> selectTab(int value) async {
    tab = value;
    notifyListeners();
    await preferences
        .saveLastSection(['Research', 'Watchlist', 'Portfolio'][value]);
  }

  @override
  void dispose() {
    auth.dispose();
    companyState.dispose();
    watchlistState.dispose();
    portfolioState.dispose();
    super.dispose();
  }
}

class EquiLensApp extends StatelessWidget {
  const EquiLensApp({super.key});
  @override
  Widget build(BuildContext context) => MaterialApp(
        title: 'EquiLens',
        theme: ThemeData(
            colorSchemeSeed: const Color(0xff177b70), useMaterial3: true),
        home: Consumer<EquiLensState>(
            builder: (context, state, _) =>
                state.signedIn ? const HomeScreen() : const AuthScreen()),
      );
}

class AuthScreen extends StatefulWidget {
  const AuthScreen({super.key});
  @override
  State<AuthScreen> createState() => _AuthScreenState();
}

class _AuthScreenState extends State<AuthScreen> {
  final email = TextEditingController();
  final password = TextEditingController();
  bool register = false;
  @override
  void dispose() {
    email.dispose();
    password.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
      body: Center(
          child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Padding(
                  padding: const EdgeInsets.all(24),
                  child: Consumer<EquiLensState>(
                      builder: (context, state, _) =>
                          Column(mainAxisSize: MainAxisSize.min, children: [
                            const Text('EquiLens',
                                style: TextStyle(
                                    fontSize: 32, fontWeight: FontWeight.bold)),
                            const SizedBox(height: 24),
                            TextField(
                                controller: email,
                                keyboardType: TextInputType.emailAddress,
                                decoration:
                                    const InputDecoration(labelText: 'Email')),
                            TextField(
                                controller: password,
                                obscureText: true,
                                decoration: const InputDecoration(
                                    labelText: 'Password')),
                            if (state.error != null)
                              Text(state.error!,
                                  style: const TextStyle(color: Colors.red)),
                            const SizedBox(height: 12),
                            FilledButton(
                                onPressed: state.busy
                                    ? null
                                    : () => state.authenticate(
                                        email.text, password.text,
                                        register: register),
                                child: Text(state.busy
                                    ? 'Please wait…'
                                    : register
                                        ? 'Create account'
                                        : 'Sign in')),
                            TextButton(
                                onPressed: () =>
                                    setState(() => register = !register),
                                child: Text(register
                                    ? 'Already registered? Sign in'
                                    : 'Create an account'))
                          ]))))));
}

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});
  @override
  Widget build(BuildContext context) =>
      Consumer<EquiLensState>(builder: (context, state, _) {
        final pages = [
          ResearchPage(state: state),
          WatchlistPage(state: state),
          PortfolioPage(state: state)
        ];
        return Scaffold(
            appBar: AppBar(title: const Text('EquiLens'), actions: [
              IconButton(
                  onPressed: state.signOut, icon: const Icon(Icons.logout))
            ]),
            body: RefreshIndicator(
                onRefresh: state.refresh, child: pages[state.tab]),
            floatingActionButton: state.tab == 2
                ? FloatingActionButton.extended(
                    onPressed: () => showDialog(
                        context: context,
                        builder: (_) => TransactionDialog(state: state)),
                    icon: const Icon(Icons.add),
                    label: const Text('Transaction'))
                : null,
            bottomNavigationBar: NavigationBar(
                selectedIndex: state.tab,
                onDestinationSelected: state.selectTab,
                destinations: const [
                  NavigationDestination(
                      icon: Icon(Icons.search), label: 'Research'),
                  NavigationDestination(
                      icon: Icon(Icons.star), label: 'Watchlist'),
                  NavigationDestination(
                      icon: Icon(Icons.account_balance_wallet),
                      label: 'Portfolio')
                ]));
      });
}

class ResearchPage extends StatelessWidget {
  const ResearchPage({required this.state, super.key});
  final EquiLensState state;
  @override
  Widget build(BuildContext context) => ListView(
      children: state.companies
          .map((company) => ListTile(
              title: Text('${company['symbol']} · ${company['name']}'),
              subtitle: Text(
                  '₹${company['currentPrice']} · P/E ${company['peRatio']} · ${company['dataClassification'] == 'SYNTHETIC_DEMO' ? 'Synthetic demo' : 'Source unverified'}'),
              onTap: () async {
                final id = company['id'] as int;
                try {
                  final details = Map<String, dynamic>.from(jsonDecode(
                      (await state.research
                              .request('/research/companies/$id', auth: false))
                          .body) as Map);
                  Map<String, dynamic>? signal;
                  try {
                    signal = Map<String, dynamic>.from(jsonDecode((await state
                            .research
                            .request('/research/companies/$id/signal',
                                auth: false))
                        .body) as Map);
                  } catch (_) {
                    // Company research remains available without a signal when
                    // financial statements or the ML service are unavailable.
                  }
                  final relative = Map<String, dynamic>.from(jsonDecode(
                      (await state.research.request('/valuation/relative',
                              method: 'POST',
                              auth: false,
                              body: {'companyId': id}))
                          .body) as Map);
                  if (!context.mounted) return;
                  final latest = details['latestFinancials'] as Map?;
                  final history =
                      (details['historicalFinancials'] as List? ?? [])
                          .cast<Map<String, dynamic>>()
                          .toList()
                          .reversed;
                  final metrics = details['metrics'] as Map? ?? {};
                  final valuationMetrics = (relative['metrics'] as List? ?? [])
                      .cast<Map<String, dynamic>>();
                  final probability = signal?['model']?['probability'];
                  showDialog(
                      context: context,
                      builder: (_) => AlertDialog(
                              title: Text('${company['symbol']} research'),
                              content: SizedBox(
                                width: 420,
                                child: SingleChildScrollView(
                                  child: Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    mainAxisSize: MainAxisSize.min,
                                    children: [
                                      Text(
                                          'Latest FY ${latest?['fiscalYear'] ?? 'not reported'}'),
                                      Text(
                                          'Revenue growth: ${metrics['revenueGrowth'] ?? 'N/A'}%'),
                                      Text(
                                          'Net profit growth: ${metrics['profitGrowth'] ?? 'N/A'}%'),
                                      Text(
                                          'Operating margin: ${metrics['operatingMargin'] ?? 'N/A'}%'),
                                      Text('ROE: ${metrics['roe'] ?? 'N/A'}%'),
                                      const SizedBox(height: 12),
                                      const Text('Latest financial statement',
                                          style: TextStyle(
                                              fontWeight: FontWeight.bold)),
                                      if (latest == null)
                                        const Text(
                                            'No financial statements are stored for this company.')
                                      else ...[
                                        Text('Revenue: ₹${latest['revenue']}'),
                                        Text(
                                            'Operating profit: ₹${latest['operatingProfit']}'),
                                        Text(
                                            'Net profit: ₹${latest['netProfit']}'),
                                        Text(
                                            'EPS: ₹${latest['eps']} · FCF: ₹${latest['freeCashFlow']}'),
                                      ],
                                      const SizedBox(height: 12),
                                      const Text('Historical statements',
                                          style: TextStyle(
                                              fontWeight: FontWeight.bold)),
                                      if (history.isEmpty)
                                        const Text(
                                            'No historical statements available.')
                                      else
                                        ...history.map((year) => Text(
                                            "FY ${year['fiscalYear']}: revenue ₹${year['revenue']} · net profit ₹${year['netProfit']} · EPS ₹${year['eps']}")),
                                      const SizedBox(height: 12),
                                      const Text('Relative valuation summary',
                                          style: TextStyle(
                                              fontWeight: FontWeight.bold)),
                                      ...valuationMetrics.map((metric) => Text(
                                          '${metric['multiple']}: ${metric['available'] == true ? metric['companyValue'] : metric['note']}')),
                                      const SizedBox(height: 12),
                                      Text(
                                          'Research signal: ${signal?['model']?['signal'] ?? 'Unavailable'}${probability is num ? ' (${(probability * 100).toStringAsFixed(1)}% model probability)' : ''}'),
                                      if (signal?['model']?['datasetType'] !=
                                          null)
                                        Text(
                                            'Model dataset: ${signal?['model']?['datasetType']}'),
                                    ],
                                  ),
                                ),
                              ),
                              actions: [
                                TextButton(
                                    onPressed: () => Navigator.pop(context),
                                    child: const Text('Close')),
                                TextButton(
                                    onPressed: () async {
                                      try {
                                        await state.toggleWatchlist(id);
                                        if (context.mounted) {
                                          Navigator.pop(context);
                                        }
                                      } catch (error) {
                                        if (context.mounted) {
                                          ScaffoldMessenger.of(context)
                                              .showSnackBar(SnackBar(
                                                  content:
                                                      Text(error.toString())));
                                        }
                                      }
                                    },
                                    child: const Text('Watch'))
                              ]));
                } catch (error) {
                  if (context.mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(content: Text(error.toString())));
                  }
                }
              }))
          .toList());
}

class WatchlistPage extends StatelessWidget {
  const WatchlistPage({required this.state, super.key});
  final EquiLensState state;
  @override
  Widget build(BuildContext context) {
    final ids = state.watchlist.map((item) => item['companyId']).toSet();
    final items =
        state.companies.where((company) => ids.contains(company['id']));
    return ListView(
        children: items
            .map((company) => ListTile(
                title: Text('${company['symbol']} · ${company['name']}'),
                trailing: IconButton(
                    icon: const Icon(Icons.star),
                    onPressed: () =>
                        state.toggleWatchlist(company['id'] as int))))
            .toList());
  }
}

class PortfolioPage extends StatelessWidget {
  const PortfolioPage({required this.state, super.key});
  final EquiLensState state;
  @override
  Widget build(BuildContext context) {
    final data = state.portfolio ?? {};
    return ListView(padding: const EdgeInsets.all(16), children: [
      Text('Market value  ₹${data['marketValue'] ?? 0}',
          style: Theme.of(context).textTheme.titleLarge),
      Text('Invested  ₹${data['investedValue'] ?? 0}'),
      Text('Unrealized P&L  ₹${data['unrealizedPnl'] ?? 0}'),
      const Divider(),
      ...((data['positions'] as List? ?? []).map((position) => ListTile(
            title: Text('${position['symbol']} · ${position['name']}'),
            subtitle: Text(
                '${position['quantity']} shares · avg ₹${position['averageCost']} · P&L ₹${position['unrealizedPnl']}'),
          ))),
      const Divider(),
      const Text('Transactions', style: TextStyle(fontWeight: FontWeight.bold)),
      ...state.transactions.map((item) {
        final date =
            DateTime.tryParse('${item['transactionDate'] ?? ''}')?.toLocal();
        return ListTile(
          title:
              Text('${item['type']} ${item['quantity']} × ₹${item['price']}'),
          subtitle: Text(
              '${state.companyLabel(item['companyId'])} · ${date?.toString() ?? 'Date unavailable'}'),
        );
      }),
    ]);
  }
}

class TransactionDialog extends StatefulWidget {
  const TransactionDialog({required this.state, super.key});
  final EquiLensState state;
  @override
  State<TransactionDialog> createState() => _TransactionDialogState();
}

class _TransactionDialogState extends State<TransactionDialog> {
  int? companyId;
  String type = 'BUY';
  final quantity = TextEditingController();
  final price = TextEditingController();
  String? error;
  @override
  void dispose() {
    quantity.dispose();
    price.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => AlertDialog(
        title: const Text('Add portfolio transaction'),
        content: Column(mainAxisSize: MainAxisSize.min, children: [
          DropdownButtonFormField<int>(
              decoration: const InputDecoration(labelText: 'Company'),
              initialValue: companyId,
              items: widget.state.companies
                  .map((company) => DropdownMenuItem(
                      value: company['id'] as int,
                      child: Text(company['symbol'] as String)))
                  .toList(),
              onChanged: (value) => setState(() => companyId = value)),
          DropdownButtonFormField<String>(
              decoration: const InputDecoration(labelText: 'Type'),
              initialValue: type,
              items: const [
                DropdownMenuItem(value: 'BUY', child: Text('Buy')),
                DropdownMenuItem(value: 'SELL', child: Text('Sell'))
              ],
              onChanged: (value) => setState(() => type = value ?? 'BUY')),
          TextField(
              controller: quantity,
              keyboardType:
                  const TextInputType.numberWithOptions(decimal: true),
              decoration: const InputDecoration(labelText: 'Quantity')),
          TextField(
              controller: price,
              keyboardType:
                  const TextInputType.numberWithOptions(decimal: true),
              decoration:
                  const InputDecoration(labelText: 'Price per share (₹)')),
          if (error != null)
            Text(error!, style: const TextStyle(color: Colors.red)),
        ]),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('Cancel')),
          FilledButton(
              onPressed: () async {
                try {
                  if (companyId == null) {
                    throw const HttpException('Select a company');
                  }
                  await widget.state.createTransaction(
                      companyId: companyId!,
                      type: type,
                      quantity: double.parse(quantity.text),
                      price: double.parse(price.text));
                  if (context.mounted) Navigator.pop(context);
                } catch (exception) {
                  setState(() => error = exception.toString());
                }
              },
              child: const Text('Save'))
        ],
      );
}
