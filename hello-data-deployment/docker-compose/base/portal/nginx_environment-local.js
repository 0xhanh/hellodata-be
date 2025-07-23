window.environment = {
    production: false,
    portalApi: 'https://localhost:8080/api',
    docsApi: 'https://localhost:8080/dbt-docs',
    debugInfoEnabled: true,
    authConfig: {
      logLevel: 1,
      authority: 'http://localhost:38080/realms/hellodata',
      redirectUrl: 'https://localhost:8080/app/',
      postLogoutRedirectUri: 'https://localhost:8080/app/',
      clientId: 'frontend-client',
      scope: 'openid profile email offline_access',
      requireHttps: false, // Tắt yêu cầu HTTPS
      showDebugInformation: true, // Hữu ích để debug
      disableAtHashCheck: true, // Tùy chọn, nếu gặp lỗi liên quan
      sessionChecksEnabled: true,
      silentRefreshTimeout: 5000,
    },
    domainNamespace: 'hellodata',
    baseDomain: 'localhost',
    deploymentEnvironment: {
      name: 'DEV',
      headerColor: 'rgb(9,106,232)'
    },
    locale: 'vi',
    subSystemsConfig: {
      airflow: {protocol: 'http://', host: 'localhost', domain: ':8080/airflow'},
      dbtDocs: {protocol: 'http://', host: 'localhost', domain: ':8080/dbt-docs'},
      dmViewer: {protocol: 'http://', host: 'localhost', domain: ':8080/cloudbeaver/'},
      dwhViewer: {protocol: 'http://', host: 'localhost', domain: ':8080/cloudbeaver/'},
      advancedAnalyticsViewer: {protocol: 'http://', host: 'localhost', domain: ':8080'},
      // filebrowser: {protocol: 'http://', host: 'localhost', domain: ':8080/filebrowser'},
      filebrowser: {protocol: 'http://', host: 'localhost', domain: ':8090'},
      monitoringStatus: {protocol: 'http://', host: '192.168.51.168', domain: ':5099'},
      // monitoringStatus: {protocol: 'http://', host: 'localhost', domain: ':8080/ms_status/'},
      // devToolsMailbox: {protocol: 'http://', host: 'localhost', domain: ':8080/mailbox'},
      devToolsMailbox: {protocol: 'http://', host: '192.168.51.168', domain: ':8001'},
      // devToolsFileBrowser: {protocol: 'http://', host: 'localhost', domain: ':8080'},
      devToolsFileBrowser: {protocol: 'http://', host: 'localhost', domain: ':8090'},
      dataGov: {protocol: 'http://', host: '192.168.51.165', domain: ':8585'}
    },
    footerConfig: {
      openSourceDataPlatformUrl: 'https://kanton-bern.github.io/hellodata-be',
      licenseUrl: 'https://github.com/kanton-bern/hellodata-be/blob/main/LICENSE',
      githubUrl: 'https://github.com/kanton-bern/hellodata-be',
      versionLink: 'https://github.com/kanton-bern/hellodata-be/releases/'
    }
  }