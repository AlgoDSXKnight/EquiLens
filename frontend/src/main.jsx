import React, { createContext, lazy, Suspense, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { API_BASE, COMPANIES_API, PORTFOLIO_API, TOKEN_KEY, USER_KEY, apiFetch, publicApiFetch } from './services/api.js';
import './styles.css';

const ResearchCharts = lazy(() => import('./ResearchCharts.jsx'));

const AuthContext = createContext(null);

function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error('useAuth must be used inside AuthContext.Provider');
  return value;
}


/* =========================================================
   APP
========================================================= */

function App() {

  const [user, setUser] =
    useState(null);

  const [authChecking, setAuthChecking] =
    useState(true);

  const [authMode, setAuthMode] =
    useState('login');

  useEffect(() => {
    const handleUnauthorized = () => setUser(null);
    window.addEventListener('equilens:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('equilens:unauthorized', handleUnauthorized);
  }, []);


  /* =======================================================
     RESTORE LOGIN SESSION
  ======================================================= */

  useEffect(() => {

    const token =
      localStorage.getItem(TOKEN_KEY);


    if (!token) {

      setAuthChecking(false);
      return;

    }


    apiFetch(`${PORTFOLIO_API}/auth/me`)
      .then(async response => {

        if (!response.ok) {
          throw new Error(
            'Session invalid'
          );
        }

        return response.json();

      })
      .then(currentUser => {

        setUser(currentUser);

        localStorage.setItem(
          USER_KEY,
          JSON.stringify(currentUser)
        );

      })
      .catch(() => {

        localStorage.removeItem(
          TOKEN_KEY
        );

        localStorage.removeItem(
          USER_KEY
        );

        setUser(null);

      })
      .finally(() => {

        setAuthChecking(false);

      });

  }, []);


  /* =======================================================
     LOGIN
  ======================================================= */

  async function login(email, password) {

    const response =
      await publicApiFetch(
        `${PORTFOLIO_API}/auth/login`,
        {
          method: 'POST',

          headers: {
            'Content-Type':
              'application/json'
          },

          body: JSON.stringify({
            email,
            password
          })
        }
      );


    const data =
      await response.json();


    if (!response.ok) {

      throw new Error(
        data.message || data.error ||
        'Invalid email or password.'
      );

    }


    localStorage.setItem(
      TOKEN_KEY,
      data.token
    );


    const currentUser = {
      id: data.userId,
      name: data.name || data.email,
      email: data.email,
      role: data.role
    };


    localStorage.setItem(
      USER_KEY,
      JSON.stringify(currentUser)
    );


    setUser(currentUser);

  }


  /* =======================================================
     REGISTER
  ======================================================= */

  async function register(
    email,
    password
  ) {

    const response =
      await publicApiFetch(
        `${PORTFOLIO_API}/auth/register`,
        {
          method: 'POST',

          headers: {
            'Content-Type':
              'application/json'
          },

          body: JSON.stringify({
            email,
            password
          })
        }
      );


    const data =
      await response.json();


    if (!response.ok) {

      throw new Error(
        data.message || data.error ||
        'Registration failed.'
      );

    }


    localStorage.setItem(
      TOKEN_KEY,
      data.token
    );


    const currentUser = {
      id: data.userId,
      name: data.name || data.email,
      email: data.email,
      role: data.role
    };


    localStorage.setItem(
      USER_KEY,
      JSON.stringify(currentUser)
    );


    setUser(currentUser);

  }


  /* =======================================================
     LOGOUT
  ======================================================= */

  function logout() {

    localStorage.removeItem(
      TOKEN_KEY
    );

    localStorage.removeItem(
      USER_KEY
    );

    setUser(null);

  }


  /* =======================================================
     AUTH CHECK
  ======================================================= */

  if (authChecking) {

    return (
      <div className="auth-page">

        <div className="auth-card">

          <div className="brand auth-brand">
            Equi<span>Lens</span>
          </div>

          <p className="muted">
            Restoring your session...
          </p>

        </div>

      </div>
    );

  }


  /* =======================================================
     SHOW LOGIN / REGISTER
  ======================================================= */

  if (!user) {

    return (
      <AuthScreen
        mode={authMode}
        setMode={setAuthMode}
        onLogin={login}
        onRegister={register}
      />
    );

  }


  /* =======================================================
     MAIN APPLICATION
  ======================================================= */

  return (
    <AuthContext.Provider value={{ user, onLogout: logout }}>
      <AuthenticatedApp />
    </AuthContext.Provider>
  );
}


/* =========================================================
   AUTH SCREEN
========================================================= */

function AuthScreen({
  mode,
  setMode,
  onLogin,
  onRegister
}) {

  const [email, setEmail] =
    useState('');

  const [password, setPassword] =
    useState('');

  const [confirmPassword, setConfirmPassword] =
    useState('');

  const [loading, setLoading] =
    useState(false);

  const [error, setError] =
    useState('');

  const isLogin =
    mode === 'login';


  async function handleSubmit(event) {

    event.preventDefault();

    setError('');


    if (!email.trim()) {

      setError(
        'Please enter your email.'
      );

      return;

    }


    if (!password) {

      setError(
        'Please enter your password.'
      );

      return;

    }


    if (!isLogin) {

      if (password.length < 6) {

        setError(
          'Password must contain at least 6 characters.'
        );

        return;

      }


      if (
        password !== confirmPassword
      ) {

        setError(
          'Passwords do not match.'
        );

        return;

      }

    }


    setLoading(true);


    try {

      if (isLogin) {

        await onLogin(
          email.trim(),
          password
        );

      } else {

        await onRegister(
          email.trim(),
          password
        );

      }

    } catch (error) {

      console.error(
        'Authentication error:',
        error
      );

      setError(
        error.message ||
        'Something went wrong.'
      );

    } finally {

      setLoading(false);

    }

  }


  function switchMode() {

    setError('');

    setPassword('');
    setConfirmPassword('');

    setMode(
      isLogin
        ? 'register'
        : 'login'
    );

  }


  return (
    <div className="auth-page">

      <div className="auth-card">

        <div className="brand auth-brand">
          Equi<span>Lens</span>
        </div>


        <p className="eyebrow">
          EQUITY RESEARCH PLATFORM
        </p>


        <h1>
          {isLogin
            ? 'Welcome back.'
            : 'Create your account.'}
        </h1>


        <p className="muted auth-description">

          {isLogin
            ? 'Sign in to continue your research workspace.'
            : 'Create your EquiLens account and start researching companies.'}

        </p>


        {error && (

          <div className="error auth-error">
            {error}
          </div>

        )}


        <form
          className="auth-form"
          onSubmit={handleSubmit}
        >

          <div className="auth-field">

            <label>
              Email
            </label>

            <input
              type="email"
              placeholder="you@example.com"
              value={email}
              onChange={event =>
                setEmail(event.target.value)
              }
              autoComplete="email"
            />

          </div>


          <div className="auth-field">

            <label>
              Password
            </label>

            <input
              type="password"
              placeholder="••••••••"
              value={password}
              onChange={event =>
                setPassword(event.target.value)
              }
              autoComplete={
                isLogin
                  ? 'current-password'
                  : 'new-password'
              }
            />

          </div>


          {!isLogin && (

            <div className="auth-field">

              <label>
                Confirm Password
              </label>

              <input
                type="password"
                placeholder="••••••••"
                value={confirmPassword}
                onChange={event =>
                  setConfirmPassword(
                    event.target.value
                  )
                }
                autoComplete="new-password"
              />

            </div>

          )}


          <button
            className="auth-submit"
            type="submit"
            disabled={loading}
          >

            {loading
              ? 'Please wait...'
              : isLogin
                ? 'Sign In'
                : 'Create Account'}

          </button>

        </form>


        <div className="auth-switch">

          <span>
            {isLogin
              ? "Don't have an account?"
              : 'Already have an account?'}
          </span>


          <button
            type="button"
            onClick={switchMode}
          >

            {isLogin
              ? 'Create account'
              : 'Sign in'}

          </button>

        </div>

      </div>

    </div>
  );
}


/* =========================================================
   AUTHENTICATED APPLICATION
========================================================= */

function AuthenticatedApp() {

  const { user, onLogout } = useAuth();

  const [companies, setCompanies] =
    useState([]);

  const [watchlist, setWatchlist] =
    useState([]);

  const [selected, setSelected] =
    useState(null);

  const [search, setSearch] =
    useState('');

  const [view, setView] =
    useState('dashboard');

  const [loading, setLoading] =
    useState(true);

  const [watchlistLoading, setWatchlistLoading] =
    useState(true);

  const [error, setError] =
    useState('');

  const [watchlistError, setWatchlistError] =
    useState('');


  /* =======================================================
     PORTFOLIO STATE
  ======================================================= */

  const [portfolio, setPortfolio] =
    useState(null);

  const [portfolioLoading, setPortfolioLoading] =
    useState(false);

  const [portfolioError, setPortfolioError] =
    useState('');

  const [portfolioActionLoading, setPortfolioActionLoading] =
    useState(false);


  /* =======================================================
     LOAD COMPANIES
  ======================================================= */

  useEffect(() => {

    apiFetch(COMPANIES_API)
      .then(async response => {

        if (!response.ok) {
          throw new Error();
        }

        return response.json();

      })
      .then(data => {

        setCompanies(data);
        setLoading(false);

      })
      .catch(error => {

        console.error(
          'Failed to load companies:',
          error
        );

        if (
          error.message ===
          'SESSION_EXPIRED'
        ) {

          return;

        }

        setError(
          'Unable to connect to the EquiLens backend.'
        );

        setLoading(false);

      });

  }, []);


  /* =======================================================
     LOAD USER WATCHLIST
  ======================================================= */

  useEffect(() => {

    loadWatchlist();

  }, []);


  async function loadWatchlist() {

    setWatchlistLoading(true);
    setWatchlistError('');


    try {

      const response =
        await apiFetch(
          `${PORTFOLIO_API}/watchlist`
        );


      if (!response.ok) {

        throw new Error(
          `Watchlist API returned ${response.status}`
        );

      }


      const data =
        await response.json();


      setWatchlist(
        Array.isArray(data)
          ? data
          : []
      );

    } catch (error) {

      console.error(
        'Failed to load watchlist:',
        error
      );


      if (
        error.message ===
        'SESSION_EXPIRED'
      ) {

        return;

      }


      setWatchlistError(
        'Unable to load your watchlist.'
      );

    } finally {

      setWatchlistLoading(false);

    }

  }


  /* =======================================================
     LOAD PORTFOLIO
  ======================================================= */

  async function loadPortfolio() {

    setPortfolioLoading(true);
    setPortfolioError('');


    try {

      const response =
        await apiFetch(
          `${PORTFOLIO_API}/portfolio/summary`
        );


      if (!response.ok) {

        throw new Error(
          `Portfolio API returned ${response.status}`
        );

      }


      const data = await response.json();
      const transactionsResponse = await apiFetch(`${PORTFOLIO_API}/portfolio/transactions`);
      if (!transactionsResponse.ok) throw new Error(`Transaction API returned ${transactionsResponse.status}`);
      const transactions = await transactionsResponse.json();
      setPortfolio({
        ...data,
        holdings: (data.positions || []).map(position => ({
          ...position,
          companyName: position.name,
          profitLoss: position.unrealizedPnl,
          profitLossPercent: position.pnlPercent
        })),
        invested: data.investedValue,
        profitLoss: data.unrealizedPnl,
        profitLossPercent: data.pnlPercent,
        transactions
      });

    } catch (error) {

      console.error(
        'Failed to load portfolio:',
        error
      );


      if (
        error.message ===
        'SESSION_EXPIRED'
      ) {

        return;

      }


      setPortfolioError(
        'Unable to load your portfolio.'
      );

    } finally {

      setPortfolioLoading(false);

    }

  }


  /* =======================================================
     LOAD PORTFOLIO WHEN PAGE OPENS
  ======================================================= */

  useEffect(() => {

    if (view === 'portfolio') {
      loadPortfolio();
    }

  }, [view]);


  /* =======================================================
     ADD PORTFOLIO TRANSACTION
  ======================================================= */

  async function addPortfolioTransaction({
    companyId,
    type,
    quantity,
    price
  }) {

    setPortfolioActionLoading(true);
    setPortfolioError('');


    try {

      const response =
        await apiFetch(
          `${PORTFOLIO_API}/portfolio/transactions`,
          {
            method: 'POST',
            body: JSON.stringify({
              companyId,
              type,
              quantity,
              price
            })
          }
        );


      if (!response.ok) {

        let message =
          'Unable to save portfolio transaction.';

        try {

          const data =
            await response.json();

          message = data?.message || data?.error || message;

        } catch {
          // Backend may return an empty error body.
        }


        throw new Error(message);

      }


      await response.json();

      await loadPortfolio();

    } catch (error) {

      console.error(
        'Portfolio transaction failed:',
        error
      );


      if (
        error.message ===
        'SESSION_EXPIRED'
      ) {

        return;

      }


      setPortfolioError(
        error.message ||
        'Unable to save portfolio transaction.'
      );

      throw error;

    } finally {

      setPortfolioActionLoading(false);

    }

  }


  /* =======================================================
     WATCHLIST STATUS
  ======================================================= */

  function isWatched(companyId) {

    return watchlist.some(
      item =>
        item.companyId === companyId
    );

  }


  /* =======================================================
     ADD / REMOVE WATCHLIST
  ======================================================= */

  async function toggleWatchlist(company) {

    if (!company?.id) {
      return;
    }


    const watched =
      isWatched(company.id);


    try {

      if (watched) {

        const response =
          await apiFetch(
            `${PORTFOLIO_API}/watchlist/${company.id}`,
            {
              method: 'DELETE'
            }
          );


        if (
          !response.ok &&
          response.status !== 204
        ) {

          throw new Error(
            `Failed to remove ${company.symbol} from watchlist`
          );

        }


        setWatchlist(current =>
          current.filter(
            item =>
              item.companyId !== company.id
          )
        );

      } else {

        const response =
          await apiFetch(
            `${PORTFOLIO_API}/watchlist/${company.id}`,
            {
              method: 'POST'
            }
          );


        if (!response.ok) {

          const data =
            await response.json()
              .catch(() => null);


          throw new Error(
            data?.message || data?.error ||
            `Failed to add ${company.symbol} to watchlist`
          );

        }


        const addedCompany =
          await response.json();


        setWatchlist(current => {

          const alreadyExists =
            current.some(
              item =>
                item.companyId === addedCompany.companyId
            );


          if (alreadyExists) {
            return current;
          }


          return [
            addedCompany,
            ...current
          ];

        });

      }

    } catch (error) {

      console.error(
        'Watchlist update failed:',
        error
      );


      setWatchlistError(
        error.message ||
        'Unable to update watchlist.'
      );

    }

  }


  /* =======================================================
     SEARCH
  ======================================================= */

  const filteredCompanies =
    useMemo(() => {

      const query =
        search.trim().toLowerCase();


      if (!query) {
        return companies;
      }


      return companies.filter(
        company =>
          company.symbol
            ?.toLowerCase()
            .includes(query) ||

          company.name
            ?.toLowerCase()
            .includes(query) ||

          company.sector
            ?.toLowerCase()
            .includes(query)
      );

    }, [companies, search]);


  /* =======================================================
     NAVIGATION
  ======================================================= */

  function openResearch(company) {

    setSelected(company);
    setView('research');

  }


  function goDashboard() {

    setView('dashboard');
    setSelected(null);

  }


  function goWatchlist() {

    setView('watchlist');
    setSelected(null);

  }


  function goPortfolio() {

    setView('portfolio');
    setSelected(null);

  }


  return (
    <div className="app">

      <aside>

        <div className="brand">
          Equi<span>Lens</span>
        </div>


        <nav>

          <button
            className={
              view === 'dashboard'
                ? 'nav-active'
                : ''
            }
            onClick={goDashboard}
          >
            Dashboard
          </button>


          <button
            className={
              view === 'research'
                ? 'nav-active'
                : ''
            }
            onClick={() =>
              selected &&
              setView('research')
            }
          >
            Research
          </button>


          <button
            className={
              view === 'watchlist'
                ? 'nav-active'
                : ''
            }
            onClick={goWatchlist}
          >
            Watchlist

            {watchlist.length > 0 && (
              <span className="nav-count">
                {watchlist.length}
              </span>
            )}

          </button>


          <button
            className={
              view === 'portfolio'
                ? 'nav-active'
                : ''
            }
            onClick={goPortfolio}
          >
            Portfolio
          </button>

        </nav>


        <div className="side-user">

          <div className="side-user-name">
            {user.name}
          </div>

          <div className="side-user-email">
            {user.email}
          </div>

          <button
            className="logout-button"
            onClick={onLogout}
          >
            Logout
          </button>

        </div>


        <div className="side-note">
          Equity Research
          <br />
          Platform v0.1
        </div>

      </aside>


      <main>

        {view === 'dashboard' ? (

          <Dashboard
            companies={filteredCompanies}
            totalCompanies={companies.length}
            search={search}
            setSearch={setSearch}
            loading={loading}
            error={error}
            onCompanyClick={openResearch}
            watchlist={watchlist}
            onToggleWatchlist={toggleWatchlist}
          />

        ) : view === 'watchlist' ? (

          <Watchlist
            companies={companies.filter(company => watchlist.some(item => item.companyId === company.id))}
            loading={watchlistLoading}
            error={watchlistError}
            onCompanyClick={openResearch}
            onToggleWatchlist={toggleWatchlist}
          />

        ) : view === 'portfolio' ? (

          <Portfolio
            companies={companies}
            portfolio={portfolio}
            loading={portfolioLoading}
            actionLoading={portfolioActionLoading}
            error={portfolioError}
            onAddTransaction={
              addPortfolioTransaction
            }
            onRefresh={loadPortfolio}
          />

        ) : (

          <Research
            company={selected}
            onBack={goDashboard}
            isWatched={
              selected
                ? isWatched(selected.id)
                : false
            }
            onToggleWatchlist={
              toggleWatchlist
            }
          />

        )}

      </main>

    </div>
  );
}


/* =========================================================
   DASHBOARD
========================================================= */

function Dashboard({
  companies,
  totalCompanies,
  search,
  setSearch,
  loading,
  error,
  onCompanyClick,
  watchlist,
  onToggleWatchlist
}) {

  function isWatched(companyId) {

    return watchlist.some(
      item =>
        item.companyId === companyId
    );

  }


  return (
    <>

      <header>

        <div>
          <p className="eyebrow">
            RESEARCH TERMINAL
          </p>

          <h1>
            Market Intelligence
          </h1>
        </div>


        <div className="search-box">

          <span>
            ⌕
          </span>

          <input
            type="text"
            placeholder="Search company..."
            value={search}
            onChange={event =>
              setSearch(event.target.value)
            }
          />

        </div>

      </header>


      {error && (
        <div className="error">
          {error} Start the Spring Boot backend first.
        </div>
      )}


      <section className="hero">

        <div>

          <p className="muted">
            Welcome to EquiLens
          </p>

          <h2>
            Research companies.
            <br />
            Understand value.
          </h2>

          <p className="muted">
            Fundamentals, valuation and research signals in one workspace.
          </p>

        </div>


        <div className="hero-badge">
          AI RESEARCH
          <br />
          <strong>ENGINE</strong>
        </div>

      </section>


      <div className="section-head">

        <div>

          <h2>
            Companies
          </h2>

          <span>
            {companies.length} of {totalCompanies} tracked
          </span>

        </div>

      </div>


      {loading ? (

        <div className="empty">
          Loading companies...
        </div>

      ) : companies.length === 0 ? (

        <div className="empty">
          No companies available yet.
        </div>

      ) : (

        <section className="grid">

          {companies.map(company => {

            const watched =
              isWatched(company.id);


            return (

              <div
                className="card company-card"
                key={company.id}
                onClick={() =>
                  onCompanyClick(company)
                }
              >

                <button
                  type="button"
                  className={
                    watched
                      ? 'star-button star-active'
                      : 'star-button'
                  }
                  title={
                    watched
                      ? 'Remove from watchlist'
                      : 'Add to watchlist'
                  }
                  onClick={event => {

                    event.stopPropagation();

                    onToggleWatchlist(
                      company
                    );

                  }}
                >
                  {watched ? '★' : '☆'}
                </button>


                <div className="ticker">
                  {company.symbol}
                </div>

                <h3>
                  {company.name}
                </h3>

                <p>
                  {company.sector}
                </p>

                <p className="company-source">
                  {companySourceLabel(company)}
                </p>


                <div className="metrics">

                  <div>
                    <small>
                      PRICE
                    </small>

                    <b>
                      ₹
                      {company.currentPrice?.toLocaleString()}
                    </b>
                  </div>


                  <div>
                    <small>
                      ROE
                    </small>

                    <b>
                      {company.roe}%
                    </b>
                  </div>


                  <div>
                    <small>
                      P/E
                    </small>

                    <b>
                      {company.peRatio}
                    </b>
                  </div>

                </div>


                <div className="card-action">
                  Open Research →
                </div>

              </div>

            );

          })}

        </section>

      )}

    </>
  );
}


/* =========================================================
   WATCHLIST PAGE
========================================================= */

function Watchlist({
  companies,
  loading,
  error,
  onCompanyClick,
  onToggleWatchlist
}) {

  return (
    <>

      <header>

        <div>

          <p className="eyebrow">
            PERSONAL RESEARCH
          </p>

          <h1>
            Watchlist
          </h1>

          <p className="muted">
            Companies you are monitoring.
          </p>

        </div>


        <div className="watchlist-summary">

          <span>
            TRACKED
          </span>

          <strong>
            {companies.length}
          </strong>

        </div>

      </header>


      {error && (

        <div className="error">
          {error}
        </div>

      )}


      {loading ? (

        <div className="empty">
          Loading your watchlist...
        </div>

      ) : companies.length === 0 ? (

        <section className="watchlist-empty">

          <div className="watchlist-empty-icon">
            ☆
          </div>

          <h2>
            Your watchlist is empty
          </h2>

          <p>
            Add companies from the Dashboard
            or Research page to monitor them here.
          </p>

        </section>

      ) : (

        <section className="grid">

          {companies.map(company => (

            <div
              className="card company-card"
              key={company.id}
              onClick={() =>
                onCompanyClick(company)
              }
            >

              <button
                type="button"
                className="star-button star-active"
                title="Remove from watchlist"
                onClick={event => {

                  event.stopPropagation();

                  onToggleWatchlist(
                    company
                  );

                }}
              >
                ★
              </button>


              <div className="ticker">
                {company.symbol}
              </div>

              <h3>
                {company.name}
              </h3>

              <p>
                {company.sector}
              </p>

              <p className="company-source">
                {companySourceLabel(company)}
              </p>


              <div className="metrics">

                <div>
                  <small>
                    PRICE
                  </small>

                  <b>
                    ₹
                    {company.currentPrice?.toLocaleString()}
                  </b>
                </div>


                <div>
                  <small>
                    ROE
                  </small>

                  <b>
                    {company.roe}%
                  </b>
                </div>


                <div>
                  <small>
                    P/E
                  </small>

                  <b>
                    {company.peRatio}
                  </b>
                </div>

              </div>


              <div className="card-action">
                Open Research →
              </div>

            </div>

          ))}

        </section>

      )}

    </>
  );
}


/* =========================================================
   PORTFOLIO PAGE
========================================================= */

function Portfolio({
  companies,
  portfolio,
  loading,
  actionLoading,
  error,
  onAddTransaction,
  onRefresh
}) {

  const [transactionType, setTransactionType] =
    useState('BUY');

  const [selectedCompanyId, setSelectedCompanyId] =
    useState(
      companies.length > 0
        ? String(companies[0].id)
        : ''
    );

  const [quantity, setQuantity] =
    useState('');

  const [price, setPrice] =
    useState('');

  const [formError, setFormError] =
    useState('');

  const [formSuccess, setFormSuccess] =
    useState('');


  /* =======================================================
     KEEP COMPANY SELECTION VALID
  ======================================================= */

  useEffect(() => {

    if (
      companies.length > 0 &&
      !companies.some(
        company =>
          String(company.id) ===
          String(selectedCompanyId)
      )
    ) {

      setSelectedCompanyId(
        String(companies[0].id)
      );

    }

  }, [companies, selectedCompanyId]);


  /* =======================================================
     SELECTED COMPANY
  ======================================================= */

  const selectedCompany =
    companies.find(
      company =>
        String(company.id) ===
        String(selectedCompanyId)
    );


  /* =======================================================
     AUTO-FILL CURRENT PRICE
  ======================================================= */

  useEffect(() => {

    if (
      selectedCompany &&
      transactionType === 'BUY' &&
      !price
    ) {

      setPrice(
        String(selectedCompany.currentPrice)
      );

    }

  }, [
    selectedCompany,
    transactionType,
    price
  ]);


  /* =======================================================
     COMPANY CHANGE
  ======================================================= */

  function handleCompanyChange(event) {

    const companyId =
      event.target.value;

    setSelectedCompanyId(companyId);

    setFormError('');
    setFormSuccess('');


    const company =
      companies.find(
        item =>
          String(item.id) ===
          String(companyId)
      );


    if (company) {

      setPrice(
        String(company.currentPrice)
      );

    }

  }


  /* =======================================================
     TRANSACTION TYPE CHANGE
  ======================================================= */

  function handleTypeChange(type) {

    setTransactionType(type);

    setFormError('');
    setFormSuccess('');


    if (selectedCompany) {

      setPrice(
        String(selectedCompany.currentPrice)
      );

    }

  }


  /* =======================================================
     SUBMIT TRANSACTION
  ======================================================= */

  async function handleSubmit(event) {

    event.preventDefault();

    setFormError('');
    setFormSuccess('');


    const numericQuantity =
      Number(quantity);

    const numericPrice =
      Number(price);


    if (!selectedCompanyId) {

      setFormError(
        'Please select a company.'
      );

      return;

    }


    if (
      !Number.isFinite(numericQuantity) ||
      numericQuantity <= 0
    ) {

      setFormError(
        'Quantity must be greater than zero.'
      );

      return;

    }


    if (
      !Number.isFinite(numericPrice) ||
      numericPrice <= 0
    ) {

      setFormError(
        'Price must be greater than zero.'
      );

      return;

    }


    try {

      await onAddTransaction({
        companyId:
          Number(selectedCompanyId),

        type:
          transactionType,

        quantity:
          numericQuantity,

        price:
          numericPrice
      });


      setQuantity('');

      setFormSuccess(
        `${transactionType} transaction recorded successfully.`
      );

    } catch (error) {

      setFormError(
        error.message ||
        'Unable to save transaction.'
      );

    }

  }


  /* =======================================================
     LOADING
  ======================================================= */

  if (loading && !portfolio) {

    return (
      <div className="empty">
        Loading portfolio...
      </div>
    );

  }


  const holdings =
    portfolio?.holdings || [];


  const transactions =
    portfolio?.transactions || [];


  return (
    <>

      <header>

        <div>

          <p className="eyebrow">
            INVESTMENT PORTFOLIO
          </p>

          <h1>
            Portfolio
          </h1>

          <p className="muted">
            Track holdings, cost basis and portfolio performance.
          </p>

        </div>


        <button
          type="button"
          className="refresh-button"
          onClick={onRefresh}
          disabled={loading}
        >
          {loading
            ? 'Refreshing...'
            : '↻ Refresh'}
        </button>

      </header>


      {error && (

        <div className="error">
          {error}
        </div>

      )}


      {/* =================================================
          PORTFOLIO SUMMARY
      ================================================= */}

      <section className="portfolio-summary">

        <PortfolioSummaryCard
          label="INVESTED"
          value={
            `₹${formatNumber(
              portfolio?.invested || 0
            )}`
          }
        />


        <PortfolioSummaryCard
          label="MARKET VALUE"
          value={
            `₹${formatNumber(
              portfolio?.marketValue || 0
            )}`
          }
        />


        <PortfolioSummaryCard
          label="UNREALIZED P&L"
          value={
            `₹${formatNumber(
              portfolio?.profitLoss || 0
            )}`
          }
          valueClass={
            (portfolio?.profitLoss || 0) >= 0
              ? 'positive'
              : 'negative'
          }
        />


        <PortfolioSummaryCard
          label="P&L %"
          value={
            `${(portfolio?.profitLoss || 0) >= 0
              ? '+'
              : ''
            }${Number(
              portfolio?.profitLossPercent || 0
            ).toFixed(2)
            }%`
          }
          valueClass={
            (portfolio?.profitLoss || 0) >= 0
              ? 'positive'
              : 'negative'
          }
        />

      </section>


      <div className="portfolio-layout">


        {/* =================================================
            HOLDINGS
        ================================================= */}

        <section className="research-panel portfolio-holdings-panel">

          <div className="panel-title">

            <div>

              <p className="eyebrow">
                CURRENT POSITIONS
              </p>

              <h2>
                Holdings
              </h2>

            </div>


            <span className="portfolio-count">
              {holdings.length}
              {' '}
              {holdings.length === 1
                ? 'position'
                : 'positions'}
            </span>

          </div>


          {holdings.length === 0 ? (

            <div className="portfolio-empty">

              <div className="portfolio-empty-icon">
                ◇
              </div>

              <h3>
                No holdings yet
              </h3>

              <p>
                Record your first BUY transaction
                using the form on the right.
              </p>

            </div>

          ) : (

            <div className="holdings-list">

              {holdings.map(
                holding => (

                  <div
                    className="holding-row"
                    key={holding.companyId}
                  >

                    <div className="holding-company">

                      <div className="holding-symbol">
                        {holding.symbol}
                      </div>

                      <div>

                        <strong>
                          {holding.companyName}
                        </strong>

                        <span>
                          {formatQuantity(
                            holding.quantity
                          )}
                          {' '}
                          shares
                        </span>

                      </div>

                    </div>


                    <div className="holding-metric">

                      <small>
                        AVG COST
                      </small>

                      <strong>
                        ₹
                        {formatNumber(
                          holding.averageCost
                        )}
                      </strong>

                    </div>


                    <div className="holding-metric">

                      <small>
                        CURRENT
                      </small>

                      <strong>
                        ₹
                        {formatNumber(
                          holding.currentPrice
                        )}
                      </strong>

                    </div>


                    <div className="holding-metric">

                      <small>
                        VALUE
                      </small>

                      <strong>
                        ₹
                        {formatNumber(
                          holding.marketValue
                        )}
                      </strong>

                    </div>


                    <div className="holding-metric">

                      <small>
                        P&L
                      </small>

                      <strong
                        className={
                          holding.profitLoss >= 0
                            ? 'positive'
                            : 'negative'
                        }
                      >
                        {holding.profitLoss >= 0
                          ? '+'
                          : ''}
                        ₹
                        {formatNumber(
                          holding.profitLoss
                        )}

                      </strong>

                      <span
                        className={
                          holding.profitLoss >= 0
                            ? 'positive'
                            : 'negative'
                        }
                      >
                        {holding.profitLoss >= 0
                          ? '+'
                          : ''}
                        {Number(
                          holding.profitLossPercent
                        ).toFixed(2)}
                        %
                      </span>

                    </div>

                  </div>

                )
              )}

            </div>

          )}

        </section>


        {/* =================================================
            TRANSACTION FORM
        ================================================= */}

        <section className="research-panel portfolio-form-panel">

          <div className="panel-title">

            <div>

              <p className="eyebrow">
                PORTFOLIO ACTION
              </p>

              <h2>
                Record Trade
              </h2>

            </div>

          </div>


          <div className="transaction-toggle">

            <button
              type="button"
              className={
                transactionType === 'BUY'
                  ? 'transaction-type active-buy'
                  : 'transaction-type'
              }
              onClick={() =>
                handleTypeChange('BUY')
              }
            >
              BUY
            </button>


            <button
              type="button"
              className={
                transactionType === 'SELL'
                  ? 'transaction-type active-sell'
                  : 'transaction-type'
              }
              onClick={() =>
                handleTypeChange('SELL')
              }
            >
              SELL
            </button>

          </div>


          {formError && (

            <div className="error portfolio-form-error">
              {formError}
            </div>

          )}


          {formSuccess && (

            <div className="success-message">
              {formSuccess}
            </div>

          )}


          <form
            className="portfolio-form"
            onSubmit={handleSubmit}
          >

            <div className="portfolio-field">

              <label>
                Company
              </label>

              <select
                value={selectedCompanyId}
                onChange={handleCompanyChange}
              >

                {companies.map(
                  company => (

                    <option
                      key={company.id}
                      value={company.id}
                    >
                      {company.symbol}
                      {' — '}
                      {company.name}
                    </option>

                  )
                )}

              </select>

            </div>


            <div className="portfolio-field">

              <label>
                Quantity
              </label>

              <input
                type="number"
                min="0.0001"
                step="0.0001"
                placeholder="e.g. 10"
                value={quantity}
                onChange={event =>
                  setQuantity(
                    event.target.value
                  )
                }
              />

            </div>


            <div className="portfolio-field">

              <label>
                Price per Share
              </label>

              <input
                type="number"
                min="0.01"
                step="0.01"
                placeholder="e.g. 1900"
                value={price}
                onChange={event =>
                  setPrice(
                    event.target.value
                  )
                }
              />

            </div>


            {selectedCompany &&
              quantity &&
              price && (

                <div className="trade-preview">

                  <span>
                    ESTIMATED TRADE VALUE
                  </span>

                  <strong>
                    ₹
                    {formatNumber(
                      Number(quantity) *
                      Number(price)
                    )}
                  </strong>

                </div>

              )}


            <button
              className={
                transactionType === 'BUY'
                  ? 'portfolio-submit buy-submit'
                  : 'portfolio-submit sell-submit'
              }
              type="submit"
              disabled={
                actionLoading ||
                companies.length === 0
              }
            >

              {actionLoading
                ? 'Saving...'
                : `Record ${transactionType}`}

            </button>

          </form>


          <p className="panel-note">

            BUY transactions increase your cost basis.
            SELL transactions reduce the holding using
            the current average-cost basis.

          </p>

        </section>

      </div>


      {/* =================================================
          TRANSACTION HISTORY
      ================================================= */}

      <section className="research-panel transaction-history-panel">

        <div className="panel-title">

          <div>

            <p className="eyebrow">
              ACTIVITY
            </p>

            <h2>
              Transaction History
            </h2>

          </div>

        </div>


        <PortfolioTransactions
          portfolio={portfolio}
        />

      </section>

    </>
  );
}


/* =========================================================
   PORTFOLIO SUMMARY CARD
========================================================= */

function PortfolioSummaryCard({
  label,
  value,
  valueClass = ''
}) {

  return (
    <div className="portfolio-summary-card">

      <small>
        {label}
      </small>

      <strong
        className={valueClass}
      >
        {value}
      </strong>

    </div>
  );
}


/* =========================================================
   PORTFOLIO TRANSACTIONS
========================================================= */

function PortfolioTransactions({
  portfolio
}) {

  /*
     The current backend summary endpoint intentionally
     returns holdings and summary values only.

     Transaction history is therefore loaded separately
     by this component.
  */

  const [transactions, setTransactions] =
    useState([]);

  const [loading, setLoading] =
    useState(true);

  const [error, setError] =
    useState('');


  async function loadTransactions() {

    setLoading(true);
    setError('');


    try {

      const response =
        await apiFetch(
          `${PORTFOLIO_API}/portfolio/transactions`
        );


      if (!response.ok) {

        throw new Error(
          `Transaction API returned ${response.status}`
        );

      }


      const data =
        await response.json();


      setTransactions(
        Array.isArray(data)
          ? [...data].reverse()
          : []
      );

    } catch (error) {

      console.error(
        'Failed to load transactions:',
        error
      );


      if (
        error.message ===
        'SESSION_EXPIRED'
      ) {

        return;

      }


      setError(
        'Unable to load transaction history.'
      );

    } finally {

      setLoading(false);

    }

  }


  useEffect(() => {

    loadTransactions();

  }, [portfolio]);


  if (loading) {

    return (
      <div className="empty">
        Loading transactions...
      </div>
    );

  }


  if (error) {

    return (
      <div className="empty">
        {error}
      </div>
    );

  }


  if (transactions.length === 0) {

    return (
      <div className="empty">
        No transactions recorded yet.
      </div>
    );

  }


  return (
    <div className="financial-table-wrapper">

      <table className="financial-table portfolio-table">

        <thead>

          <tr>

            <th>
              Date
            </th>

            <th>
              Type
            </th>

            <th>
              Company
            </th>

            <th>
              Quantity
            </th>

            <th>
              Price
            </th>

            <th>
              Trade Value
            </th>

          </tr>

        </thead>


        <tbody>

          {transactions.map(
            transaction => {

              const company =
                transaction.company;


              const tradeValue =
                Number(transaction.quantity) *
                Number(transaction.price);


              return (

                <tr
                  key={transaction.id}
                >

                  <td>
                    {formatTransactionDate(
                      transaction.transactionDate
                    )}
                  </td>


                  <td>

                    <span
                      className={
                        transaction.type === 'BUY'
                          ? 'transaction-badge buy-badge'
                          : 'transaction-badge sell-badge'
                      }
                    >
                      {transaction.type}
                    </span>

                  </td>


                  <td>

                    <strong>
                      {company?.symbol || '-'}
                    </strong>

                    <span className="table-subtext">
                      {company?.name || ''}
                    </span>

                  </td>


                  <td>
                    {formatQuantity(
                      transaction.quantity
                    )}
                  </td>


                  <td>
                    ₹
                    {formatNumber(
                      transaction.price
                    )}
                  </td>


                  <td>
                    ₹
                    {formatNumber(
                      tradeValue
                    )}
                  </td>

                </tr>

              );

            }
          )}

        </tbody>

      </table>

    </div>
  );
}


/* =========================================================
   RESEARCH PAGE
========================================================= */

function Research({
  company,
  onBack,
  isWatched,
  onToggleWatchlist
}) {

  const [financials, setFinancials] =
    useState([]);

  const [financialLoading, setFinancialLoading] =
    useState(false);

  const [financialError, setFinancialError] =
    useState('');

  const [researchView, setResearchView] = useState(null);

  const [mlSignal, setMlSignal] = useState(null);
  const [mlSignalError, setMlSignalError] = useState('');
  const [serverDcf, setServerDcf] = useState(null);
  const [dcfLoading, setDcfLoading] = useState(false);
  const [dcfError, setDcfError] = useState('');
  const [relativeResult, setRelativeResult] = useState(null);
  const [relativeError, setRelativeError] = useState('');
  const dcfRequestRevision = useRef(0);
  const relativeRequestRevision = useRef(0);


  /* P/E VALUATION */

  const [targetPE, setTargetPE] =
    useState(
      company?.peRatio || 20
    );


  /* DCF */

  const [fcf, setFcf] =
    useState(100);

  const [growthRate, setGrowthRate] =
    useState(8);

  const [discountRate, setDiscountRate] =
    useState(10);

  const [terminalGrowth, setTerminalGrowth] =
    useState(3);

  const [forecastYears, setForecastYears] =
    useState(5);

  function invalidateDcf() {
    dcfRequestRevision.current += 1;
    setServerDcf(null);
    setDcfError('');
    setDcfLoading(false);
  }

  function updateDcfAssumption(setter, value) {
    invalidateDcf();
    setter(value);
  }

  function invalidateRelativeValuation() {
    relativeRequestRevision.current += 1;
    setRelativeResult(null);
    setRelativeError('');
  }


  /* =======================================================
     LOAD FINANCIAL DATA
  ======================================================= */

  useEffect(() => {

    if (!company?.id) {
      return;
    }

    const controller = new AbortController();
    const signal = controller.signal;
    setResearchView(null);
    setFinancials([]);
    setFinancialLoading(true);
    setFinancialError('');
    setMlSignal(null);
    setMlSignalError('');
    setFcf(100);
    setTargetPE(company.peRatio || 20);
    invalidateDcf();
    invalidateRelativeValuation();


    const loadFinancials =
      async () => {

        try {

          const response =
            await apiFetch(
              `${COMPANIES_API}/${company.id}/financials`,
              { signal }
            );


          if (!response.ok) {

            throw new Error(
              `Financial API returned ${response.status}`
            );

          }


          const data =
            await response.json();

          if (signal.aborted) return;

          setFinancials(
            Array.isArray(data)
              ? data
              : []
          );

        } catch (error) {

          if (signal.aborted) return;

          console.error(
            'Failed to load financials:',
            error
          );


          if (
            error.message ===
            'SESSION_EXPIRED'
          ) {

            return;

          }


          setFinancialError(
            'Unable to load historical financial data.'
          );

          setFinancials([]);

        } finally {

          if (!signal.aborted) setFinancialLoading(false);

        }

      };


    loadFinancials();

    apiFetch(`${API_BASE}/research/companies/${company.id}`, { signal })
      .then(async response => {
        if (!response.ok) throw new Error(`Research API returned ${response.status}`);
        return response.json();
      })
      .then(result => {
        if (!signal.aborted) setResearchView(result);
      })
      .catch(error => {
        if (!signal.aborted && error.message !== 'SESSION_EXPIRED') {
          console.error('Failed to load derived research metrics:', error);
        }
      });

    apiFetch(`${API_BASE}/research/companies/${company.id}/signal`, { signal })
      .then(async response => {
        if (!response.ok) throw new Error(`Signal API returned ${response.status}`);
        return response.json();
      })
      .then(result => {
        if (!signal.aborted) setMlSignal(result);
      })
      .catch(error => {
        if (!signal.aborted) {
          setMlSignalError(error.message === 'SESSION_EXPIRED' ? '' : error.message);
        }
      });

    return () => controller.abort();

  }, [company?.id]);

  async function calculateServerDcf() {
    const revision = ++dcfRequestRevision.current;
    setServerDcf(null);
    setDcfError('');
    setDcfLoading(true);
    try {
      const response = await apiFetch(`${API_BASE}/valuation/dcf`, {
        method: 'POST',
        body: JSON.stringify({
          freeCashFlow: Number(fcf),
          annualGrowthRate: Number(growthRate),
          projectionYears: Number(forecastYears),
          wacc: Number(discountRate),
          terminalGrowthRate: Number(terminalGrowth)
        })
      });
      const result = await response.json();
      if (!response.ok) throw new Error(result.error || 'DCF request failed');
      if (dcfRequestRevision.current === revision) setServerDcf(result);
    } catch (error) {
      if (dcfRequestRevision.current === revision) {
        setDcfError(error.message || 'DCF request failed');
      }
    } finally {
      if (dcfRequestRevision.current === revision) setDcfLoading(false);
    }
  }

  async function calculateRelativeValuation() {
    const revision = ++relativeRequestRevision.current;
    setRelativeError('');
    try {
      const response = await apiFetch(`${API_BASE}/valuation/relative`, {
        method: 'POST',
        body: JSON.stringify({ companyId: company.id, peerPe: Number(targetPE) })
      });
      const result = await response.json();
      if (!response.ok) throw new Error(result.error || 'Relative valuation failed');
      if (relativeRequestRevision.current === revision) setRelativeResult(result);
    } catch (error) {
      if (relativeRequestRevision.current === revision) {
        setRelativeError(error.message || 'Relative valuation failed');
      }
    }
  }


  /* =======================================================
     AUTOMATIC BASE FCF
  ======================================================= */

  const latestFinancial =
    financials.length > 0
      ? financials[
      financials.length - 1
      ]
      : null;


  useEffect(() => {

    if (
      latestFinancial &&
      Number(
        latestFinancial.freeCashFlow
      ) > 0
    ) {

      setFcf(
        Number(latestFinancial.freeCashFlow)
      );
      invalidateDcf();

    }

  }, [latestFinancial]);


  if (!company) {

    return (
      <div className="empty">
        No company selected.
      </div>
    );

  }


  /* =======================================================
     P/E VALUATION
  ======================================================= */

  const eps =
    company.peRatio
      ? company.currentPrice /
      company.peRatio
      : 0;


  const estimatedValue =
    eps * targetPE;


  const upside =
    company.currentPrice
      ? (
        (
          estimatedValue -
          company.currentPrice
        ) /
        company.currentPrice
      ) * 100
      : 0;


  /* =======================================================
     DCF
  ======================================================= */

  const dcfResult =
    calculateDCF({
      fcf,
      growthRate,
      discountRate,
      terminalGrowth,
      forecastYears
    });


  /* =======================================================
     BASIC METRICS CHART
  ======================================================= */

  const chartData = [
    {
      metric: 'Revenue',
      value: researchView ? researchView.metrics?.revenueGrowth : company.revenueGrowth
    },
    {
      metric: 'Profit',
      value: researchView ? researchView.metrics?.profitGrowth : company.profitGrowth
    },
    {
      metric: 'ROE',
      value: researchView ? researchView.metrics?.roe : company.roe
    }
  ];


  /* =======================================================
     HISTORICAL CHART DATA
  ======================================================= */

  const historyChartData =
    financials.map(item => ({
      year: item.fiscalYear,
      revenue: item.revenue,
      operatingProfit:
        item.operatingProfit,
      netProfit: item.netProfit,
      eps: item.eps,
      freeCashFlow:
        item.freeCashFlow
    }));


  /* =======================================================
     FUNDAMENTAL SIGNAL
  ======================================================= */

  const researchMetrics = researchView?.metrics;
  const displayedFundamentals = researchView
    ? {
      ...company,
      revenueGrowth: researchMetrics?.revenueGrowth,
      profitGrowth: researchMetrics?.profitGrowth,
      roe: researchMetrics?.roe,
      debtToEquity: researchMetrics?.debtToEquity
    }
    : company;

  const signal = calculateSignal(displayedFundamentals);
  const relativePeMetric = relativeResult?.metrics?.find(metric => metric.multiple === 'P/E');


  /* =======================================================
     DCF SENSITIVITY
  ======================================================= */

  const sensitivityData =
    createSensitivityTable({
      fcf,
      growthRate,
      discountRate,
      terminalGrowth,
      forecastYears
    });


  return (
    <>

      <div className="research-topbar">

        <button
          className="back-button"
          onClick={onBack}
        >
          ← Back to Companies
        </button>

        <span className="eyebrow">
          EQUITY RESEARCH
        </span>

      </div>


      <section className="company-header">

        <div>

          <p className="ticker-large">
            {company.symbol}
          </p>

          <h1>
            {company.name}
          </h1>

          <p className="company-sector">
            {company.sector}
          </p>

          <p className="company-source">
            {companySourceLabel(company)}
          </p>

        </div>


        <div className="company-header-actions">

          <button
            type="button"
            className={
              isWatched
                ? 'watchlist-button watchlist-active'
                : 'watchlist-button'
            }
            onClick={() =>
              onToggleWatchlist(company)
            }
          >

            <span>
              {isWatched ? '★' : '☆'}
            </span>

            {isWatched
              ? 'In Watchlist'
              : 'Add to Watchlist'}

          </button>


          <div className="price-block">

            <small>
              MARKET PRICE
            </small>

            <strong>
              ₹
              {company.currentPrice?.toLocaleString()}
            </strong>

          </div>

        </div>

      </section>


      <section className="research-metrics">

        <ResearchMetric
          label="Market Cap"
          value={formatNumber(
            company.marketCap
          )}
        />

        <ResearchMetric
          label="P/E Ratio"
          value={company.peRatio}
        />

        <ResearchMetric
          label="ROE"
          value={displayedFundamentals.roe == null ? '-' : `${displayedFundamentals.roe}%`}
        />

        <ResearchMetric
          label="Revenue Growth"
          value={displayedFundamentals.revenueGrowth == null ? '-' : `${displayedFundamentals.revenueGrowth}%`}
        />

        <ResearchMetric
          label="Profit Growth"
          value={displayedFundamentals.profitGrowth == null ? '-' : `${displayedFundamentals.profitGrowth}%`}
        />

        <ResearchMetric
          label="Debt / Equity"
          value={displayedFundamentals.debtToEquity ?? '-'}
        />

      </section>


      <div className="research-layout">


        {/* =================================================
            FUNDAMENTALS
        ================================================= */}

        <section className="research-panel">

          <div className="panel-title">

            <div>

              <p className="eyebrow">
                FUNDAMENTALS
              </p>

              <h2>
                Business Snapshot
              </h2>

            </div>

          </div>


          <div className="fundamental-list">

            <Fundamental
              label="Revenue Growth"
              value={displayedFundamentals.revenueGrowth == null ? '-' : `${displayedFundamentals.revenueGrowth}%`}
            />

            <Fundamental
              label="Profit Growth"
              value={displayedFundamentals.profitGrowth == null ? '-' : `${displayedFundamentals.profitGrowth}%`}
            />

            <Fundamental
              label="Return on Equity"
              value={displayedFundamentals.roe == null ? '-' : `${displayedFundamentals.roe}%`}
            />

            <Fundamental
              label="Debt / Equity"
              value={displayedFundamentals.debtToEquity ?? '-'}
            />

            <Fundamental
              label="Price / Earnings"
              value={company.peRatio}
            />

          </div>

        </section>


        {/* =================================================
            P/E VALUATION
        ================================================= */}

        <section className="research-panel">

          <div className="panel-title">

            <div>

              <p className="eyebrow">
                VALUATION
              </p>

              <h2>
                P/E Valuation
              </h2>

            </div>

          </div>


          <div className="valuation-box">

            <div>

              <small>
                CURRENT PRICE
              </small>

              <strong>
                ₹
                {company.currentPrice?.toLocaleString()}
              </strong>

            </div>


            <div>

              <small>
                CURRENT P/E
              </small>

              <strong>
                {company.peRatio}×
              </strong>

            </div>


            <div>

              <small>
                IMPLIED EPS
              </small>

              <strong>
                ₹{eps.toFixed(2)}
              </strong>

            </div>


            <div>

              <small>
                TARGET P/E
              </small>

              <input
                className="valuation-input"
                type="number"
                min="1"
                max="100"
                step="0.5"
                value={targetPE}
                onChange={event => {
                  invalidateRelativeValuation();
                  setTargetPE(
                    Number(
                      event.target.value
                    )
                  );
                }}
              />

            </div>

          </div>


          <div className="valuation-result">

            <div>

              <small>
                ESTIMATED VALUE
              </small>

              <strong>
                ₹
                {estimatedValue.toLocaleString(
                  undefined,
                  {
                    maximumFractionDigits: 0
                  }
                )}
              </strong>

            </div>


            <div>

              <small>
                IMPLIED UPSIDE / DOWNSIDE
              </small>

              <strong>

                {upside >= 0
                  ? '+'
                  : ''}

                {upside.toFixed(1)}%

              </strong>

            </div>

          </div>


          <p className="panel-note">
            Estimated value = implied EPS × target P/E.
            This is a simplified relative valuation model
            and depends entirely on the assumptions entered above.
          </p>

          <button type="button" className="refresh-button" onClick={calculateRelativeValuation}>
            Compare with target P/E
          </button>
          {relativeError && <div className="error">{relativeError}</div>}
          {relativeResult && <p className="panel-note">{relativePeMetric?.impliedMarketCap != null
            ? `Backend implied market cap at ${targetPE}×: ₹${formatNumber(relativePeMetric.impliedMarketCap)}`
            : relativePeMetric?.note || 'The backend could not calculate an implied P/E valuation from the available data.'}</p>}

        </section>


        {/* =================================================
            GROWTH CHART
        ================================================= */}

        <section className="research-panel chart-panel">

          <div className="panel-title">

            <div>

              <p className="eyebrow">
                METRICS
              </p>

              <h2>
                Growth & Returns
              </h2>

            </div>

          </div>


          <div className="chart">
            <Suspense fallback={<div className="empty">Loading chart…</div>}>
              <ResearchCharts variant="growth" data={chartData} />
            </Suspense>

          </div>

        </section>


        {/* =================================================
            HISTORICAL FINANCIALS
        ================================================= */}

        <section className="research-panel chart-panel historical-panel">

          <div className="panel-title">

            <div>

              <p className="eyebrow">
                HISTORICAL FINANCIALS
              </p>

              <h2>
                Revenue & Net Profit
              </h2>

            </div>

          </div>


          {financialLoading ? (

            <div className="empty">
              Loading financial history...
            </div>

          ) : financialError ? (

            <div className="empty">
              {financialError}
            </div>

          ) : financials.length === 0 ? (

            <div className="empty">
              No historical financial data available.
            </div>

          ) : (

            <>

              <div className="chart">
                <Suspense fallback={<div className="empty">Loading chart…</div>}>
                  <ResearchCharts variant="revenue-profit" data={historyChartData} />
                </Suspense>

              </div>


              <div className="financial-table-wrapper">

                <table className="financial-table">

                  <thead>

                    <tr>
                      <th>Year</th>
                      <th>Revenue</th>
                      <th>Operating Profit</th>
                      <th>Net Profit</th>
                      <th>EPS</th>
                      <th>FCF</th>
                    </tr>

                  </thead>


                  <tbody>

                    {financials.map(
                      item => (

                        <tr key={item.id}>

                          <td>
                            {item.fiscalYear}
                          </td>

                          <td>
                            {formatNumber(
                              item.revenue
                            )}
                          </td>

                          <td>
                            {formatNumber(
                              item.operatingProfit
                            )}
                          </td>

                          <td>
                            {formatNumber(
                              item.netProfit
                            )}
                          </td>

                          <td>
                            ₹{item.eps}
                          </td>

                          <td>
                            {formatNumber(
                              item.freeCashFlow
                            )}
                          </td>

                        </tr>

                      )
                    )}

                  </tbody>

                </table>

              </div>

            </>

          )}

        </section>


        {/* =================================================
            EPS + FCF
        ================================================= */}

        <section className="research-panel chart-panel">

          <div className="panel-title">

            <div>

              <p className="eyebrow">
                PER-SHARE & CASH FLOW
              </p>

              <h2>
                EPS & Free Cash Flow
              </h2>

            </div>

          </div>


          {financials.length === 0 ? (

            <div className="empty">
              No historical financial data available.
            </div>

          ) : (

            <div className="chart">
              <Suspense fallback={<div className="empty">Loading chart…</div>}>
                <ResearchCharts variant="eps-cash-flow" data={historyChartData} />
              </Suspense>

            </div>

          )}

        </section>


        {/* =================================================
            DCF VALUATION
        ================================================= */}

        <section className="research-panel dcf-panel">

          <div className="panel-title">

            <div>

              <p className="eyebrow">
                INTRINSIC VALUATION
              </p>

              <h2>
                Discounted Cash Flow
              </h2>

            </div>

          </div>


          <div className="dcf-source">

            <span>
              BASE FCF
            </span>

            <strong>
              {latestFinancial
                ? `FY${latestFinancial.fiscalYear}`
                : 'Manual'}
            </strong>

            <span>
              {latestFinancial
                ? formatNumber(
                  latestFinancial.freeCashFlow
                )
                : 'No historical FCF'}
            </span>

          </div>


          <div className="dcf-grid">

            <div>

              <label>
                Starting FCF
              </label>

              <input
                type="number"
                min="0"
                value={fcf}
                onChange={event =>
                  updateDcfAssumption(
                    setFcf,
                    Number(
                      event.target.value
                    )
                  )
                }
              />

            </div>


            <div>

              <label>
                Growth Rate %
              </label>

              <input
                type="number"
                step="0.5"
                value={growthRate}
                onChange={event =>
                  updateDcfAssumption(
                    setGrowthRate,
                    Number(
                      event.target.value
                    )
                  )
                }
              />

            </div>


            <div>

              <label>
                Discount Rate %
              </label>

              <input
                type="number"
                step="0.5"
                value={discountRate}
                onChange={event =>
                  updateDcfAssumption(
                    setDiscountRate,
                    Number(
                      event.target.value
                    )
                  )
                }
              />

            </div>


            <div>

              <label>
                Terminal Growth %
              </label>

              <input
                type="number"
                step="0.5"
                value={terminalGrowth}
                onChange={event =>
                  updateDcfAssumption(
                    setTerminalGrowth,
                    Number(
                      event.target.value
                    )
                  )
                }
              />

            </div>


            <div>

              <label>
                Forecast Years
              </label>

              <input
                type="number"
                min="1"
                max="10"
                value={forecastYears}
                onChange={event =>
                  updateDcfAssumption(
                    setForecastYears,
                    Number(
                      event.target.value
                    )
                  )
                }
              />

            </div>

          </div>


          <button type="button" className="refresh-button" onClick={calculateServerDcf} disabled={dcfLoading}>
            {dcfLoading ? 'Calculating…' : 'Calculate with Research Service'}
          </button>
          {dcfError && <div className="error">{dcfError}</div>}

          {!dcfResult.valid ? (

            <div className="empty">

              Invalid DCF assumptions.

              <br />

              Discount rate must be greater than
              terminal growth and all values must be positive.

            </div>

          ) : serverDcf ? (

            <div className="dcf-result-grid">

              <div>

                <small>
                  ENTERPRISE VALUE
                </small>

                <strong>
                  {formatNumber(Math.round(serverDcf.enterpriseValue))}
                </strong>

              </div>


              <div>

                <small>
                  TERMINAL VALUE
                </small>

                <strong>
                  {formatNumber(Math.round(serverDcf.terminalValue))}
                </strong>

              </div>


              <div>

                <small>
                  TERMINAL VALUE %
                </small>

                <strong>
                  {(serverDcf.enterpriseValue !== 0
                    ? serverDcf.presentValueOfTerminalValue / serverDcf.enterpriseValue * 100
                    : 0).toFixed(1)}%
                </strong>

              </div>

            </div>

          ) : (

            <div className="empty">
              {dcfLoading
                ? 'Calculating valuation…'
                : 'Set assumptions and calculate with the Research Service to see the enterprise value.'}
            </div>

          )}


          <p className="panel-note">

            DCF uses the latest available historical FCF
            as the starting cash flow. Future FCF is grown
            at the selected growth rate and discounted using
            the selected discount rate.

            This is a simplified educational valuation model,
            not investment advice.

          </p>

        </section>


        {/* =================================================
            DCF SENSITIVITY
        ================================================= */}

        <section className="research-panel chart-panel">

          <div className="panel-title">

            <div>

              <p className="eyebrow">
                DCF ANALYSIS
              </p>

              <h2>
                Valuation Sensitivity
              </h2>

            </div>

          </div>


          {!dcfResult.valid ? (

            <div className="empty">
              Enter valid DCF assumptions to calculate sensitivity.
            </div>

          ) : (

            <>

              <p className="panel-note">
                Enterprise value under different growth-rate
                and discount-rate assumptions.
              </p>


              <div className="financial-table-wrapper">

                <table className="financial-table">

                  <thead>

                    <tr>

                      <th>
                        Growth \ Discount
                      </th>

                      {sensitivityData.discountRates.map(
                        rate => (

                          <th key={rate}>
                            {rate}%
                          </th>

                        )
                      )}

                    </tr>

                  </thead>


                  <tbody>

                    {sensitivityData.rows.map(
                      row => (

                        <tr key={row.growthRate}>

                          <td>
                            {row.growthRate}%
                          </td>

                          {row.values.map(
                            (value, index) => (

                              <td key={index}>
                                {formatNumber(
                                  Math.round(value)
                                )}
                              </td>

                            )
                          )}

                        </tr>

                      )
                    )}

                  </tbody>

                </table>

              </div>

            </>

          )}

        </section>


        {/* =================================================
            RESEARCH SIGNAL
        ================================================= */}

        <section className="research-panel signal-panel">

          <div className="panel-title">

            <div>

              <p className="eyebrow">
                RESEARCH ENGINE · FUNDAMENTALS + ML
              </p>

              <h2>
                Research Signal
              </h2>

            </div>

          </div>


          <div className="signal">

            <div className="signal-score">
              {signal.score}
            </div>


            <div>

              <p className="eyebrow">
                FUNDAMENTAL ANALYSIS
              </p>

              <h3>
                {signal.label}
              </h3>

              <p>
                {signal.reason}
              </p>

            </div>

          </div>


          <div className="signal" style={{ marginTop: '12px' }}>

            <div className="signal-score">
              {mlSignal
                ? Number(mlSignal.model?.probability || 0).toLocaleString(undefined, {
                  style: 'percent',
                  maximumFractionDigits: 1
                })
                : '—'}
            </div>


            <div>

              <p className="eyebrow">
                MACHINE LEARNING
              </p>

              <h3>
                {mlSignal?.model?.signal || (mlSignalError ? 'Unavailable' : 'Loading…')}
              </h3>

              <p>
                {mlSignal
                  ? `Model confidence from ${mlSignal.model?.modelVersion || 'the configured model'}.`
                  : mlSignalError
                    ? `Model unavailable: ${mlSignalError}`
                    : 'Loading model signal…'}
              </p>

            </div>

          </div>


          {mlSignal && (
            <p className="panel-note">
              ML model: <strong>{mlSignal.model?.signal || 'Unavailable'}</strong>
              {' · '}confidence {Number(mlSignal.model?.probability || 0).toLocaleString(undefined, { style: 'percent', maximumFractionDigits: 1 })}
              {' · '}version {mlSignal.model?.modelVersion || 'unknown'}
              {' · '}synthetic demonstration dataset
            </p>
          )}


          <p className="panel-note">

            The fundamental signal is a rule-based research indicator.
            The ML signal is a controlled synthetic-data demonstration.
            Neither signal is investment advice.

          </p>

        </section>

      </div>

    </>
  );
}


/* =========================================================
   SMALL COMPONENTS
========================================================= */

function ResearchMetric({
  label,
  value
}) {

  return (
    <div className="research-metric">

      <small>
        {label}
      </small>

      <strong>
        {value}
      </strong>

    </div>
  );
}


function Fundamental({
  label,
  value
}) {

  return (
    <div className="fundamental">

      <span>
        {label}
      </span>

      <strong>
        {value}
      </strong>

    </div>
  );
}


/* =========================================================
   NUMBER FORMATTER
========================================================= */

function formatNumber(value) {

  if (
    value === undefined ||
    value === null ||
    Number.isNaN(Number(value))
  ) {

    return '-';

  }


  return Number(value).toLocaleString();
}


/* =========================================================
   QUANTITY FORMATTER
========================================================= */

function formatQuantity(value) {

  if (
    value === undefined ||
    value === null ||
    Number.isNaN(Number(value))
  ) {

    return '-';

  }


  return Number(value).toLocaleString(
    undefined,
    {
      maximumFractionDigits: 4
    }
  );
}


/* =========================================================
   TRANSACTION DATE FORMATTER
========================================================= */

function formatTransactionDate(value) {

  if (!value) {
    return '-';
  }


  const date =
    new Date(value);


  if (Number.isNaN(date.getTime())) {
    return value;
  }


  return date.toLocaleString(
    undefined,
    {
      dateStyle: 'medium',
      timeStyle: 'short'
    }
  );
}

function companySourceLabel(company) {
  if (company.dataClassification === 'SYNTHETIC_DEMO') {
    return 'Synthetic demonstration data';
  }
  if (company.dataClassification === 'USER_PROVIDED') {
    return 'User-provided data';
  }
  return 'Data source not verified';
}


/* =========================================================
   FUNDAMENTAL SIGNAL
========================================================= */

function calculateSignal(company) {

  let score = 0;


  if (Number.isFinite(company.revenueGrowth) && company.revenueGrowth >= 8) {
    score += 1;
  }


  if (Number.isFinite(company.profitGrowth) && company.profitGrowth >= 8) {
    score += 1;
  }


  if (Number.isFinite(company.roe) && company.roe >= 15) {
    score += 1;
  }


  if (Number.isFinite(company.debtToEquity) && company.debtToEquity >= 0 && company.debtToEquity <= 0.5) {
    score += 1;
  }


  if (Number.isFinite(company.peRatio) && company.peRatio > 0 && company.peRatio <= 30) {
    score += 1;
  }


  if (score >= 4) {

    return {
      score: `${score}/5`,
      label: 'Strong Fundamentals',
      reason:
        'The company scores positively across the selected growth, profitability, leverage and valuation metrics.'
    };

  }


  if (score >= 3) {

    return {
      score: `${score}/5`,
      label: 'Mixed Fundamentals',
      reason:
        'Several fundamental metrics are positive, while some areas require further research.'
    };

  }


  return {
    score: `${score}/5`,
    label: 'Needs Further Research',
    reason:
      'The current metric set does not produce a strong fundamental signal.'
  };
}


/* =========================================================
   DCF CALCULATION
========================================================= */

function calculateDCF({
  fcf,
  growthRate,
  discountRate,
  terminalGrowth,
  forecastYears
}) {

  const growth =
    Number(growthRate) / 100;

  const discount =
    Number(discountRate) / 100;

  const terminal =
    Number(terminalGrowth) / 100;

  const startingFCF =
    Number(fcf);

  const years =
    Number(forecastYears);


  if (
    discount <= terminal ||
    discount <= 0 ||
    startingFCF <= 0 ||
    years <= 0
  ) {

    return {
      enterpriseValue: 0,
      terminalValue: 0,
      terminalValueShare: 0,
      valid: false
    };

  }


  let presentValue = 0;

  let futureFCF =
    startingFCF;


  for (
    let year = 1;
    year <= years;
    year++
  ) {

    futureFCF *=
      1 + growth;


    const discountedFCF =
      futureFCF /
      Math.pow(
        1 + discount,
        year
      );


    presentValue +=
      discountedFCF;

  }


  const terminalFCF =
    futureFCF *
    (1 + terminal);


  const terminalValue =
    terminalFCF /
    (discount - terminal);


  const discountedTerminalValue =
    terminalValue /
    Math.pow(
      1 + discount,
      years
    );


  const enterpriseValue =
    presentValue +
    discountedTerminalValue;


  const terminalValueShare =
    enterpriseValue > 0
      ? (
        discountedTerminalValue /
        enterpriseValue
      ) * 100
      : 0;


  return {
    enterpriseValue,
    terminalValue,
    terminalValueShare,
    valid: true
  };
}


/* =========================================================
   DCF SENSITIVITY
========================================================= */

function createSensitivityTable({
  fcf,
  growthRate,
  discountRate,
  terminalGrowth,
  forecastYears
}) {

  const growthRates = [
    Math.max(
      0,
      growthRate - 2
    ),

    growthRate,

    growthRate + 2
  ];


  const discountRates = [
    Math.max(
      terminalGrowth + 1,
      discountRate - 2
    ),

    discountRate,

    discountRate + 2
  ];


  const rows =
    growthRates.map(
      growth => {

        const values =
          discountRates.map(
            discount => {

              const result =
                calculateDCF({
                  fcf,
                  growthRate:
                    growth,
                  discountRate:
                    discount,
                  terminalGrowth,
                  forecastYears
                });


              return result.valid
                ? result.enterpriseValue
                : 0;

            }
          );


        return {
          growthRate: growth,
          values
        };

      }
    );


  return {
    growthRates,
    discountRates,
    rows
  };
}


/* =========================================================
   ROOT
========================================================= */

createRoot(
  document.getElementById('root')
).render(
  <App />
);
