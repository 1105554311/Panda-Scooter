(function () {
  'use strict';

  const STORAGE_KEYS = {
    apiBaseUrl: 'panda_admin_api_base_url',
    token: 'panda_admin_token',
    user: 'panda_admin_user'
  };

  const DEFAULT_API_BASE_URL = 'http://localhost:8080';
  const API_TIMEOUT_MS = 12000;
  const PROTECTED_VIEWS = new Set(['overview', 'pricing', 'packages', 'zones', 'dispatchers']);

  const appState = {
    currentView: 'login',
    authState: {
      token: '',
      user: null
    },
    apiState: {
      baseURL: DEFAULT_API_BASE_URL,
      lastError: ''
    },
    uiState: {
      packageQuery: { page: 1, pageSize: 10, keyword: '', status: '' },
      packageTotal: 0,
      zoneQuery: { page: 1, pageSize: 10, keyword: '' },
      zoneTotal: 0,
      dispatcherQuery: { page: 1, pageSize: 10, keyword: '', areaId: '' },
      dispatcherTotal: 0,
      overviewSeries: [],
      chartLoaded: false
    }
  };

  const dom = {};
  let overviewChart = null;
  let authFailureNotified = false;

  document.addEventListener('DOMContentLoaded', init);

  function init() {
    cacheDom();
    restoreLocalState();
    bindGlobalEvents();
    renderAuthSummary();
    updateApiStatus('未检查', 'muted');
    const initialView = hasAuthToken() ? 'overview' : 'login';
    setActiveView(initialView);
  }

  function cacheDom() {
    dom.toastRoot = document.getElementById('toast-root');
    dom.apiBaseInput = document.getElementById('api-base-input');
    dom.saveApiBtn = document.getElementById('save-api-btn');
    dom.pingApiBtn = document.getElementById('ping-api-btn');
    dom.apiStatus = document.getElementById('api-status');
    dom.authUserText = document.getElementById('auth-user-text');
    dom.logoutBtn = document.getElementById('logout-btn');
    dom.menuItems = Array.from(document.querySelectorAll('.menu-item'));
    dom.views = {
      login: document.getElementById('view-login'),
      overview: document.getElementById('view-overview'),
      pricing: document.getElementById('view-pricing'),
      packages: document.getElementById('view-packages'),
      zones: document.getElementById('view-zones'),
      dispatchers: document.getElementById('view-dispatchers'),
      settings: document.getElementById('view-settings')
    };

    dom.modalBackdrop = document.getElementById('modal-backdrop');
    dom.modalTitle = document.getElementById('modal-title');
    dom.modalForm = document.getElementById('modal-form');
    dom.modalCloseBtn = document.getElementById('modal-close-btn');

    dom.loginForm = document.getElementById('login-form');
    dom.loginEmail = document.getElementById('login-email');
    dom.loginPassword = document.getElementById('login-password');
    dom.loginSubmitBtn = document.getElementById('login-submit-btn');

    dom.overviewFilterForm = document.getElementById('overview-filter-form');
    dom.overviewStartDate = document.getElementById('overview-start-date');
    dom.overviewEndDate = document.getElementById('overview-end-date');
    dom.overviewGranularity = document.getElementById('overview-granularity');
    dom.overviewAreaId = document.getElementById('overview-area-id');
    dom.overviewRefreshBtn = document.getElementById('overview-refresh-btn');
    dom.statOrders = document.getElementById('stat-orders');
    dom.statRevenue = document.getElementById('stat-revenue');
    dom.statOnline = document.getElementById('stat-online');
    dom.statFault = document.getElementById('stat-fault');
    dom.overviewChartBox = document.getElementById('overview-chart');
    dom.overviewTableBody = document.getElementById('overview-table-body');

    dom.pricingForm = document.getElementById('pricing-form');
    dom.pricingBasePrice = document.getElementById('pricing-base-price');
    dom.pricingPricePerMin = document.getElementById('pricing-price-per-min');
    dom.pricingBillingInterval = document.getElementById('pricing-billing-interval');
    dom.pricingLoadBtn = document.getElementById('pricing-load-btn');
    dom.pricingSaveBtn = document.getElementById('pricing-save-btn');

    dom.packageFilterForm = document.getElementById('package-filter-form');
    dom.packageKeyword = document.getElementById('package-keyword');
    dom.packageStatus = document.getElementById('package-status');
    dom.packagePageSize = document.getElementById('package-page-size');
    dom.packageSearchBtn = document.getElementById('package-search-btn');
    dom.packageResetBtn = document.getElementById('package-reset-btn');
    dom.packageAddBtn = document.getElementById('package-add-btn');
    dom.packageTableBody = document.getElementById('package-table-body');
    dom.packagePrevBtn = document.getElementById('package-prev-btn');
    dom.packageNextBtn = document.getElementById('package-next-btn');
    dom.packagePageInfo = document.getElementById('package-page-info');

    dom.zoneFilterForm = document.getElementById('zone-filter-form');
    dom.zoneKeyword = document.getElementById('zone-keyword');
    dom.zonePageSize = document.getElementById('zone-page-size');
    dom.zoneSearchBtn = document.getElementById('zone-search-btn');
    dom.zoneResetBtn = document.getElementById('zone-reset-btn');
    dom.zoneAddBtn = document.getElementById('zone-add-btn');
    dom.zoneTableBody = document.getElementById('zone-table-body');
    dom.zonePrevBtn = document.getElementById('zone-prev-btn');
    dom.zoneNextBtn = document.getElementById('zone-next-btn');
    dom.zonePageInfo = document.getElementById('zone-page-info');

    dom.dispatcherFilterForm = document.getElementById('dispatcher-filter-form');
    dom.dispatcherKeyword = document.getElementById('dispatcher-keyword');
    dom.dispatcherAreaId = document.getElementById('dispatcher-area-id');
    dom.dispatcherPageSize = document.getElementById('dispatcher-page-size');
    dom.dispatcherSearchBtn = document.getElementById('dispatcher-search-btn');
    dom.dispatcherResetBtn = document.getElementById('dispatcher-reset-btn');
    dom.dispatcherAddBtn = document.getElementById('dispatcher-add-btn');
    dom.dispatcherTableBody = document.getElementById('dispatcher-table-body');
    dom.dispatcherPrevBtn = document.getElementById('dispatcher-prev-btn');
    dom.dispatcherNextBtn = document.getElementById('dispatcher-next-btn');
    dom.dispatcherPageInfo = document.getElementById('dispatcher-page-info');
  }

  function restoreLocalState() {
    const savedBaseURL = localStorage.getItem(STORAGE_KEYS.apiBaseUrl);
    appState.apiState.baseURL = normalizeBaseURL(savedBaseURL || DEFAULT_API_BASE_URL);
    dom.apiBaseInput.value = appState.apiState.baseURL;

    appState.authState.token = localStorage.getItem(STORAGE_KEYS.token) || '';
    const userString = localStorage.getItem(STORAGE_KEYS.user);
    if (userString) {
      try {
        appState.authState.user = JSON.parse(userString);
      } catch (error) {
        appState.authState.user = null;
      }
    }
  }

  function bindGlobalEvents() {
    dom.menuItems.forEach((item) => {
      item.addEventListener('click', () => {
        const view = item.getAttribute('data-view');
        if (view) {
          setActiveView(view);
        }
      });
    });

    dom.saveApiBtn.addEventListener('click', handleSaveApiBaseURL);
    dom.pingApiBtn.addEventListener('click', handlePingApi);
    dom.logoutBtn.addEventListener('click', handleLogout);

    dom.loginForm.addEventListener('submit', handleLogin);
    dom.overviewFilterForm.addEventListener('submit', handleOverviewRefresh);
    dom.pricingForm.addEventListener('submit', handleSavePricing);
    dom.pricingLoadBtn.addEventListener('click', handleLoadPricing);

    dom.packageFilterForm.addEventListener('submit', handlePackageSearch);
    dom.packageResetBtn.addEventListener('click', handlePackageReset);
    dom.packageAddBtn.addEventListener('click', openPackageCreateModal);
    dom.packagePrevBtn.addEventListener('click', () => changePackagePage(-1));
    dom.packageNextBtn.addEventListener('click', () => changePackagePage(1));

    dom.zoneFilterForm.addEventListener('submit', handleZoneSearch);
    dom.zoneResetBtn.addEventListener('click', handleZoneReset);
    dom.zoneAddBtn.addEventListener('click', openZoneCreateModal);
    dom.zonePrevBtn.addEventListener('click', () => changeZonePage(-1));
    dom.zoneNextBtn.addEventListener('click', () => changeZonePage(1));

    dom.dispatcherFilterForm.addEventListener('submit', handleDispatcherSearch);
    dom.dispatcherResetBtn.addEventListener('click', handleDispatcherReset);
    dom.dispatcherAddBtn.addEventListener('click', openDispatcherCreateModal);
    dom.dispatcherPrevBtn.addEventListener('click', () => changeDispatcherPage(-1));
    dom.dispatcherNextBtn.addEventListener('click', () => changeDispatcherPage(1));

    dom.modalCloseBtn.addEventListener('click', closeModal);
    dom.modalBackdrop.addEventListener('click', (event) => {
      if (event.target === dom.modalBackdrop) {
        closeModal();
      }
    });
  }

  function setActiveView(viewKey, options) {
    const opts = options || {};
    if (!opts.skipAuthCheck && isProtectedView(viewKey) && !hasAuthToken()) {
      notify('请先登录后台账号', 'error');
      setActiveView('login', { skipAuthCheck: true });
      return;
    }

    appState.currentView = viewKey;

    dom.menuItems.forEach((item) => {
      item.classList.toggle('active', item.getAttribute('data-view') === viewKey);
    });
    Object.keys(dom.views).forEach((key) => {
      dom.views[key].classList.toggle('active', key === viewKey);
    });

    if (viewKey === 'overview') {
      ensureOverviewLoaded();
    } else if (viewKey === 'pricing') {
      handleLoadPricing();
    } else if (viewKey === 'packages') {
      loadPackages();
    } else if (viewKey === 'zones') {
      loadZones();
    } else if (viewKey === 'dispatchers') {
      loadDispatchers();
    }
  }

  function ensureOverviewLoaded() {
    if (appState.uiState.overviewSeries.length === 0) {
      const end = formatDateInput(new Date());
      const startDate = new Date();
      startDate.setDate(startDate.getDate() - 6);
      dom.overviewStartDate.value = formatDateInput(startDate);
      dom.overviewEndDate.value = end;
      dom.overviewGranularity.value = 'day';
      void loadOverview();
    }
  }

  function handleSaveApiBaseURL() {
    const value = normalizeBaseURL(dom.apiBaseInput.value);
    appState.apiState.baseURL = value;
    dom.apiBaseInput.value = value;
    localStorage.setItem(STORAGE_KEYS.apiBaseUrl, value);
    updateApiStatus('已保存', 'success');
    notify('API 地址已保存', 'success');
  }

  async function handlePingApi() {
    dom.pingApiBtn.disabled = true;
    updateApiStatus('检查中...', 'muted');
    try {
      await probeApiConnection();
      updateApiStatus('可访问', 'success');
      notify('API 连通成功', 'success');
    } catch (error) {
      updateApiStatus('不可访问', 'danger');
      notify(resolveErrorMessage(error), 'error');
    } finally {
      dom.pingApiBtn.disabled = false;
    }
  }

  async function handleLogin(event) {
    event.preventDefault();
    const email = dom.loginEmail.value.trim();
    const password = dom.loginPassword.value.trim();
    if (!email || !password) {
      notify('请输入邮箱和密码', 'error');
      return;
    }

    dom.loginSubmitBtn.disabled = true;
    try {
      const data = await api.log.login({ email, password });
      appState.authState.token = data.token || '';
      if (!appState.authState.token) {
        throw new Error('登录成功但未返回 token');
      }
      appState.authState.user = {
        id: data.id || '',
        username: data.username || '',
        email: data.email || email
      };
      localStorage.setItem(STORAGE_KEYS.token, appState.authState.token);
      localStorage.setItem(STORAGE_KEYS.user, JSON.stringify(appState.authState.user));
      renderAuthSummary();
      setActiveView('overview');
      notify('登录成功', 'success');
    } catch (error) {
      notify(resolveErrorMessage(error), 'error');
    } finally {
      dom.loginSubmitBtn.disabled = false;
    }
  }

  async function handleLogout() {
    try {
      await api.log.logout();
    } catch (error) {
      // ignore
    }

    clearAdminSession();
    renderAuthSummary();
    setActiveView('login', { skipAuthCheck: true });
    notify('已退出登录', 'success');
  }

  function renderAuthSummary() {
    if (appState.authState.user) {
      const user = appState.authState.user;
      dom.authUserText.textContent = `${user.username || '管理员'} (${user.email || '-'})`;
      return;
    }
    if (hasAuthToken()) {
      dom.authUserText.textContent = '已登录';
      return;
    }
    dom.authUserText.textContent = '未登录';
  }

  function updateApiStatus(text, level) {
    dom.apiStatus.textContent = text;
    dom.apiStatus.classList.remove('status-muted', 'status-success', 'status-danger');
    if (level === 'success') {
      dom.apiStatus.classList.add('status-success');
    } else if (level === 'danger') {
      dom.apiStatus.classList.add('status-danger');
    } else {
      dom.apiStatus.classList.add('status-muted');
    }
  }

  async function handleOverviewRefresh(event) {
    event.preventDefault();
    await loadOverview();
  }

  async function loadOverview() {
    dom.overviewRefreshBtn.disabled = true;
    try {
      const params = {
        startDate: dom.overviewStartDate.value || undefined,
        endDate: dom.overviewEndDate.value || undefined,
        granularity: dom.overviewGranularity.value || 'day',
        areaId: toNullableNumber(dom.overviewAreaId.value)
      };

      const [liveData, overviewData] = await Promise.all([
        api.data.liveData({ date: params.endDate, areaId: params.areaId }),
        api.data.overview(params)
      ]);

      renderLiveStats(liveData);
      renderOverviewTable(overviewData.series || []);
      appState.uiState.overviewSeries = overviewData.series || [];
      await renderOverviewChart(appState.uiState.overviewSeries);
      notify('数据已更新', 'success');
    } catch (error) {
      notify(resolveErrorMessage(error), 'error');
    } finally {
      dom.overviewRefreshBtn.disabled = false;
    }
  }

  function renderLiveStats(data) {
    dom.statOrders.textContent = safeText(data.todayOrders);
    dom.statRevenue.textContent = safeText(data.todayRevenue);
    dom.statOnline.textContent = safeText(data.onlineScooters);
    dom.statFault.textContent = safeText(data.faultScooters);
  }

  function renderOverviewTable(series) {
    if (!series.length) {
      dom.overviewTableBody.innerHTML = '<tr><td colspan="3" class="empty-cell">暂无数据</td></tr>';
      return;
    }

    dom.overviewTableBody.innerHTML = series.map((item) => `
      <tr>
        <td>${escapeHtml(safeText(item.time))}</td>
        <td>${escapeHtml(safeText(item.orderCount))}</td>
        <td>${escapeHtml(safeText(item.revenue))}</td>
      </tr>
    `).join('');
  }

  async function renderOverviewChart(series) {
    if (!series.length) {
      dom.overviewChartBox.innerHTML = '<div class="chart-placeholder">暂无趋势数据</div>';
      if (overviewChart) {
        overviewChart.dispose();
        overviewChart = null;
      }
      return;
    }

    try {
      const echarts = await loadECharts();
      if (!document.getElementById('overview-chart-canvas')) {
        dom.overviewChartBox.innerHTML = '<div id="overview-chart-canvas" style="width:100%;height:320px;"></div>';
      }

      const chartEl = document.getElementById('overview-chart-canvas');
      if (!overviewChart) {
        overviewChart = echarts.init(chartEl);
      }

      overviewChart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['订单数', '收入'] },
        grid: { left: 40, right: 60, top: 40, bottom: 30 },
        xAxis: {
          type: 'category',
          data: series.map((item) => safeText(item.time))
        },
        yAxis: [
          { type: 'value', name: '订单数' },
          { type: 'value', name: '收入' }
        ],
        series: [
          {
            name: '订单数',
            type: 'bar',
            data: series.map((item) => Number(item.orderCount) || 0),
            itemStyle: { color: '#1f64ff' }
          },
          {
            name: '收入',
            type: 'line',
            yAxisIndex: 1,
            smooth: true,
            data: series.map((item) => Number(item.revenue) || 0),
            itemStyle: { color: '#0f9d58' }
          }
        ]
      });

      window.addEventListener('resize', () => {
        if (overviewChart) {
          overviewChart.resize();
        }
      }, { passive: true, once: true });
    } catch (error) {
      dom.overviewChartBox.innerHTML = '<div class="chart-placeholder">图表加载失败，请检查网络</div>';
    }
  }

  async function handleLoadPricing() {
    dom.pricingLoadBtn.disabled = true;
    try {
      const data = await api.pricing.getRules();
      dom.pricingBasePrice.value = safeNumberInput(data.basePrice);
      dom.pricingPricePerMin.value = safeNumberInput(data.pricePerMin);
      dom.pricingBillingInterval.value = safeNumberInput(data.billingInterval);
    } catch (error) {
      notify(resolveErrorMessage(error), 'error');
    } finally {
      dom.pricingLoadBtn.disabled = false;
    }
  }

  async function handleSavePricing(event) {
    event.preventDefault();
    const payload = {
      basePrice: toNullableNumber(dom.pricingBasePrice.value),
      pricePerMin: toNullableNumber(dom.pricingPricePerMin.value),
      billingInterval: toNullableInteger(dom.pricingBillingInterval.value)
    };

    if (payload.basePrice === null || payload.pricePerMin === null || payload.billingInterval === null) {
      notify('请填写完整的定价信息', 'error');
      return;
    }

    dom.pricingSaveBtn.disabled = true;
    try {
      await api.pricing.editRules(payload);
      notify('定价策略已保存', 'success');
    } catch (error) {
      notify(resolveErrorMessage(error), 'error');
    } finally {
      dom.pricingSaveBtn.disabled = false;
    }
  }

  async function handlePackageSearch(event) {
    event.preventDefault();
    appState.uiState.packageQuery.page = 1;
    appState.uiState.packageQuery.keyword = dom.packageKeyword.value.trim();
    appState.uiState.packageQuery.status = dom.packageStatus.value;
    appState.uiState.packageQuery.pageSize = Number(dom.packagePageSize.value) || 10;
    await loadPackages();
  }

  async function handlePackageReset() {
    dom.packageKeyword.value = '';
    dom.packageStatus.value = '';
    dom.packagePageSize.value = '10';
    appState.uiState.packageQuery = { page: 1, pageSize: 10, keyword: '', status: '' };
    await loadPackages();
  }

  async function changePackagePage(step) {
    const query = appState.uiState.packageQuery;
    const maxPage = Math.max(1, Math.ceil(appState.uiState.packageTotal / query.pageSize));
    const nextPage = query.page + step;
    if (nextPage < 1 || nextPage > maxPage) {
      return;
    }
    query.page = nextPage;
    await loadPackages();
  }

  async function loadPackages() {
    const query = appState.uiState.packageQuery;
    dom.packageSearchBtn.disabled = true;
    try {
      const raw = await api.packages.getPackageList({
        page: query.page,
        pageSize: query.pageSize,
        keyword: query.keyword || undefined,
        status: query.status || undefined
      });
      const normalized = normalizePagedList(raw, 'list');
      appState.uiState.packageTotal = normalized.total;
      renderPackageTable(normalized.list);
      renderPaginationText('package', query.page, query.pageSize, normalized.total);
    } catch (error) {
      dom.packageTableBody.innerHTML = '<tr><td colspan="7" class="empty-cell">加载失败</td></tr>';
      notify(resolveErrorMessage(error), 'error');
    } finally {
      dom.packageSearchBtn.disabled = false;
    }
  }

  function renderPackageTable(list) {
    if (!list.length) {
      dom.packageTableBody.innerHTML = '<tr><td colspan="7" class="empty-cell">暂无数据</td></tr>';
      return;
    }

    dom.packageTableBody.innerHTML = list.map((item) => `
      <tr>
        <td>${escapeHtml(safeText(item.id))}</td>
        <td>${escapeHtml(safeText(item.title))}</td>
        <td>${escapeHtml(getPackageTypeLabel(item.type))}</td>
        <td>${escapeHtml(safeText(item.price))}</td>
        <td>${renderStatusBadge(item.status)}</td>
        <td>${escapeHtml(formatDateTime(item.createTime))}</td>
        <td>
          <div class="actions-cell">
            <button class="btn-link" data-action="edit-package" data-id="${escapeAttr(item.id)}">编辑</button>
            <button class="btn-link danger" data-action="delete-package" data-id="${escapeAttr(item.id)}" data-title="${escapeAttr(item.title)}">删除</button>
          </div>
        </td>
      </tr>
    `).join('');

    dom.packageTableBody.querySelectorAll('[data-action="edit-package"]').forEach((btn) => {
      btn.addEventListener('click', () => openPackageEditModal(btn.getAttribute('data-id')));
    });
    dom.packageTableBody.querySelectorAll('[data-action="delete-package"]').forEach((btn) => {
      btn.addEventListener('click', () => confirmDeletePackage(btn.getAttribute('data-id'), btn.getAttribute('data-title')));
    });
  }

  async function openPackageCreateModal() {
    openModal({
      title: '新增套餐',
      fields: [
        { key: 'title', label: '标题', type: 'text', required: true },
        { key: 'description', label: '描述', type: 'textarea' },
        { key: 'type', label: '类型(1月卡/2季卡/3年卡)', type: 'number', required: true, value: 1 },
        { key: 'price', label: '价格', type: 'number', step: '0.01', required: true }
      ],
      submitText: '创建',
      onSubmit: async (values) => {
        await api.packages.addPackage({
          title: values.title,
          description: values.description || '',
          type: toNullableInteger(values.type),
          price: toNullableNumber(values.price)
        });
        notify('套餐已新增', 'success');
        await loadPackages();
      }
    });
  }

  async function openPackageEditModal(id) {
    const target = findRowDataById(dom.packageTableBody, id);
    openModal({
      title: `编辑套餐 #${id}`,
      fields: [
        { key: 'id', label: 'ID', type: 'number', value: id, readonly: true },
        { key: 'title', label: '标题', type: 'text', required: true, value: target.title },
        { key: 'description', label: '描述', type: 'textarea', value: target.description },
        { key: 'type', label: '类型(1月卡/2季卡/3年卡)', type: 'number', required: true, value: target.typeRaw || 1 },
        { key: 'price', label: '价格', type: 'number', step: '0.01', required: true, value: target.price }
      ],
      submitText: '保存',
      onSubmit: async (values) => {
        await api.packages.editPackage({
          id: toNullableInteger(values.id),
          title: values.title,
          description: values.description || '',
          type: toNullableInteger(values.type),
          price: toNullableNumber(values.price)
        });
        notify('套餐已更新', 'success');
        await loadPackages();
      }
    });
  }

  async function confirmDeletePackage(id, title) {
    const ok = window.confirm(`确认删除套餐 "${title || id}" ?`);
    if (!ok) {
      return;
    }
    try {
      await api.packages.deletePackage({ id: toNullableInteger(id) });
      notify('套餐已删除', 'success');
      await loadPackages();
    } catch (error) {
      notify(resolveErrorMessage(error), 'error');
    }
  }

  async function handleZoneSearch(event) {
    event.preventDefault();
    appState.uiState.zoneQuery.page = 1;
    appState.uiState.zoneQuery.keyword = dom.zoneKeyword.value.trim();
    appState.uiState.zoneQuery.pageSize = Number(dom.zonePageSize.value) || 10;
    await loadZones();
  }

  async function handleZoneReset() {
    dom.zoneKeyword.value = '';
    dom.zonePageSize.value = '10';
    appState.uiState.zoneQuery = { page: 1, pageSize: 10, keyword: '' };
    await loadZones();
  }

  async function changeZonePage(step) {
    const query = appState.uiState.zoneQuery;
    const maxPage = Math.max(1, Math.ceil(appState.uiState.zoneTotal / query.pageSize));
    const nextPage = query.page + step;
    if (nextPage < 1 || nextPage > maxPage) {
      return;
    }
    query.page = nextPage;
    await loadZones();
  }

  async function loadZones() {
    const query = appState.uiState.zoneQuery;
    dom.zoneSearchBtn.disabled = true;
    try {
      const raw = await api.zones.getZoneList({
        page: query.page,
        pageSize: query.pageSize,
        keyword: query.keyword || undefined
      });
      const normalized = normalizePagedList(raw, 'areaList');
      appState.uiState.zoneTotal = normalized.total;
      renderZoneTable(normalized.list);
      renderPaginationText('zone', query.page, query.pageSize, normalized.total);
    } catch (error) {
      dom.zoneTableBody.innerHTML = '<tr><td colspan="5" class="empty-cell">加载失败</td></tr>';
      notify(resolveErrorMessage(error), 'error');
    } finally {
      dom.zoneSearchBtn.disabled = false;
    }
  }

  function renderZoneTable(list) {
    if (!list.length) {
      dom.zoneTableBody.innerHTML = '<tr><td colspan="5" class="empty-cell">暂无数据</td></tr>';
      return;
    }

    dom.zoneTableBody.innerHTML = list.map((item) => `
      <tr>
        <td>${escapeHtml(safeText(item.id))}</td>
        <td>${escapeHtml(safeText(item.name))}</td>
        <td class="mono">${escapeHtml(safeText(item.polygon)).slice(0, 150)}</td>
        <td>${escapeHtml(formatDateTime(item.createTime))}</td>
        <td>
          <div class="actions-cell">
            <button class="btn-link" data-action="zone-detail" data-id="${escapeAttr(item.id)}">详情</button>
            <button class="btn-link" data-action="zone-edit" data-id="${escapeAttr(item.id)}">编辑</button>
            <button class="btn-link danger" data-action="zone-delete" data-id="${escapeAttr(item.id)}" data-name="${escapeAttr(item.name)}">删除</button>
          </div>
        </td>
      </tr>
    `).join('');

    dom.zoneTableBody.querySelectorAll('[data-action="zone-detail"]').forEach((btn) => {
      btn.addEventListener('click', () => showZoneDetail(btn.getAttribute('data-id')));
    });
    dom.zoneTableBody.querySelectorAll('[data-action="zone-edit"]').forEach((btn) => {
      btn.addEventListener('click', () => openZoneEditModal(btn.getAttribute('data-id')));
    });
    dom.zoneTableBody.querySelectorAll('[data-action="zone-delete"]').forEach((btn) => {
      btn.addEventListener('click', () => confirmDeleteZone(btn.getAttribute('data-id'), btn.getAttribute('data-name')));
    });
  }

  async function showZoneDetail(id) {
    try {
      const detail = await api.zones.getZoneDetail({ id: toNullableInteger(id) });
      openModal({
        title: `片区详情 #${id}`,
        fields: [
          { key: 'id', label: 'ID', type: 'number', value: detail.id, readonly: true },
          { key: 'name', label: '名称', type: 'text', value: detail.name || '', readonly: true },
          { key: 'polygon', label: 'Polygon', type: 'textarea', value: detail.polygon || '', readonly: true },
          { key: 'dispatcher', label: '调度员', type: 'text', value: detail.dispatcher ? `${detail.dispatcher.name || ''} ${detail.dispatcher.email || ''}` : '未分配', readonly: true }
        ],
        submitText: '关闭',
        hideSubmit: true
      });
    } catch (error) {
      notify(resolveErrorMessage(error), 'error');
    }
  }

  function openZoneCreateModal() {
    openModal({
      title: '新增片区',
      fields: [
        { key: 'name', label: '片区名称', type: 'text', required: true },
        { key: 'polygon', label: 'Polygon', type: 'textarea', required: true },
        { key: 'dispatcherId', label: '调度员ID(可选)', type: 'number' }
      ],
      submitText: '创建',
      onSubmit: async (values) => {
        await api.zones.addZone({
          name: values.name,
          polygon: values.polygon,
          dispatcherId: toNullableInteger(values.dispatcherId)
        });
        notify('片区已新增', 'success');
        await loadZones();
      }
    });
  }

  async function openZoneEditModal(id) {
    let detail = null;
    try {
      detail = await api.zones.getZoneDetail({ id: toNullableInteger(id) });
    } catch (error) {
      notify(resolveErrorMessage(error), 'error');
      return;
    }

    openModal({
      title: `编辑片区 #${id}`,
      fields: [
        { key: 'id', label: 'ID', type: 'number', value: id, readonly: true },
        { key: 'name', label: '片区名称', type: 'text', required: true, value: detail.name || '' },
        { key: 'polygon', label: 'Polygon', type: 'textarea', required: true, value: detail.polygon || '' }
      ],
      submitText: '保存',
      onSubmit: async (values) => {
        await api.zones.editZone({
          id: toNullableInteger(values.id),
          name: values.name,
          polygon: values.polygon
        });
        notify('片区已更新', 'success');
        await loadZones();
      }
    });
  }

  async function confirmDeleteZone(id, name) {
    const ok = window.confirm(`确认删除片区 "${name || id}" ?`);
    if (!ok) {
      return;
    }
    try {
      await api.zones.deleteZone({
        id: toNullableInteger(id),
        name: name || undefined
      });
      notify('片区已删除', 'success');
      await loadZones();
    } catch (error) {
      notify(resolveErrorMessage(error), 'error');
    }
  }

  async function handleDispatcherSearch(event) {
    event.preventDefault();
    appState.uiState.dispatcherQuery.page = 1;
    appState.uiState.dispatcherQuery.keyword = dom.dispatcherKeyword.value.trim();
    appState.uiState.dispatcherQuery.areaId = dom.dispatcherAreaId.value.trim();
    appState.uiState.dispatcherQuery.pageSize = Number(dom.dispatcherPageSize.value) || 10;
    await loadDispatchers();
  }

  async function handleDispatcherReset() {
    dom.dispatcherKeyword.value = '';
    dom.dispatcherAreaId.value = '';
    dom.dispatcherPageSize.value = '10';
    appState.uiState.dispatcherQuery = { page: 1, pageSize: 10, keyword: '', areaId: '' };
    await loadDispatchers();
  }

  async function changeDispatcherPage(step) {
    const query = appState.uiState.dispatcherQuery;
    const maxPage = Math.max(1, Math.ceil(appState.uiState.dispatcherTotal / query.pageSize));
    const nextPage = query.page + step;
    if (nextPage < 1 || nextPage > maxPage) {
      return;
    }
    query.page = nextPage;
    await loadDispatchers();
  }

  async function loadDispatchers() {
    const query = appState.uiState.dispatcherQuery;
    dom.dispatcherSearchBtn.disabled = true;
    try {
      const raw = await api.dispatchers.getDispatcherList({
        page: query.page,
        pageSize: query.pageSize,
        keyword: query.keyword || undefined,
        areaId: toNullableInteger(query.areaId)
      });
      const normalized = normalizePagedList(raw, 'dispatcherList');
      appState.uiState.dispatcherTotal = normalized.total;
      renderDispatcherTable(normalized.list);
      renderPaginationText('dispatcher', query.page, query.pageSize, normalized.total);
    } catch (error) {
      dom.dispatcherTableBody.innerHTML = '<tr><td colspan="6" class="empty-cell">加载失败</td></tr>';
      notify(resolveErrorMessage(error), 'error');
    } finally {
      dom.dispatcherSearchBtn.disabled = false;
    }
  }

  function renderDispatcherTable(list) {
    if (!list.length) {
      dom.dispatcherTableBody.innerHTML = '<tr><td colspan="6" class="empty-cell">暂无数据</td></tr>';
      return;
    }

    dom.dispatcherTableBody.innerHTML = list.map((item) => `
      <tr>
        <td>${escapeHtml(safeText(item.id))}</td>
        <td>${escapeHtml(safeText(item.name))}</td>
        <td>${escapeHtml(safeText(item.email))}</td>
        <td>${escapeHtml(safeText(item.areaId))}</td>
        <td>${escapeHtml(formatDateTime(item.createTime))}</td>
        <td>
          <div class="actions-cell">
            <button class="btn-link" data-action="dispatcher-edit" data-id="${escapeAttr(item.id)}" data-name="${escapeAttr(item.name)}" data-email="${escapeAttr(item.email)}" data-area-id="${escapeAttr(item.areaId)}">编辑</button>
          </div>
        </td>
      </tr>
    `).join('');

    dom.dispatcherTableBody.querySelectorAll('[data-action="dispatcher-edit"]').forEach((btn) => {
      btn.addEventListener('click', () => {
        openDispatcherEditModal({
          id: btn.getAttribute('data-id'),
          name: btn.getAttribute('data-name'),
          email: btn.getAttribute('data-email'),
          areaId: btn.getAttribute('data-area-id')
        });
      });
    });
  }

  function openDispatcherCreateModal() {
    openModal({
      title: '新增管理员',
      fields: [
        { key: 'name', label: '姓名', type: 'text', required: true },
        { key: 'email', label: '邮箱', type: 'email', required: true },
        { key: 'password', label: '密码', type: 'password', required: true },
        { key: 'areaId', label: '区域ID', type: 'number', required: true }
      ],
      submitText: '创建',
      onSubmit: async (values) => {
        await api.dispatchers.addDispatcher({
          name: values.name,
          email: values.email,
          password: values.password,
          areaId: toNullableInteger(values.areaId)
        });
        notify('管理员已新增', 'success');
        await loadDispatchers();
      }
    });
  }

  function openDispatcherEditModal(item) {
    openModal({
      title: `编辑管理员 #${item.id}`,
      fields: [
        { key: 'id', label: 'ID', type: 'number', value: item.id, readonly: true },
        { key: 'name', label: '姓名', type: 'text', required: true, value: item.name },
        { key: 'email', label: '邮箱', type: 'email', required: true, value: item.email },
        { key: 'password', label: '新密码(可选)', type: 'password' },
        { key: 'areaId', label: '区域ID', type: 'number', required: true, value: item.areaId }
      ],
      submitText: '保存',
      onSubmit: async (values) => {
        const payload = {
          id: toNullableInteger(values.id),
          name: values.name,
          email: values.email,
          areaId: toNullableInteger(values.areaId)
        };
        if (values.password) {
          payload.password = values.password;
        }
        await api.dispatchers.editDispatcher(payload);
        notify('管理员已更新', 'success');
        await loadDispatchers();
      }
    });
  }

  function renderPaginationText(type, page, pageSize, total) {
    const maxPage = Math.max(1, Math.ceil(total / pageSize));
    const text = `第 ${page} / ${maxPage} 页，共 ${total} 条`;
    if (type === 'package') {
      dom.packagePageInfo.textContent = text;
      dom.packagePrevBtn.disabled = page <= 1;
      dom.packageNextBtn.disabled = page >= maxPage;
    } else if (type === 'zone') {
      dom.zonePageInfo.textContent = text;
      dom.zonePrevBtn.disabled = page <= 1;
      dom.zoneNextBtn.disabled = page >= maxPage;
    } else {
      dom.dispatcherPageInfo.textContent = text;
      dom.dispatcherPrevBtn.disabled = page <= 1;
      dom.dispatcherNextBtn.disabled = page >= maxPage;
    }
  }

  function openModal(options) {
    const { title, fields, submitText, hideSubmit, onSubmit } = options;
    dom.modalTitle.textContent = title || '操作';
    dom.modalForm.innerHTML = '';

    fields.forEach((field) => {
      const wrapper = document.createElement('label');
      wrapper.className = 'field';
      wrapper.innerHTML = `<span>${escapeHtml(field.label || '')}</span>`;

      const input = field.type === 'textarea'
        ? document.createElement('textarea')
        : document.createElement('input');

      input.name = field.key;
      input.className = 'text-input';
      if (field.type !== 'textarea') {
        input.type = field.type || 'text';
      }
      if (field.step) {
        input.step = field.step;
      }
      if (field.required) {
        input.required = true;
      }
      if (field.readonly) {
        input.readOnly = true;
      }
      if (typeof field.value !== 'undefined' && field.value !== null) {
        input.value = String(field.value);
      }
      wrapper.appendChild(input);
      dom.modalForm.appendChild(wrapper);
    });

    const actions = document.createElement('div');
    actions.className = 'modal-actions';
    actions.innerHTML = `
      <button type="button" class="btn btn-outline" data-action="cancel">取消</button>
      ${hideSubmit ? '' : `<button type="submit" class="btn btn-primary">${escapeHtml(submitText || '保存')}</button>`}
    `;
    dom.modalForm.appendChild(actions);

    dom.modalForm.onsubmit = async (event) => {
      event.preventDefault();
      if (!onSubmit) {
        closeModal();
        return;
      }

      const submitBtn = dom.modalForm.querySelector('button[type="submit"]');
      if (submitBtn) {
        submitBtn.disabled = true;
      }

      const formData = new FormData(dom.modalForm);
      const values = {};
      formData.forEach((value, key) => {
        values[key] = String(value || '').trim();
      });

      try {
        await onSubmit(values);
        closeModal();
      } catch (error) {
        notify(resolveErrorMessage(error), 'error');
      } finally {
        if (submitBtn) {
          submitBtn.disabled = false;
        }
      }
    };

    dom.modalForm.querySelector('[data-action="cancel"]').addEventListener('click', closeModal);
    dom.modalBackdrop.classList.remove('hidden');
  }

  function closeModal() {
    dom.modalBackdrop.classList.add('hidden');
    dom.modalForm.onsubmit = null;
    dom.modalForm.innerHTML = '';
  }

  function notify(message, type) {
    const toast = document.createElement('div');
    toast.className = `toast-item ${type || ''}`.trim();
    toast.textContent = message || '操作完成';
    dom.toastRoot.appendChild(toast);
    setTimeout(() => {
      toast.remove();
    }, 2500);
  }

  function normalizeBaseURL(input) {
    const value = String(input || '').trim().replace(/\/+$/, '');
    return value || DEFAULT_API_BASE_URL;
  }

  function buildUrl(path, params) {
    const url = new URL(appState.apiState.baseURL + path);
    if (params) {
      Object.keys(params).forEach((key) => {
        const value = params[key];
        if (value !== undefined && value !== null && value !== '') {
          url.searchParams.set(key, value);
        }
      });
    }
    return url.toString();
  }

  function hasAuthToken() {
    return Boolean(appState.authState.token);
  }

  function isProtectedView(viewKey) {
    return PROTECTED_VIEWS.has(viewKey);
  }

  function clearAdminSession() {
    appState.authState.token = '';
    appState.authState.user = null;
    localStorage.removeItem(STORAGE_KEYS.token);
    localStorage.removeItem(STORAGE_KEYS.user);
  }

  function isAuthFailure(status, message) {
    if (status === 401 || status === 403) {
      return true;
    }
    const msg = String(message || '');
    return msg.includes('未登录') || msg.includes('登录已失效');
  }

  function handleAuthFailure(message) {
    const hadSession = hasAuthToken();
    clearAdminSession();
    renderAuthSummary();
    setActiveView('login', { skipAuthCheck: true });

    if (hadSession && !authFailureNotified) {
      authFailureNotified = true;
      notify(message || '登录已失效，请重新登录', 'error');
      setTimeout(() => {
        authFailureNotified = false;
      }, 800);
    }
  }

  async function probeApiConnection() {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), API_TIMEOUT_MS);

    try {
      await fetch(buildUrl('/admin/log/login'), {
        method: 'GET',
        signal: controller.signal
      });
    } catch (error) {
      if (error && error.name === 'AbortError') {
        throw new Error('请求超时，请检查后端服务');
      }
      throw new Error('网络请求失败，请检查 API 地址和后端服务');
    } finally {
      clearTimeout(timeoutId);
    }
  }

  async function request(path, options) {
    const config = options || {};
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), API_TIMEOUT_MS);

    const headers = {
      'Content-Type': 'application/json',
      ...(config.headers || {})
    };

    if (hasAuthToken() && config.auth !== false) {
      headers.token = appState.authState.token;
    }

    let response;
    try {
      response = await fetch(buildUrl(path, config.params), {
        method: config.method || 'GET',
        headers,
        body: config.body ? JSON.stringify(config.body) : undefined,
        signal: controller.signal
      });
    } catch (error) {
      clearTimeout(timeoutId);
      if (error && error.name === 'AbortError') {
        throw new Error('请求超时，请检查后端服务');
      }
      throw new Error('网络请求失败，请检查 API 地址和后端服务');
    }
    clearTimeout(timeoutId);

    let payload = null;
    try {
      payload = await response.json();
    } catch (error) {
      payload = null;
    }

    const message = payload ? (payload.msg || payload.message || '') : '';
    const code = payload ? String(payload.code) : '';
    const businessFailed = Boolean(payload) && code !== '0' && code !== '200';

    if (isAuthFailure(response.status, message)) {
      handleAuthFailure(message);
    }

    if (!response.ok) {
      throw new Error(message || `请求失败: HTTP ${response.status}`);
    }

    if (!payload) {
      throw new Error('服务返回内容不是有效 JSON');
    }

    if (businessFailed) {
      throw new Error(message || '后端返回错误');
    }

    return payload.data;
  }

  const api = {
    log: {
      login: (body) => request('/admin/log/login', { method: 'POST', body, auth: false }),
      logout: () => request('/admin/log/logout', { method: 'POST' })
    },
    data: {
      overview: (params) => request('/admin/data/overview', { method: 'GET', params }),
      liveData: (params) => request('/admin/data/liveData', { method: 'GET', params })
    },
    pricing: {
      getRules: () => request('/admin/princingRule/getRules', { method: 'GET' }),
      editRules: (body) => request('/admin/princingRule/editRules', { method: 'POST', body })
    },
    packages: {
      getPackageList: (params) => request('/admin/packages/getPackageList', { method: 'GET', params }),
      addPackage: (body) => request('/admin/packages/addPackage', { method: 'POST', body }),
      editPackage: (body) => request('/admin/packages/editPackage', { method: 'POST', body }),
      deletePackage: (body) => request('/admin/packages/deletePackage', { method: 'DELETE', body })
    },
    zones: {
      getZoneList: (params) => request('/admin/zones/getZoneList', { method: 'GET', params }),
      getZoneDetail: (params) => request('/admin/zones/getZoneDetail', { method: 'GET', params }),
      addZone: (body) => request('/admin/zones/addZone', { method: 'POST', body }),
      editZone: (body) => request('/admin/zones/editZone', { method: 'POST', body }),
      deleteZone: (body) => request('/admin/zones/deleteZone', { method: 'DELETE', body })
    },
    dispatchers: {
      getDispatcherList: (params) => request('/admin/dispatchers/getDispatcherList', { method: 'GET', params }),
      addDispatcher: (body) => request('/admin/dispatchers/addDispatcher', { method: 'POST', body }),
      editDispatcher: (body) => request('/admin/dispatchers/editDispatcher', { method: 'POST', body })
    }
  };

  function normalizePagedList(raw, listKey) {
    const list = Array.isArray(raw && raw[listKey]) ? raw[listKey] : [];
    const total = Number(raw && raw.total) || 0;
    const page = Number(raw && raw.page) || 1;
    const pageSize = Number(raw && raw.pageSize) || 10;
    return { list, total, page, pageSize };
  }

  function resolveErrorMessage(error) {
    if (!error) {
      return '请求失败';
    }
    if (typeof error === 'string') {
      return error;
    }
    if (error.message) {
      return error.message;
    }
    return '请求失败';
  }

  function safeText(value) {
    if (value === null || value === undefined || value === '') {
      return '--';
    }
    return String(value);
  }

  function safeNumberInput(value) {
    if (value === null || value === undefined || value === '') {
      return '';
    }
    return String(value);
  }

  function formatDateInput(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  function formatDateTime(value) {
    if (!value) {
      return '--';
    }
    const raw = String(value).replace('T', ' ').replace('Z', '').slice(0, 19);
    return raw || '--';
  }

  function toNullableNumber(value) {
    if (value === null || value === undefined || value === '') {
      return null;
    }
    const num = Number(value);
    return Number.isFinite(num) ? num : null;
  }

  function toNullableInteger(value) {
    if (value === null || value === undefined || value === '') {
      return null;
    }
    const num = Number(value);
    return Number.isInteger(num) ? num : null;
  }

  function getPackageTypeLabel(type) {
    const map = { 1: '月卡', 2: '季卡', 3: '年卡' };
    return map[Number(type)] || safeText(type);
  }

  function renderStatusBadge(status) {
    const val = Number(status);
    if (val === 1) {
      return '<span class="badge badge-success">启用</span>';
    }
    if (val === 0) {
      return '<span class="badge badge-danger">停用</span>';
    }
    return `<span class="badge badge-muted">${escapeHtml(safeText(status))}</span>`;
  }

  function loadECharts() {
    if (window.echarts) {
      return Promise.resolve(window.echarts);
    }

    return new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.src = 'https://cdn.jsdelivr.net/npm/echarts@5.5.1/dist/echarts.min.js';
      script.onload = () => resolve(window.echarts);
      script.onerror = () => reject(new Error('ECharts load failed'));
      document.head.appendChild(script);
    });
  }

  function escapeHtml(value) {
    return String(value || '')
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#39;');
  }

  function escapeAttr(value) {
    return escapeHtml(value);
  }

  function findRowDataById(tableBody, id) {
    const row = Array.from(tableBody.querySelectorAll('tr')).find((tr) => tr.firstElementChild && tr.firstElementChild.textContent.trim() === String(id));
    if (!row) {
      return {};
    }
    const cells = row.querySelectorAll('td');
    return {
      id: cells[0] ? cells[0].textContent.trim() : '',
      title: cells[1] ? cells[1].textContent.trim() : '',
      typeRaw: parseTypeText(cells[2] ? cells[2].textContent.trim() : ''),
      price: cells[3] ? cells[3].textContent.trim() : '',
      description: ''
    };
  }

  function parseTypeText(text) {
    if (text === '月卡') return 1;
    if (text === '季卡') return 2;
    if (text === '年卡') return 3;
    const val = Number(text);
    return Number.isFinite(val) ? val : 1;
  }
})();
