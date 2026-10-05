import 'dart:async';

import 'package:http/http.dart' as http;
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:equilens_mobile/main.dart';

void main() {
  test('keeps a saved token when session verification is offline', () async {
    final store = MemoryTokenStore()..values['equilens_token'] = 'still-valid';
    final state = EquiLensState(
      firebaseReady: false,
      tokenStore: store,
      portfolioApi: FailingApiClient(TimeoutException('offline'), store),
    );

    await state.restoreSession();

    expect(store.values['equilens_token'], 'still-valid');
    expect(state.signedIn, isFalse);
    expect(state.error, contains('token was kept'));
    state.dispose();
  });

  test('clears a saved token when session verification returns unauthorized',
      () async {
    final store = MemoryTokenStore()..values['equilens_token'] = 'expired';
    final state = EquiLensState(
      firebaseReady: false,
      tokenStore: store,
      portfolioApi: FailingApiClient(
          const UnauthorizedException('Session expired'), store),
    );

    await state.restoreSession();

    expect(store.values, isEmpty);
    expect(state.signedIn, isFalse);
    expect(state.error, isNull);
    state.dispose();
  });

  test('auth state records and clears the backend session', () {
    final state = AuthState()
      ..email = 'investor@example.com'
      ..error = 'previous error';

    expect(state.signedIn, isTrue);
    state.clear();
    expect(state.signedIn, isFalse);
    expect(state.error, isNull);

    state
      ..email = 'investor@example.com'
      ..error = 'stale backend error';
    state.clear();
    expect(state.email, isNull);
    expect(state.error, isNull);
    state.dispose();
  });

  test('company and watchlist states replace their own collections', () {
    final companies = CompanyState();
    final watchlist = WatchlistState();

    companies.replace([
      {'id': 7, 'symbol': 'INFY'}
    ]);
    watchlist.replace([
      {'companyId': 7}
    ]);

    expect(companies.companies.single['symbol'], 'INFY');
    expect(watchlist.watchlist.single['companyId'], 7);
    companies.dispose();
    watchlist.dispose();
  });

  test('portfolio state replaces summary and transaction history together', () {
    final state = PortfolioState();
    state.replace(
      summary: {'marketValue': 10000},
      history: [
        {'type': 'BUY', 'quantity': 5}
      ],
    );

    expect(state.portfolio?['marketValue'], 10000);
    expect(state.transactions.single['type'], 'BUY');
    state.clear();
    expect(state.portfolio, isNull);
    expect(state.transactions, isEmpty);
    state.dispose();
  });

  testWidgets('shows the sign-in screen when there is no backend session',
      (tester) async {
    await tester.pumpWidget(ChangeNotifierProvider(
      create: (_) => EquiLensState(firebaseReady: false),
      child: const EquiLensApp(),
    ));

    expect(find.text('EquiLens'), findsOneWidget);
    expect(find.text('Sign in'), findsOneWidget);
  });
}

class MemoryTokenStore implements TokenStore {
  final Map<String, String> values = {};

  @override
  Future<String?> read(String key) async => values[key];

  @override
  Future<void> write(String key, String value) async => values[key] = value;

  @override
  Future<void> delete(String key) async => values.remove(key);
}

class FailingApiClient extends ApiClient {
  FailingApiClient(this.failure, TokenStore store)
      : super('http://test', tokenStore: store);

  final Object failure;

  @override
  Future<http.Response> request(String path,
      {String method = 'GET', Object? body, bool auth = true}) async {
    throw failure;
  }
}
